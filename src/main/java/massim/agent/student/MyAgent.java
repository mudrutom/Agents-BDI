package massim.agent.student;

import cz.agents.alite.communication.Message;
import massim.agent.Action;
import massim.agent.MASAgent;
import massim.agent.MASPerception;
import massim.agent.Position;
import massim.agent.student.game.Fence;
import massim.agent.student.game.GameConstants;
import massim.agent.student.game.GameMap;
import massim.agent.student.utils.MessageData;
import massim.agent.student.utils.MessageUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

/**
 * MAS agent implementation using BDI architecture.
 */
public class MyAgent extends MASAgent implements GameConstants {

	private static final boolean INFO = true, DEBUG = false, VERBOSE = false;

	/** Agents' random generator. */
	private final Random random;

	/** Meta-data about agents' friends. */
	private final Map<String, AgentMetadata> friendMetadata;

	/** Agents' desired positions. */
	private final Queue<Position> desiredPositions;
	/** Agents' checkpoints to visit. */
	private final Queue<Position> myCheckpoints;

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
	/** Current fence to be passed through. */
	private Fence currentFence;

	/** Constructor of the MyAgent class. */
	public MyAgent(String host, int port, String username, String password) {
		super(host, port, username, password);
		random = new Random(System.nanoTime());
		friendMetadata = new LinkedHashMap<String, AgentMetadata>(FRIENDS);
		desiredPositions = new LinkedList<Position>();
		myCheckpoints = new LinkedList<Position>(CHECKPOINTS);
		myNumber = null;
		isLeader = null;
		state = AgentState.init;
		map = null;
		myPosition = null;
		intendedPosition = null;
		currentFence = null;
	}

	@Override
	protected void onStart(int gridWidth, int gridHeight, int visibility) {
		printInfo("START");
		printDebug("gridWidth=" + gridWidth + " gridHeight=" + gridHeight + " visibility=" + visibility);
		map = new GameMap(gridWidth, gridHeight);
	}

    @Override
    protected Action deliberate(MASPerception percept) {
		final long t = System.currentTimeMillis();

		// refresh agents' position and the map
		myPosition = new Position(percept.getPosX(), percept.getPosY());
		map.refresh(myPosition, percept.getCellPercepts());

		processMessages();

		// decide the next action
		final Action action;
		switch (state) {
			case init:
				action = doInit();
				break;
			case walking:
				action = doWalking();
				break;
			case scouting:
				action = doScouting();
				break;
			case idle:
			case finished:
				action = doIdleWalk();
				break;
			case ready:
			case waiting:
			case terminated:
				action = Action.SKIP;
				break;
			default:
				action = doRandomWalk();
		}

		if (isLeader == Boolean.TRUE) {
			sendCommands();
		}

		printVerbose("step=" + percept.getStep() +  " action=" + action + " t=" + (System.currentTimeMillis() - t));
        return action;
    }

	/** Resets the agent into its initial state. */
	protected void reset() {
		printDebug("reset initiated");
		friendMetadata.clear();
		desiredPositions.clear();
		myNumber = null;
		isLeader = null;
		setState(AgentState.init);
		map.init();
		myPosition = null;
		intendedPosition = null;
		currentFence = null;
	}

	/** Processing of the messages in agents' inbox. */
	protected void processMessages() {
		final List<Message> messages = getNewMessages();
		for (Message message : messages) {
			// parse received data
			MessageData data = MessageUtils.parse(message);
			String type = data.getType();
			printDebug("MSG from " + message.getSender() + " [" + type + ": " + data.getData() + "]");

			// retrieve meta-data about the sender (friend agent)
			AgentMetadata metadata = friendMetadata.get(message.getSender());
			if (metadata == null) {
				metadata = new AgentMetadata(message.getSender());
				friendMetadata.put(message.getSender(), metadata);
			}

			// processing of general messages
			if ("reset".equals(type)) {
				reset();
				return;
			} else if ("myState".equals(type)) {
				metadata.state = MessageUtils.getData(data);
			} else if ("myPosition".equals(type)) {
				metadata.position = MessageUtils.getData(data);
			} else if ("myNumber".equals(type)) {
				metadata.number = MessageUtils.getData(data);
			} else if ("leader".equals(type)) {
				metadata.isLeader = MessageUtils.getData(data);
			} else if ("foundFence".equals(type)) {
				currentFence = MessageUtils.getData(data);
			}

			if (isLeader == Boolean.FALSE) {
				precessLeaderCommand(data);
			}
			if (isLeader == Boolean.TRUE) {
				processFollowerMessage(data);
			}
		}
	}

	/** Sends appropriate leader commands. */
	private void sendCommands() {
		AgentMetadata scout = null, follower = null;
		boolean allFinished = (state == AgentState.finished);
		for (AgentMetadata metadata : friendMetadata.values()) {
			allFinished &= (metadata.state == AgentState.finished);
			if (metadata.isScout == Boolean.TRUE) {
				scout = metadata;
			} else {
				follower = metadata;
			}
		}
		if (scout == null || follower == null) return;

		printVerbose("leader=" + state + " scout=" + scout.state + " follower=" + follower.state);

		// terminal state of the game
		if (allFinished) {
			printInfo("GAME OVER");
			broadcast(MessageUtils.create("terminate"));
			setState(AgentState.terminated);
			return;
		}

		// leader self-commands
		if (state == AgentState.ready && scout.state == AgentState.scouting) {
			setState(AgentState.idle);
		}

		// commands for the scout agent
		if (scout.state == AgentState.ready || scout.state == AgentState.idle) {
			sendMessage(scout.getName(), MessageUtils.create("scout"));
			scout.state = AgentState.scouting;
		}
		if (scout.state == AgentState.waiting && follower.state == AgentState.ready && state == AgentState.ready) {
			final Position furtherAfterSwitch = GameMap.move(currentFence.getPositionAfterSwitch(), currentFence.getDirection());
			sendMessage(scout.getName(), MessageUtils.create("goto", furtherAfterSwitch));
			scout.state = AgentState.walking;
		}

		// commands for the follower agent
		if (follower.state == AgentState.ready && scout.state == AgentState.scouting) {
			sendMessage(follower.getName(), MessageUtils.create("idleWalk"));
			follower.state = AgentState.idle;
		}
	}

