package massim.agent.student.utils;

import java.io.Serializable;

/**
 * Message data container class.
 */
public class MessageData<T> implements Serializable {

	private static final long serialVersionUID = -8701920249370513743L;

	private final String type;
	private final Class<T> clazz;
	private final T data;

	/** Constructor of the MessageData class. */
	public MessageData(String type, Class<T> clazz, T data) {
		this.type = type;
		this.clazz = clazz;
		this.data = data;
	}

	/** Returns type string of the message. */
	public String getType() {
		return type;
	}

	/** Returns class type of the message data. */
	public Class<T> getClazz() {
		return clazz;
	}

	/** Returns the message data itself. */
	public T getData() {
		return data;
	}
}
