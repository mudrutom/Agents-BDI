package massim.agent.student.utils;

import massim.agent.Position;

import java.io.Serializable;

public class MessageData implements Serializable {

	private final String type;
	private final String string;
	private final Long number;
	private final Boolean bool;
	private final Position position;

	public MessageData(String type, String string, Long number, Boolean bool, Position position) {
		this.type = type;
		this.string = string;
		this.number = number;
		this.bool = bool;
		this.position = position;
	}

	public String getType() {
		return type;
	}

	public String getString() {
		return string;
	}

	public Long getNumber() {
		return number;
	}

	public Boolean getBool() {
		return bool;
	}

	public Position getPosition() {
		return position;
	}
}
