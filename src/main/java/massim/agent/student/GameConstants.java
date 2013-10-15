package massim.agent.student;

import massim.agent.Action;
import massim.agent.Position;

/**
 * Contains the constant of the game (problem).
 */
public interface GameConstants {

	/** The number of friend agents. */
	public static final int FRIENDS = 2;

	/** The checkpoints any agent needs to visit. */
	public static final Position[] CHECKPOINTS = {
			new Position(2,2), new Position(27,2), new Position(27,27), new Position(2,27)
	};

	/** Possible actions of the agents. */
	public static final Action[] ACTIONS = {
			Action.NORTH, Action.NORTHEAST, Action.EAST, Action.SOUTHEAST,
			Action.SOUTH, Action.SOUTHWEST, Action.WEST, Action.NORTHWEST,
			Action.SKIP
	};

}
