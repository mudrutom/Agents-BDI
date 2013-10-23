package massim.agent;

import java.io.Serializable;

public class Position implements Serializable {

	private static final long serialVersionUID = -4193199787421713050L;

	private final int x, y;

	public Position(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof Position) {
			final Position other = (Position) obj;
			return (x == other.x && y == other.y);
		}
		return false;
	}

	@Override
	public String toString() {
		return "Point [x=" + x + ", y=" + y + "]";
	}
}
