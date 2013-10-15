package massim.agent;

import cz.agents.alite.communication.Communicator;
import cz.agents.alite.communication.Message;
import cz.agents.alite.communication.MessageHandler;
import cz.agents.alite.communication.content.Content;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/** Description of an agent for the simulation */
public abstract class MASAgent extends AbstractAgent implements MessageHandler {

	interface OnPositionChangedCallback {
		void positionChanged(Position pos);
	}

	Communicator communicator;
	List<String> agents = new LinkedList<String>();
	List<Message> inbox = new LinkedList<Message>();
	int nAgents;

	private OnPositionChangedCallback positionChangedCallback;

	public MASAgent(String host, int port, String username, String password) {
		this.setUsername(username);
		this.setPassword(password);
		this.setHost(host);
		this.setPort(port);
	}

	@Override
	public synchronized void notify(Message msg) {
		inbox.add(msg);
	}

	protected synchronized List<Message> getNewMessages() {
		LinkedList<Message> newMessages = new LinkedList<Message>(inbox);
		inbox = new LinkedList<Message>();
		return newMessages;
	}

	public void setCommunicator(Communicator communicator, List<String> agents) {
		this.communicator = communicator;
		this.agents = new LinkedList<String>(agents);
		this.nAgents = agents.size();
	}

	protected void sendMessage(String receiver, Content content) {
		Message msg = communicator.createMessage(content);
		LinkedList<String> receivers = new LinkedList<String>();
		receivers.add(receiver);
		msg.addReceivers(receivers);
		communicator.sendMessage(msg);
	}

	protected void broadcast(Content content) {
		Message msg = communicator.createMessage(content);
		LinkedList<String> receivers = new LinkedList<String>();
		for (String agent : agents) {
			if (!agent.equals(getUsername())) {
				receivers.add(agent);
			}
		}
		msg.addReceivers(receivers);
		communicator.sendMessage(msg);
	}

	@Override
	public void processSimulationStart(Element perception, long currentTime) {
		super.processSimulationStart(perception, currentTime);

		int gridWidth = Integer.parseInt(perception.getAttribute("gsizex"));
		int gridHeight = Integer.parseInt(perception.getAttribute("gsizey"));

		int visibility = Integer.parseInt(perception.getAttribute("lineOfSight"));
		onStart(gridWidth, gridHeight, visibility);
	}

	protected abstract void onStart(int gridWidth, int gridHeight, int visibility);

	@Override
	public void processRequestAction(Element perception, Element target, long currentTime, long deadline) {
		// process percepts

		int posX = Integer.parseInt(perception.getAttribute("posx"));
		int posY = Integer.parseInt(perception.getAttribute("posy"));

		if (positionChangedCallback != null) {
			positionChangedCallback.positionChanged(new Position(posX, posY));
		}

		int cowsInCorral = Integer.parseInt(perception.getAttribute("cowsInCorral"));

		int step = Integer.parseInt(perception.getAttribute("step"));

		// parse cell content data

		Collection<CellPercept> cellPercepts = new LinkedList<CellPercept>();

		NodeList nodeList = perception.getElementsByTagName("cell");

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;

				int x = Integer.parseInt(element.getAttribute("x"));
				int y = Integer.parseInt(element.getAttribute("y"));

				NodeList children = element.getChildNodes();
				Element childElement;

				boolean obstacle = false;
				boolean agent = false;
				boolean cow = false;
				int cowId = -1;
				boolean corral = false;
				boolean fenceSwitch = false;
				boolean openFence = false;
				boolean closedFence = false;
				boolean empty = false;

				for (int j = 0; j < children.getLength(); j++) {
					if (children.item(j).getNodeType() == Node.ELEMENT_NODE) {
						childElement = (Element) children.item(j);
						String elementName = childElement.getNodeName();

						if (elementName.equals("agent")) {
							agent = true;
						}
						if (elementName.equals("obstacle")) {
							obstacle = true;
						}
						if (elementName.equals("cow")) {
							cow = true;
							cowId = Integer.parseInt(childElement.getAttribute("ID"));
						}
						if (elementName.equals("corral")) {
							corral = true;
						}
						if (elementName.equals("switch")) {
							fenceSwitch = true;
						}
						if (elementName.equals("fence")) {
							boolean open = Boolean.parseBoolean(childElement.getAttribute("open"));
							if (open) {
								openFence = true;
							}
							else {
								closedFence = true;
							}
						}
						if (elementName.equals("empty")) {
							empty = true;
						}
					}
				}

				cellPercepts.add(new CellPercept(x, y, obstacle, agent, cow, cowId, corral, fenceSwitch, openFence, closedFence, empty));
			}
		}

		MASPerception percept = new MASPerception(posX, posY, cowsInCorral, step, cellPercepts);

		Action action = deliberate(percept);

		target.setAttribute("type", action.toString().toLowerCase());
	}

	protected abstract Action deliberate(MASPerception percept);

	public int getNoOfAgents() {
		return nAgents;
	}

	public void registerPositionChangedCallback(OnPositionChangedCallback callback) {
		this.positionChangedCallback = callback;
	}

}
