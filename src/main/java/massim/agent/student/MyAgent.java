package massim.agent.student;

import cz.agents.alite.communication.Message;
import massim.agent.Action;
import massim.agent.MASAgent;
import massim.agent.MASPerception;
import massim.agent.Position;
import massim.agent.student.utils.MessageData;
import massim.agent.student.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * MAS agent implementation using BDI architecture.
 */
public class MyAgent extends MASAgent implements GameConstants {

	private static final boolean INFO = true, DEBUG = true, VERBOSE = false;

	/** Agents' random generator. */
	private final Random random;

	/** Meta-data about agents' friends. */
	private final Map<String, AgentMetadata> friendMetadata;

	/** Agents' desired positions. */
	private final Deque<Position> desiredPositions;
	/** Agents' checkpoints to visit. */
	private final List<Position> myCheckpoints;

	/** Agents' number. */
	private Long myNumber;
	/** Agents' leader flag. */
	private Boolean isLeader;

	/** Current state of the agent. */
	private AgentState state;
	/** Current state of the map. */
	private GameMap map;
	/** Current position of the agent. */
	private Position myPosition;
	/** Current position the agent intend to visit. */
	private Position intendedPosition;

	/** Constructor of the MyAgent class. */
	public MyAgent(String host, int port, String username, String password) {
		super(host, port, username, password);
		random = new Random(System.nanoTime());
		friendMetadata = new LinkedHashMap<String, AgentMetadata>(FRIENDS);
		desiredPositions = new LinkedList<Position>();
		myCheckpoints = new ArrayList<Position>(CHECKPOINTS);
		myNumber = null;
		isLeader = null;
		state = AgentState.init;
		map = null;
		myPosition = null;
		intendedPosition = null;
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
			case finished:
				action = Action.SKIP;
				break;
			case idle:
			default:
				action = doRandomWalk();
		}
        return action;
    }

	/** Resets the agent into its initial state. */
	protected void reset() {
		friendMetadata.clear();
		desiredPositions.clear();
		myNumber = null;
		isLeader = null;
		setState(AgentState.init);
		map.init();
		myPosition = null;
		intendedPosition = null;
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
				metadata.state = MessageUtils.getData(data);
			} else if ("myNumber".equals(data.getType())) {
				metadata.number = MessageUtils.getData(data);
			} else if ("leader".equals(data.getType())) {
				metadata.isLeader = MessageUtils.getData(data);
			} else if ("position".equals(data.getType())) {
				metadata.position = MessageUtils.getData(data);
			} else if ("goto".equals(data.getType())) {
				desiredPositions.addFirst(MessageUtils.<Position>getData(data));
			}
			printDebug("[" + message.getSender() + " " + data.getType() + "] " + data.getData());
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
			if (intendedPosition == null) {
				printDebug("map:\n" + map.toString());
				final List<Position> switches = map.findAllSwitches();
				if (!switches.isEmpty()) {
					final Position positionBeforeSwitch = map.getPositionBeforeSwitch(myPosition, switches.get(0));
					broadcast(MessageUtils.create("goto", positionBeforeSwitch));
				}
				intendedPosition = myPosition;
			}
		} else {
			if (intendedPosition == null && desiredPositions.isEmpty()) {
				printInfo("FINISHED");
				setState(AgentState.finished);
			} else if (intendedPosition == null) {
				intendedPosition = desiredPositions.removeFirst();
				printDebug("new intention " + intendedPosition);
				return map.planMove(myPosition, intendedPosition);
			} else if (myPosition.equals(intendedPosition)) {
				intendedPosition = null;
				printDebug("map:\n" + map.toString());
				broadcast(MessageUtils.create("position", myPosition));
			} else {
				return map.planMove(myPosition, intendedPosition);
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
		broadcast(MessageUtils.create("myState", state));
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