	/** Processing of leader command messages. */
	private void precessLeaderCommand(MessageData data) {
		final String type = data.getType();
		if ("goto".equals(type)) {
			desiredPositions.add(MessageUtils.<Position>getData(data));
			setState(AgentState.walking);
		} else if ("scout".equals(type)) {
			setState(AgentState.scouting);
		} else if ("idleWalk".equals(type)) {
			setState(AgentState.idle);
		} else if ("terminate".equals(type)) {
			setState(AgentState.terminated);
		}
	}

	/** Processing of messages from the followers. */
	private void processFollowerMessage(MessageData data) {
		final String type = data.getType();
		if ("foundFence".equals(type)) {
			// leader self-command
			desiredPositions.add(currentFence.getPositionBehindFence());
			setState(AgentState.walking);

			// command for the follower agents
			final Position after = currentFence.getPositionAfterSwitch();
			for (AgentMetadata metadata : friendMetadata.values()) {
				if (metadata.isScout == Boolean.FALSE) {
					sendMessage(metadata.getName(), MessageUtils.create("goto", after));
					metadata.state = AgentState.walking;
				}
			}
		}
	}

	/** Agent initialization, establish leader and scout agent. */
	private Action doInit() {
		if (myNumber == null) {
			// send agents' number
			myNumber = random.nextLong();
			broadcast(MessageUtils.create("myNumber", myNumber));
			broadcast(MessageUtils.create("myPosition", myPosition));
		} else if (isLeader == null && friendMetadata.size() == FRIENDS) {
			// determine the leader agent
			Long max = myNumber;
			int count = 0;
			for (AgentMetadata metadata : friendMetadata.values()) {
				if (metadata.number != null) {
					count++;
					if (myNumber.compareTo(metadata.number) == 0) {
						// number collision -> reset
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
				setState(AgentState.idle);

				if (isLeader) {
					// determine the scout agent
					int num = 0;
					for (AgentMetadata metadata : friendMetadata.values()) {
						metadata.isScout = (num == 0);
						num++;
					}
				}
			}
		}

		// do nothing while in init
		return Action.SKIP;
	}

	/** Agents' walking mode. */
	private Action doWalking() {
		final Action goForIt = goForCheckpoint();
		if (goForIt != null) return goForIt;

		if (intendedPosition == null) {
			if (desiredPositions.isEmpty()) {
				setState(AgentState.ready);
			} else {
				intendedPosition = desiredPositions.poll();
				return map.planMove(myPosition, intendedPosition);
			}
		} else if (myPosition.equals(intendedPosition)) {
			intendedPosition = null;
			broadcast(MessageUtils.create("myPosition", myPosition));
		} else {
			return map.planMove(myPosition, intendedPosition);
		}

		return Action.SKIP;
	}

	/** Agents' scouting mode. */
	private Action doScouting() {
		final Action goForIt = goForCheckpoint();
		if (goForIt != null) return goForIt;

		final Action scoutDirection = map.getScoutDirection(myPosition);
		final Position nextPosition = GameMap.move(myPosition, scoutDirection);

		// test for fence switch
		if (map.get(nextPosition) == GameMap.SWITCH) {
			final Fence fence = new Fence(nextPosition, scoutDirection);
			broadcast(MessageUtils.create("foundFence", fence));
			broadcast(MessageUtils.create("myPosition", myPosition));
			setState(AgentState.waiting);
			return Action.SKIP;
		}

		return scoutDirection;
	}

	/** Agents' (blind) idle walk mode. */
	private Action doIdleWalk() {
		final Action goForIt = goForCheckpoint();
		if (goForIt != null) return goForIt;

		// move with 50% probability
		return (random.nextFloat() < 0.5f) ? Action.SKIP : map.getIdleWalkDirection(myPosition);
	}

	/** Go for the next checkpoint if it's near enough. */
	private Action goForCheckpoint() {
		if (state == AgentState.finished) {
			return null;
		}
		if (myCheckpoints.isEmpty()) {
			printInfo("FINISHED");
			setState(AgentState.finished);
			return map.getIdleWalkDirection(myPosition);
		}

		final Position checkpoint = myCheckpoints.peek();
		if (myPosition.equals(checkpoint)) {
			printInfo("CHECKPOINT " + checkpoint);
			myCheckpoints.remove();
			return Action.SKIP;
		}
		if (GameMap.isNearCheckpoint(myPosition, checkpoint)) {
			return map.planMove(myPosition, checkpoint);
		} else {
			return null;
		}
	}

	/** Performs a random walk. */
	private Action doRandomWalk() {
		return ACTIONS[random.nextInt(ACTIONS.length - 1)];
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
		if (DEBUG) System.out.println(username + "(" + state + "): " + message);
	}

	/** Prints given message to STD-OUT if in VERBOSE mode. */
	protected void printVerbose(String message) {
		if (VERBOSE) System.out.println(username + ": " + message);
	}
}
