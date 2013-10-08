package massim.agent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class provides a very simple foundation to agents. It will only connect once (no automatic reconnection).
 * It will authenticate itself and wait for any messages. You can send ping using "sendPing" whenever
 */
public abstract class AbstractAgent {

    @SuppressWarnings("serial")
    private class SocketClosedException extends Exception {}
    private int networkport;
    private String networkhost;
    private InetSocketAddress socketaddress;
    private Socket socket;

    private InputStream inputstream;
    private OutputStream outputstream;
    protected String username;
    private String password;

    protected DocumentBuilderFactory documentbuilderfactory;
    private TransformerFactory transformerfactory;

    protected static Logger logger=Logger.getLogger("agentLog.log");

    public static String getDate() {
        Date dt = new Date();
        SimpleDateFormat df = new SimpleDateFormat("HH-mm-ss_dd-MM-yyyy");
        return df.format(dt);
    }

    public AbstractAgent() {

        networkhost = "localhost";
        networkport = 0;

        socket = new Socket();
        documentbuilderfactory=DocumentBuilderFactory.newInstance();
        transformerfactory = TransformerFactory.newInstance();
    }

    public String getHost() {
        return networkhost;
    }

    public void setHost(String host) {
        this.networkhost = host;
    }

    public int getPort() {
        return networkport;
    }

    public void setPort(int port) {
        this.networkport=port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username=username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password=password;
    }

