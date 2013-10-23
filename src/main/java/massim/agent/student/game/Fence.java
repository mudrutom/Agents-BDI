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

	private boolean opened;

	/** Constructor of the Fence class. */
	public Fence(Position switchPosition, Action direction) {
		this.switchPosition = switchPosition;
		this.direction = direction;
		opened = false;
	}

	/** Returns the position of the fence switch. */
	public Position getSwitchPosition() {
		return switchPosition;
	}

	/** Returns the direction of the fence. */
	public Action getDirection() {
		return direction;
	}

	/** Indicates whether the fence is opened. */
	public boolean isOpened() {
		return opened;
	}

	/** @param opened the fence opened status */
	public void setOpened(boolean opened) {
		this.opened = opened;
	}

	/** Returns the position before fence switch. */
	public Position getPositionBeforeSwitch() {
		return GameMap.move(switchPosition, GameMap.inverse(direction));
	}

	/** Returns the position after fence switch. */
	public Position getPositionAfterSwitch() {
		return GameMap.move(switchPosition, direction);
	}

	/** Returns the position after fence. */
	public Position getPositionAfterFence() {
		return GameMap.move(getPositionAfterSwitch(), direction);
	}

	@Override
	public String toString() {
		return "Fence { switch=" + switchPosition  + ", direction=" + direction + ", opened=" + opened + " }";
	}
}
