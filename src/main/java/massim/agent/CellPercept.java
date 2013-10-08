package massim.agent;


public class CellPercept {

    public CellPercept(int x, int y, boolean obstacle, boolean agent,
            boolean cow, int cowId, boolean corral, boolean fenceSwitch,
            boolean openFence, boolean closedFence, boolean empty) {
        super();
        this.x = x;
        this.y = y;
        this.obstacle = obstacle;
        this.agent = agent;
        this.cow = cow;
        this.cowId = cowId;
        this.corral = corral;
        this.fenceSwitch = fenceSwitch;
        this.openFence = openFence;
        this.closedFence = closedFence;
        this.empty = empty;
    }

    /** x-coordinate of the cell relative to the current position of the agent */
    final int x;
    /** y-coordinate of the cell relative to the current position of the agent */
    final int y;

    // content

    final boolean obstacle;

    final boolean agent;

    final boolean cow;
    final int cowId;

    final boolean corral;
    final boolean fenceSwitch;

    final boolean openFence;
    final boolean closedFence;

    final boolean empty;

    @Override
    public String toString() {
        return "CellPercept [x=" + x + ", y=" + y + ", obstacle=" + obstacle
                + ", agent=" + agent + ", cow=" + cow + ", cowId=" + cowId
                + ", corral=" + corral + ", fenceSwitch=" + fenceSwitch
                + ", openFence=" + openFence + ", closedFence=" + closedFence
                + ", empty=" + empty + "]";
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean containsObstacle() {
        return obstacle;
    }

    public boolean containsAgent() {
        return agent;
    }

    public boolean containsCow() {
        return cow;
    }

    public int getCowId() {
        return cowId;
    }

    public boolean isInCorral() {
        return corral;
    }

    public boolean containsFenceSwitch() {
        return fenceSwitch;
    }

    public boolean containsClosedFence() {
        return closedFence;
    }

    public boolean containsOpenFence() {
        return openFence;
    }

    public boolean isEmpty() {
        return empty;
    }

}