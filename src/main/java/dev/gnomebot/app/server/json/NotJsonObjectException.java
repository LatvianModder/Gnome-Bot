package dev.gnomebot.app.server.json;

/**
 * @author LatvianModder
 */
public class NotJsonObjectException extends WrongJsonTypeException {
	public NotJsonObjectException() {
		super("object");
	}
}