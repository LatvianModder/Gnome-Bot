package dev.gnomebot.app.server.json;

public class NotJsonArrayException extends WrongJsonTypeException {
	public NotJsonArrayException() {
		super("array");
	}
}