package dev.gnomebot.app.server.handler;

import io.javalin.http.HttpStatus;

/**
 * @author LatvianModder
 */
public class HTTPCodeException extends Exception {
	public final HttpStatus responseCode;
	public final String msg;

	public HTTPCodeException(HttpStatus c, String message) {
		super(c.getCode() + " " + c.getMessage() + ": " + message);
		responseCode = c;
		msg = message;
	}
}
