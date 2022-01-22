package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.server.HTTPResponseCode;

/**
 * @author LatvianModder
 */
public class HTTPCodeException extends Exception {
	public final HTTPResponseCode responseCode;
	public final String msg;

	public HTTPCodeException(HTTPResponseCode c, String message) {
		super(c.code + " " + c.message + ": " + message);
		responseCode = c;
		msg = message;
	}
}