    /**
     * Starts the agent main thread.
     * @see @link agentThread
     */
    public void start() {
        new Thread() {
            public void run() {agentThread();}
        }.start();
    }

/**
 * Provides a easy way for the authentication against a server.
 * It must be called before the agent is bind to the server and the outputstream
 * is initialized.
 *
 * @param username Username of the actual agent.
 * @param password Password associated with the username.
 * @throws java.io.IOException When the conection have not been initialized.
 */
    public void sendAuthentication(String username, String password) throws IOException{

        try {
            Document doc = documentbuilderfactory.newDocumentBuilder().newDocument();
            Element root = doc.createElement("message");
            root.setAttribute("type","auth-request");
            doc.appendChild(root);

            Element auth = doc.createElement("authentication");
            auth.setAttribute("username",username);
            auth.setAttribute("password",password);
            root.appendChild(auth);

            this.sendDocument(doc);

        } catch (ParserConfigurationException e) {
            System.err.println("unable to create new document for authentication.");
            e.printStackTrace();
        }
    }
/**
 * Waits for an authentication response from the server. It must be called
 * after the <code>sendAuthentication</code> method call.
 * @return true when the authentication hat been succesful, false othercase.
 * @throws java.io.IOException When the conection have not been initialized.
 */
    public boolean receiveAuthenticationResult() throws IOException {

        try {
            Document doc = receiveDocument();
            Element root = doc.getDocumentElement();
            if (root==null) return false;
            if (!root.getAttribute("type").equalsIgnoreCase("auth-response")) return false;
            NodeList nl = root.getChildNodes();
            Element authresult = null;
            for (int i=0;i<nl.getLength();i++) {
                Node n = nl.item(i);
                if (n.getNodeType()==Element.ELEMENT_NODE && n.getNodeName().equalsIgnoreCase("authentication")) {
                    authresult = (Element) n;
                    break;
                }
            }
            if (!authresult.getAttribute("result").equalsIgnoreCase("ok")) return false;
        } catch (SAXException e) {
            e.printStackTrace();
            return false;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return false;
        } catch (SocketClosedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    /**
     * Unifies the authentication process. It sends the authentication
     * and then waits for the response from the server.
     * @param username Username of the actual agent.
     * @param password Password associated with the username.
     * @return true when the authentication hat been succesful, false othercase.
     * @throws java.io.IOException When the conection have not been initialized.
     * @see #sendAuthentication(String, String) sendAuthentication
     * @see #receiveAuthenticationResult() receiveAuthenticationResult
     */
    public boolean doAuthentication(String username, String password) throws IOException {
        sendAuthentication(username, password);
        return receiveAuthenticationResult();
    }


    /**
     * This method manages the reception of a packet from the server. It takes no
     * parameters and suposes the authentication is done and hat succeed. It also writes
     * to stderr the contents of the package.
     *
     * @return a byte array with the response from the server.
     * @throws java.io.IOException When the conection have not been initialized.
     * @throws massim.agent.AbstractAgent.SocketClosedException
     */
    public byte[] receivePacket() throws IOException, SocketClosedException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read = inputstream.read();
        while (read!=0) {
            if (read==-1) {
                throw new SocketClosedException();
            }
            buffer.write(read);
            read = inputstream.read();
        }
        String s = "Server -> Agent: AgentName " +this.username+"\n"+buffer.toString();

        //System.err.println(s);
        synchronized (logger) {
            logger.log(Level.ALL, s);
        }


        return buffer.toByteArray();
    }


    /**
     * Receives a packet from the server using the <code>receivePacket<code> method
     * and converts the received data to a XML Document object.
     *
     * @return A valid XML Document object.
     * @throws org.xml.sax.SAXException When the received data is not wellformed.
     * @throws java.io.IOException When the conection have not been initialized.
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws massim.agent.AbstractAgent.SocketClosedException
     * @see #receivePacket() receivePacket
     */
    public Document receiveDocument() throws SAXException, IOException, ParserConfigurationException, SocketClosedException {

        byte[] raw = receivePacket();
        Document doc = documentbuilderfactory.newDocumentBuilder().parse(new ByteArrayInputStream(raw));
        return doc;
    }


    /**
     * Is the main agent's thread. It makes all the agent's work.
     * First it manages the authentication, if it is not successful it will end.
     * Then calls the <code>processLogin</code> method that is an user specified method. And next
     * it remains in an infininte loop receiving and processing messages from the server.
     * The messages must start with the <code>message<code> element.
     * If it encounters any problem with the reception it ends execution.
     *
     * @see #doAuthentication(String, String) doAuthentication
     * @see #processLogIn() processLogIn
     * @see #receiveDocument() receiveDocument
     * @see #processMessage(org.w3c.dom.Element) processMessage
     *
     */
    public void agentThread() {

        try {
            socketaddress = new InetSocketAddress(networkhost,networkport);
            socket.connect(socketaddress);
            inputstream = socket.getInputStream();
            outputstream = socket.getOutputStream();

            boolean auth = doAuthentication(username, password);
            if (!auth) {
                System.err.println("Authentication failed");
                return;
            }
            processLogIn();
            while(true) {
                Document doc = null;
                try {
                    doc = receiveDocument();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }

                Element el_root = doc.getDocumentElement();

                if (el_root == null) {
                    System.err.println("No document element found");
                    continue;
                }

                if (el_root.getNodeName().equals("message")) {
                    if (!processMessage(el_root)) break;
                } else {
                    System.err.println("Unknown document received");
                }
            }

        } catch (IOException e) {
            System.err.println("IOException");
            e.printStackTrace();
            return;
        } catch (SocketClosedException e) {
            System.err.println("Socket was closed");
        }
    }


    /**
     * This method parses the message received from the server and selects
     * the right action to do next. The messages must be of the type:
     * <ol>
     * <li><code>request-action</code></li>
     * <li><code>sim-start</code></li>
     * <li><code>sim-end</code></li>
     * </ol>
     *
     * If the type is one of the first three, it builds a valid response
     * envelop and calls the method related with the actual request wich
     * will build the correct response content for the server. The responsible of
     * sending such response is this method also, after it is builded.
     *
     * @param el_message XML Element object containing the message to process.
     * @return true always
     *
     * @see #processRequestAction(org.w3c.dom.Element, org.w3c.dom.Element, long, long) processRequestAction
     * @see #processSimulationStart(org.w3c.dom.Element, long) processSimulationStart
     * @see #processSimulationEnd(org.w3c.dom.Element, long) processSimulationEnd
     * @see #sendDocument(org.w3c.dom.Document) sendDocument
     */
    public boolean processMessage(Element el_message) {

        String type = el_message.getAttribute("type");
        if (type.equals("request-action") || type.equals("sim-start") || type.equals("sim-end")) {
            //get perception
            Element el_perception = null;
            NodeList nl = el_message.getChildNodes();
            String infoelementname ="perception";

            if (type.equals("request-action")) {
                infoelementname = "perception";
            } else if (type.equals("sim-start")) {
                infoelementname = "simulation";
            } else if (type.equals("sim-end")) {
                infoelementname = "sim-result";
            }

            for (int i=0;i<nl.getLength();i++) {
                Node n = nl.item(i);
                if (n.getNodeType()==Element.ELEMENT_NODE && n.getNodeName().equalsIgnoreCase(infoelementname)) {
                    if (el_perception==null) el_perception = (Element) n; else {
                        System.err.println("perception message doesn't contain right number of perception elements");
                        return true;
                    }
                }
            }

            Document doc = null;
            try {
                doc = documentbuilderfactory.newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                System.err.println("parser config error");
                e.printStackTrace();
                System.exit(1);
            }
            Element el_response = doc.createElement("message");

            doc.appendChild(el_response);
            Element el_action=doc.createElement("action");
            el_response.setAttribute("type","action");
            el_response.appendChild(el_action);

            long currenttime = 0;
            try {
                currenttime = Long.parseLong(el_message.getAttribute("timestamp"));
            } catch (NumberFormatException e) {
                System.err.println("number format invalid");
                e.printStackTrace();
                return true;
            }

            long deadline = 0;

            if (type.equals("request-action")) {

                try {
                    deadline = Long.parseLong(el_perception.getAttribute("deadline"));
                } catch (NumberFormatException e) {
                    System.err.println("number format invalid");
                    e.printStackTrace();
                    return true;
                }
                processRequestAction(el_perception, el_action, currenttime, deadline);
            } else if (type.equals("sim-start")) {
                processSimulationStart(el_perception, currenttime);
            } else if (type.equals("sim-end")) {
                processSimulationEnd(el_perception, currenttime);
            }

            el_action.setAttribute("id",el_perception.getAttribute("id"));

            /*if(deadline > 0){

                Thread t = new Thread();
                Random r = new Random();
                System.out.println("Timestamp: " + currenttime);
                System.out.println("Deadline: " + deadline);

                double time = deadline - currenttime;
                System.out.println("Time: " + time);


                PNO, 21.03.2007, Simulation of the slow network removed!

                int random = r.nextInt((int)time + 200);

                System.out.println("Random: " + random);

                t.start();

                try{

                    t.sleep(random);
                }
                catch(Exception e) {};

                t.interrupt();

            }*/

            try {

                // sending of action only for request-action message!!!
                if(type.equals("request-action")) sendDocument(doc);

            } catch (IOException e) {
                System.err.println("IO Exception while trying to send action");
                e.printStackTrace();
                System.exit(1);
            }

        }
        return true;
    }

    public void processRequestAction(Element perception, Element target, long currenttime, long deadline) {
        //System.err.println("---#-#-#-#-#-#-- processPerception --#-#-#-#-#-#---");
    }

    public void processSimulationEnd(Element perception, long currenttime) {
        //System.err.println("---#-#-#-#-#-#-- processSimEnd --#-#-#-#-#-#---");
    }

    public void processSimulationStart(Element perception, long currenttime) {
        //System.err.println("---#-#-#-#-#-#-- processSimStart --#-#-#-#-#-#---");
    }

    public void processLogIn() {
        //System.err.println("---#-#-#-#-#-#-- login --#-#-#-#-#-#---");
    }


    /**
     *Sends an specified XML Document to the server.
     * @param doc An XML Document object containing the message to send.
     * @throws java.io.IOException
     */
    public void sendDocument(Document doc) throws IOException {


        try {
            transformerfactory.newTransformer().transform(new DOMSource(doc),new StreamResult(outputstream));

            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            transformerfactory.newTransformer().transform(new DOMSource(doc),new StreamResult(temp));
            String s = "Agent -> Server:\n"+temp.toString();
            //System.err.println(s);
            logger.log(Level.ALL,s);
            outputstream.write(0);
            outputstream.flush();
        } catch (TransformerConfigurationException e) {
            System.err.println("transformer config error");
            e.printStackTrace();
            System.exit(1);
        } catch (TransformerException e) {
            System.err.println("transformer error error");
            e.printStackTrace();
            System.exit(1);
        }
    }

}
