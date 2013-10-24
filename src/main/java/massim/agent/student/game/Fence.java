package massim.agent.student.game;

import massim.agent.Action;
import massim.agent.Position;

import java.io.Serializable;

/**
 * A class representing a fence in the game.
 */
public class Fence implements Serializable {

	private static final long serialVersionUID = -4959623460936304569L;

	private final Position switchPosition;
	private final Action direction;

	/** Constructor of the Fence class. */
	public Fence(Position switchPosition, Action direction) {
		this.switchPosition = switchPosition;
		this.direction = direction;
	}

	/** Returns the position of the fence switch. */
	public Position getSwitchPosition() {
		return switchPosition;
	}

	/** Returns the direction of the fence. */
	public Action getDirection() {
		return direction;
	}

	/** Returns the position after the fence switch. */
	public Position getPositionAfterSwitch() {
		return GameMap.move(switchPosition, direction);
	}

	/** Returns the position behind the fence. */
	public Position getPositionBehindFence() {
		final int x = switchPosition.getX(), y = switchPosition.getY();
		final Position inFence;
		switch (direction) {
			case NORTH: inFence = new Position(x + 2, y); break;
			case EAST: inFence = new Position(x, y + 2); break;
			case SOUTH: inFence = new Position(x - 2, y); break;
			case WEST: inFence = new Position(x, y - 2); break;
			default: inFence = switchPosition;
		}
		// position two steps beside switch and two steps behind fence
		return GameMap.move(GameMap.move(inFence, direction), direction);
	}

	@Override
	public String toString() {
		return "Fence { switch=" + switchPosition  + ", direction=" + direction + " }";
	}
}
