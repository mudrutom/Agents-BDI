package massim.agent.student.game;

import massim.agent.Action;
import massim.agent.Position;

import java.util.Arrays;
import java.util.List;

/**
 * Contains the constant of the game (problem).
 */
public interface GameConstants {

	/** The number of friend agents. */
	public static final int FRIENDS = 2;

	/** The checkpoints any agent needs to visit. */
	public static final List<Position> CHECKPOINTS = Arrays.asList(
			new Position(2, 2), new Position(27, 2), new Position(27, 27), new Position(2, 27)
	);

	/** Possible actions of the agents. */
	public static final Action[] ACTIONS = {
			Action.NORTH, Action.NORTHEAST, Action.EAST, Action.SOUTHEAST,
			Action.SOUTH, Action.SOUTHWEST, Action.WEST, Action.NORTHWEST,
			Action.SKIP
	};

}
