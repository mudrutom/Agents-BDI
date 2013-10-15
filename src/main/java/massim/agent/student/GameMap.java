package massim.agent.student;

import massim.agent.Action;
import massim.agent.CellPercept;
import massim.agent.Position;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Representation of the game map with utility methods.
 */
public class GameMap implements GameConstants {

	/** Map cells content constants. */
	public static final char FREE = ' ', WALL = '#', FENCE = '+', FENCE_OPEN = '.', SWITCH = 'O', AGENT = 'A';

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
			Arrays.fill(column, WALL);
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
			}
		}
	}

	/** Finds all fence switches in the map. */
	public List<Position> findAllSwitches() {
		final List<Position> switches = new LinkedList<Position>();
		for (int col = 0, width = map.length; col < width; col++) {
			for (int row = 0, height = map[col].length; row < height; row++) {
				if (map[col][row] == SWITCH) {
					switches.add(new Position(col, row));
				}
			}
		}
		return switches;
	}

	public List<Action> planPath(Position fromPos, Position toPos) {
		// TODO plan the path
		return Collections.emptyList();
	}

	/** Returns the map as a string */
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

	/** Returns new position after on move. */
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
}
