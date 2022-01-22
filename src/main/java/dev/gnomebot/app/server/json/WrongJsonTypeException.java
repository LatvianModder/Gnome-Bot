package dev.gnomebot.app.server.json;

/**
 * @author LatvianModder
 */
public class WrongJsonTypeException extends IllegalArgumentException {
	public final String expectedType;

	public WrongJsonTypeException(String s) {
		super("Input JSON is not '" + s + "'!");
		expectedType = s;
	}
}