package massim.agent.student.game;

import massim.agent.Action;
import massim.agent.CellPercept;
import massim.agent.Position;

import java.util.Arrays;
import java.util.Collection;

/**
 * Representation of the game map with utility methods.
 */
public class GameMap implements GameConstants {

	/** Map cells content constants. */
	public static final char FREE = ' ', WALL = '#', FENCE = '+', FENCE_OPEN = '.', SWITCH = '%', AGENT = '@', UNKNOWN = '*';

	/** The map cells. */
	private final char map[][];

	/** Constructor of the GameMap class. */
	public GameMap(int width, int height) {
		map = new char[width][height];
		init();
	}

	/** Initializes new map. */
	public void init() {
		for (char[] column : map) {
			Arrays.fill(column, UNKNOWN);
		}
	}

	/** Refreshes the map with given cell percepts. */
	public void refresh(Position position, Collection<CellPercept> cells) {
		for (CellPercept cell : cells) {
			int x = position.getX() + cell.getX();
			int y = position.getY() + cell.getY();
			if (cell.isEmpty() || cell.isInCorral()) {
				map[x][y] = FREE;
			} else if (cell.containsObstacle() || cell.containsCow()) {
				map[x][y] = WALL;
			} else if (cell.containsClosedFence()) {
				map[x][y] = FENCE;
			} else if (cell.containsOpenFence()) {
				map[x][y] = FENCE_OPEN;
			} else if (cell.containsFenceSwitch()) {
				map[x][y] = SWITCH;
			} else if (cell.containsAgent()) {
				map[x][y] = AGENT;
			} else {
				map[x][y] = UNKNOWN;
			}
		}
	}

	/** Returns a map cell at given position. */
	public char get(Position position) {
		return map[position.getX()][position.getY()];
	}

	/** Plans the next move from one position to the other. */
	public Action planMove(Position fromPos, Position toPos) {
		Action action = getAction(fromPos, toPos);
		Position next = move(fromPos, action);
		while (map[next.getX()][next.getY()] != FREE && map[next.getX()][next.getY()] != FENCE_OPEN) {
			action = next(action);
			next = move(fromPos, action);
		}
		return action;
	}

	/**
	 * Returns a scouting direction (action) for given agent position.<br/>
	 * Note: Scouting route goes along the border of the map.
	 */
	public Action getScoutDirection(Position agentPos) {
		final int x = agentPos.getX(), y = agentPos.getY();
		final int left = 1, center = map.length / 2, right = map.length - 2;
		final int top = 1, middle = map[0].length / 2, bottom = map[0].length - 2;

		if (x > left && x < right && y > top && y < bottom) {
			// direction to reach map border
			if (Math.abs(x - center) < Math.abs(y - middle)) {
				return (y < middle) ? Action.NORTH : Action.SOUTH;
			} else {
				return (x < center) ? Action.WEST : Action.EAST;
			}
		} else if (x <= left && y > top) {
			return Action.NORTH;
		} else if (y <= top && x < right) {
			return Action.EAST;
		} else if (x >= right && y < bottom) {
			return Action.SOUTH;
		} else if (y >= bottom && x > left) {
			return Action.WEST;
		}
		return Action.SKIP;
	}

	/** Returns the map as a string. */
	public String toString() {
		final StringBuilder sb = new StringBuilder(map.length + map.length * map[0].length);
		for (int row = 0, height = map[0].length; row < height; row++) {
			for (char[] column : map) {
				sb.append(column[row]);
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	/** Returns an Action to get form one position to the other. */
	public static Action getAction(Position fromPos, Position toPos) {
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

	/** Returns new position after the one move. */
	public static Position move(Position position, Action action) {
		final int x = position.getX(), y = position.getY();
		switch (action) {
			case NORTH: return new Position(x, y - 1);
			case SOUTH: return new Position(x, y + 1);
			case WEST: return new Position(x - 1, y);
			case EAST: return new Position(x + 1, y);
			case NORTHWEST: return new Position(x - 1, y - 1);
			case NORTHEAST: return new Position(x + 1, y - 1);
			case SOUTHWEST: return new Position(x - 1, y + 1);
			case SOUTHEAST: return new Position(x + 1, y + 1);
			default: return position;
		}
	}

	/** Returns the next action after the given action. */
	public static Action next(Action action) {
		switch (action) {
			case NORTH: return Action.EAST;
			case NORTHEAST: return Action.EAST;
			case EAST: return Action.SOUTH;
			case SOUTHEAST: return Action.SOUTH;
			case SOUTH: return Action.WEST;
			case SOUTHWEST: return Action.WEST;
			case WEST: return Action.NORTH;
			case NORTHWEST: return Action.NORTH;
			default: return Action.SKIP;
		}
	}

	/** Returns the inverse action for the given action. */
	public static Action inverse(Action action) {
		switch (action) {
			case NORTH: return Action.SOUTH;
			case NORTHEAST: return Action.SOUTHWEST;
			case EAST: return Action.WEST;
			case SOUTHEAST: return Action.NORTHWEST;
			case SOUTH: return Action.NORTH;
			case SOUTHWEST: return Action.NORTHEAST;
			case WEST: return Action.EAST;
			case NORTHWEST: return Action.SOUTHEAST;
			default: return Action.SKIP;
		}
	}

	/** Returns <tt>true</tt> IFF the agent is near given checkpoint. */
	public static boolean isNearCheckpoint(Position agentPos, Position checkpoint) {
		return getDistance(agentPos, checkpoint) < 5;
	}

	/** Returns the Manhattan distance between given two points. */
	public static int getDistance(Position one, Position two) {
		return Math.max(Math.abs(one.getX() - two.getX()), Math.abs(one.getY() - two.getY()));
	}
}
