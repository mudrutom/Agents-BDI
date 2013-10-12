package massim.agent.student;

import cz.agents.alite.communication.Message;
import massim.agent.Action;
import massim.agent.MASAgent;
import massim.agent.MASPerception;
import massim.agent.Position;
import massim.agent.student.utils.MessageData;
import massim.agent.student.utils.MessageUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MyAgent extends MASAgent {

	/** The number of friend agents. */
	protected static final int FRIENDS = 2;

	/** The checkpoints the agent needs to visit. */
	protected static final Position[] CHECKPOINTS = {
			new Position(2,2), new Position(27,2), new Position(27,27), new Position(2,27)
	};

	/** Possible agents' actions. */
	protected static final Action[] ACTIONS = {
			Action.NORTH, Action.NORTHEAST, Action.EAST, Action.SOUTHEAST,
			Action.SOUTH, Action.SOUTHWEST, Action.WEST, Action.NORTHWEST,
			Action.SKIP
	};

	/** Mata-data about agents' friends. */
	private final Map<String, AgentMetadata> friendMetadata;

	/** Agents' random generator. */
	private final Random random;

	/** Current state of the agent. */
	private AgentState state;

	/** Agents' leader flag. */
	private Boolean isLeader;

	/** Agents' number. */
	private Long myNumber;

	private Position myPosition, desiredPosition;

	public MyAgent(String host, int port, String username, String password) {
		super(host, port, username, password);
		friendMetadata = new LinkedHashMap<String, AgentMetadata>(FRIENDS);
		random = new Random(System.nanoTime());
		state = AgentState.init;
		isLeader = null;
		myNumber = null;
		myPosition = null;
		desiredPosition = null;
	}

	@Override
	protected void onStart(int gridWidth, int gridHeight, int visibility) {
		System.out.println(getUsername() + ": The world is " + gridWidth + " cells wide and " + gridHeight + " cells high.");
	}

    @Override
    protected Action deliberate(MASPerception percept) {
		myPosition = new Position(percept.getPosX(), percept.getPosY());
		processMessages();

		final Action action;
		switch (state) {
			case init:
				action = doInit();
				break;
			case ready:
				action = doWalk();
				break;
			case idle:
			default:
				action = doRandomWalk();
		}
        return action;
    }

	/** Resets the agent into the initial state. */
	protected void reset() {
		friendMetadata.clear();
		setState(AgentState.init);
		isLeader = null;
		myNumber = null;
		myPosition = null;
		desiredPosition = null;
	}

	/** Processing of the messages in agents' inbox. */
	protected void processMessages() {
		final List<Message> messages = getNewMessages();
		for (Message message : messages) {
			// retrieve meta-data of the sender (friend agent)
			AgentMetadata metadata = friendMetadata.get(message.getSender());
			if (metadata == null) {
				metadata = new AgentMetadata();
				friendMetadata.put(message.getSender(), metadata);
			}

			// process received data
			MessageData data = MessageUtils.parse(message);
			if ("reset".equals(data.getType())) {
				reset();
			} else if ("myState".equals(data.getType())) {
				metadata.state = AgentState.valueOf(data.getString());
			} else if ("myNumber".equals(data.getType())) {
				metadata.number = data.getNumber();
			} else if ("leader".equals(data.getType())) {
				metadata.isLeader = data.getBool();
			} else if ("position".equals(data.getType())) {
				metadata.position = data.getPosition();
			} else if ("goto".equals(data.getType())) {
				desiredPosition = data.getPosition();
			}
			System.out.println(getUsername() + ": [" + message.getSender() + " " + data.getType() + "] " +
					data.getString() + " " + data.getNumber() + " " + data.getBool() + " " + data.getPosition());
		}
	}

	private Action doInit() {
		if (myNumber == null) {
			// send agents' number
			myNumber = random.nextLong();
			broadcast(MessageUtils.create("myNumber", myNumber));
		} else if (isLeader == null && friendMetadata.size() == FRIENDS) {
			// determine the leader agent
			Long max = myNumber;
			int count = 0;
			for (AgentMetadata metadata : friendMetadata.values()) {
				if (metadata.number != null) {
					count++;
					if (myNumber.compareTo(metadata.number) == 0) {
						broadcast(MessageUtils.create("reset"));
						reset();
					} else if (max.compareTo(metadata.number) < 0) {
						max = metadata.number;
					}
				}
			}
			if (count == FRIENDS) {
				isLeader = (myNumber.compareTo(max) == 0);
				broadcast(MessageUtils.create("leader", isLeader));
				setState(AgentState.ready);
			}
		}

		// do nothing while in init
		return Action.SKIP;
	}

	private Action doWalk() {
		if (isLeader) {
			if (desiredPosition == null) {
				broadcast(MessageUtils.create("goto", new Position(1, 1)));
				desiredPosition = myPosition;
			}
		} else {
			if (myPosition.equals(desiredPosition)) {
				broadcast(MessageUtils.create("position", myPosition));
				setState(AgentState.idle);
			} else if (desiredPosition != null) {
				return getAction(myPosition, desiredPosition);
			}
		}
		return Action.SKIP;
	}

	private Action doRandomWalk() {
		return ACTIONS[random.nextInt(ACTIONS.length)];
	}

	/** @param state new state of the agent */
	protected void setState(AgentState state) {
		this.state = state;
		broadcast(MessageUtils.create("myState", state.name()));
	}

	/** @return Action to get form one position to the other. */
	protected Action getAction(Position fromPos, Position toPos) {
		final int fromX = fromPos.getX(), fromY = fromPos.getY();
		final int toX = toPos.getX(), toY = toPos.getY();

		if (fromX == toX && fromY == toY) {
			return Action.SKIP;
		} else if (fromX == toX) {
			return (fromY < toY) ? Action.SOUTH : Action.NORTH;
		} else if (fromY == toY) {
			return (fromX < toX) ? Action.EAST : Action.WEST;
		} else {
			if (fromX < toX) {
				return (fromY < toY) ? Action.SOUTHEAST : Action.NORTHEAST;
			} else {
				return (fromY < toY) ? Action.SOUTHWEST : Action.NORTHWEST;
			}
		}
	}
}
