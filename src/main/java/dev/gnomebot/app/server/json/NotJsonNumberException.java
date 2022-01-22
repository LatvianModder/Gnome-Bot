package dev.gnomebot.app.server.json;

/**
 * @author LatvianModder
 */
public class NotJsonNumberException extends WrongJsonTypeException {
	public NotJsonNumberException() {
		super("number");
	}
}