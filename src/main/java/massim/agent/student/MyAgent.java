package massim.agent.student;

import cz.agents.alite.communication.Message;
import massim.agent.Action;
import massim.agent.CellPercept;
import massim.agent.MASAgent;
import massim.agent.MASPerception;

import java.util.List;

/**
 * This agent illustrates the API available in the MASAgent class.
 */
public class MyAgent extends MASAgent {

    public MyAgent(String host, int port, String username, String password) {
       super(host, port, username, password);
    }

    @Override
    protected void onStart(int gridWidth, int gridHeight, int visibility) {
        System.out.println(getUsername() + ": The world is " + gridWidth + " cells wide and " + gridHeight + " cells high.");
    }

    @Override
    protected Action deliberate(MASPerception percept) {

        // You can obtain the list of new messages using this method
        List<Message> newMessages = getNewMessages();

        for (Message message : newMessages) {
            System.out.println(getUsername() + ": Yay, I have received a message from " + message.getSender() + " with the content " + message.getContent());
            // You can reply to a message using the following method
            if (Math.random() < 0.3) {
                sendMessage(message.getSender(), new HelloContent("Dear " + message.getSender() + ", thank you for your message. Yours " + getUsername()));
            }
        }

        System.out.println(getUsername() + ": My current position is " + percept.getPosX() + ", " + percept.getPosY());
        System.out.println(getUsername() + " I see  " + percept.getCellPercepts().size() + " cells around me.");

        for (CellPercept cellPercept : percept.getCellPercepts()) {

            // the postion of the cell can be obtained using these two methods
            cellPercept.getX();
            cellPercept.getY();

            // the contents of the cell can be determined using one of these methods
            cellPercept.containsObstacle();
            cellPercept.containsAgent();
            cellPercept.containsFenceSwitch();
            cellPercept.containsOpenFence();
            cellPercept.containsClosedFence();
            cellPercept.isEmpty();
        }

        // the agent can send a message to all others using the following two method
        broadcast(new HelloContent("Hello, I am " + getUsername()));

        Action action;

        // The available actions are the following
        Action[] actions = {Action.EAST, Action.WEST, Action.NORTH, Action.SOUTH,
                Action.NORTHEAST, Action.NORTHWEST,
                Action.SOUTHEAST, Action.SOUTHWEST,
                Action.SKIP};

        // Pick random action
        action = actions[(int)(Math.random()*actions.length)];


        return action;
    }

}
