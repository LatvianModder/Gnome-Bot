package dev.gnomebot.app.server.json;

/**
 * @author LatvianModder
 */
public class NotJsonStringException extends WrongJsonTypeException {
	public NotJsonStringException() {
		super("string");
	}
}