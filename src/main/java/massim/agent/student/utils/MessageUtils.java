package massim.agent.student.utils;

import cz.agents.alite.communication.Message;
import cz.agents.alite.communication.content.Content;

/**
 * Utility class for working with messages.
 */
public class MessageUtils {

	/** Creates new empty message. */
	public static Content create(String type) {
		return create(type, null);
	}

	/** Creates new message with given data. */
	@SuppressWarnings("unchecked")
	public static <T> Content create(String type, T data) {
		final Class<T> clazz = (data == null) ? null : (Class<T>) data.getClass();
		return new Content(new MessageData<T>(type, clazz, data));
	}

	/** Parses message data from given message. */
	public static MessageData parse(Message message) {
		return (MessageData) message.getContent().getData();
	}

	/** Convince method to get typed data of the message. */
	@SuppressWarnings("unchecked")
	public static <T> T getData(MessageData messageData) {
		try {
			return (T) messageData.getData();
		} catch (ClassCastException e) {
			return null;
		}
	}
}
