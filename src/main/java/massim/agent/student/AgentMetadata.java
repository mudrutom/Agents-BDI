package massim.agent.student;

import massim.agent.Position;

/**
 * Class encapsulating meta-data about the agent.
 */
public class AgentMetadata {

	private final String name;

	/** State of the agent. */
	public AgentState state;

	/** Agents' number. */
	public Long number;

	/** Agents' leader and scout flags. */
	public Boolean isLeader, isScout;

	/** Position of the agent. */
	public Position position;

	/** Constructor of the AgentMetadata class. */
	public AgentMetadata(String name) {
		this.name = name;
	}

	/** Returns the name of the agent. */
	public String getName() {
		return name;
	}
}
