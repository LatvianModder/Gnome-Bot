package dev.gnomebot.app.server.json;

public class NotJsonNumberException extends WrongJsonTypeException {
	public NotJsonNumberException() {
		super("number");
	}
}