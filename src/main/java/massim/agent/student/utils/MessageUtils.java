package massim.agent.student.utils;

import cz.agents.alite.communication.Message;
import cz.agents.alite.communication.content.Content;
import massim.agent.Position;

public class MessageUtils {

	public static Content create(String type) {
		return new Content(new MessageData(type, null, null, null, null));
	}

	public static Content create(String type, Long number) {
		return new Content(new MessageData(type, null, number, null, null));
	}

	public static Content create(String type, String string) {
		return new Content(new MessageData(type, string, null, null, null));
	}

	public static Content create(String type, Boolean bool) {
		return new Content(new MessageData(type, null, null, bool, null));
	}

	public static Content create(String type, Position position) {
		return new Content(new MessageData(type, null, null, null, position));
	}

	public static MessageData parse(Message message) {
		return (MessageData) message.getContent().getData();
	}

}
