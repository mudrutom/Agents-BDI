package massim.agent;

public interface MASAgentFactory {
   MASAgent  createAgent(String host, int port, String username, String password);
}
