package dev.gnomebot.app.server.json;

public class NotJsonObjectException extends WrongJsonTypeException {
	public NotJsonObjectException() {
		super("object");
	}
}