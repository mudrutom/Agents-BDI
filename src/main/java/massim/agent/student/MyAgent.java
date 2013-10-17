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

/**
 * MAS agent implementation using BDI architecture.
 */
public class MyAgent extends MASAgent implements GameConstants {

	private static final boolean INFO = true, DEBUG = true, VERBOSE = false;

	/** Meta-data about agents' friends. */
	private final Map<String, AgentMetadata> friendMetadata;

	/** Agents' random generator. */
	private final Random random;

	/** Agents' number. */
	private Long myNumber;
	/** Agents' leader flag. */
	private Boolean isLeader;

	/** Current state of the agent. */
	private AgentState state;
	/** Current state of the map. */
	private GameMap map;
	/** Current positions of the agent, */
	private Position myPosition;

	/** Agents' desired position. */
	private Position desiredPosition;

	/** Constructor of the MyAgent class. */
	public MyAgent(String host, int port, String username, String password) {
		super(host, port, username, password);
		friendMetadata = new LinkedHashMap<String, AgentMetadata>(FRIENDS);
		random = new Random(System.nanoTime());
		state = AgentState.init;
		map = null;
		isLeader = null;
		myNumber = null;
		myPosition = null;
		desiredPosition = null;
	}

	@Override
	protected void onStart(int gridWidth, int gridHeight, int visibility) {
		printInfo("gridWidth=" + gridWidth + " gridHeight=" + gridHeight + " visibility=" + visibility);
		map = new GameMap(gridWidth, gridHeight);
	}

    @Override
    protected Action deliberate(MASPerception percept) {
		printVerbose("step=" + percept.getStep());

		myPosition = new Position(percept.getPosX(), percept.getPosY());
		map.refresh(myPosition, percept.getCellPercepts());

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
		map.init();
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
			printDebug("[" + message.getSender() + " " + data.getType() + "] " +
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
				printDebug("map:\n" + map.toString());
				final List<Position> switches = map.findAllSwitches();
				if (!switches.isEmpty()) {
					final Position positionBeforeSwitch = map.getPositionBeforeSwitch(myPosition, switches.get(0));
					broadcast(MessageUtils.create("goto", positionBeforeSwitch));
				} else {
					broadcast(MessageUtils.create("goto", CHECKPOINTS[0]));
				}
				desiredPosition = myPosition;
			}
		} else {
			if (myPosition.equals(desiredPosition)) {
				printDebug("map:\n" + map.toString());
				broadcast(MessageUtils.create("position", myPosition));
				setState(AgentState.idle);
			} else if (desiredPosition != null) {
				return map.planMove(myPosition, desiredPosition);
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

	/** Prints given message to STD-OUT if in INFO mode. */
	protected void printInfo(String message) {
		if (INFO) System.out.println(username + ": " + message);
	}

	/** Prints given message to STD-OUT if in DEBUG mode. */
	protected void printDebug(String message) {
		if (DEBUG) System.out.println(username + "@" + state + ": " + message);
	}

	/** Prints given message to STD-OUT if in VERBOSE mode. */
	protected void printVerbose(String message) {
		if (VERBOSE) System.out.println(username + ": " + message);
	}
}
