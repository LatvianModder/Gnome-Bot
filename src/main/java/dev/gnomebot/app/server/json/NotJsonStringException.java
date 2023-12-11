package dev.gnomebot.app.server.json;

public class NotJsonStringException extends WrongJsonTypeException {
	public NotJsonStringException() {
		super("string");
	}
}