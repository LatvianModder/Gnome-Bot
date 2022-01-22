package dev.gnomebot.app.server.json;

/**
 * @author LatvianModder
 */
public class NotJsonArrayException extends WrongJsonTypeException {
	public NotJsonArrayException() {
		super("array");
	}
}