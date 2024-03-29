package dev.gnomebot.app.server.json;

public class MissingValueException extends IllegalArgumentException {
	public final String key;

	public MissingValueException(String k) {
		super("'" + k + "' not found in input object!");
		key = k;
	}
}