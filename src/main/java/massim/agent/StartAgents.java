package massim.agent;

import cz.agents.alite.communication.DefaultCommunicator;
import cz.agents.alite.communication.channel.CommunicationChannelException;
import cz.agents.alite.communication.channel.DirectCommunicationChannel;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.ReceiverTable;
import massim.agent.student.MyAgent;

import java.util.LinkedList;

public class StartAgents {

    public static void main(String[] args) {
        startAgents("localhost", 12300, 3);
    }

    public static void startAgents(String host, int port, int nAgents) {

        ReceiverTable receiverTable = new DirectCommunicationChannel.DefaultReceiverTable();

        LinkedList<String> agentNames = new LinkedList<String>();

        for (int i = 1; i <= nAgents; i++) {
            agentNames.add("b" + i);
        }

        for (int i = 1; i <= nAgents; i++) {

            String agentName = "b" + i;

            final MASAgent agent = new MyAgent(host, port, agentName, "1");

            DefaultCommunicator communicator = new DefaultCommunicator(agentName);
            try {
                communicator.addChannel(new DirectCommunicationChannel(communicator, receiverTable));
            } catch (CommunicationChannelException e) {
                e.printStackTrace();
            }
            communicator.addMessageHandler(agent);

            // setup communication infrastructure
            agent.setCommunicator(communicator, agentNames);

            // start in a new thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    agent.start();
                }
            }).run();

        }
    }

}
