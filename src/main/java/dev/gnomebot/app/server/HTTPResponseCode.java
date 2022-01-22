package dev.gnomebot.app.server;

import dev.gnomebot.app.server.handler.HTTPCodeException;

/**
 * @author LatvianModder
 */
public enum HTTPResponseCode {
	UNKNOWN(0, "unknown_error", "Unknown Error"),
	OK(200, "ok", "OK"),
	NO_CONTENT(204, "no_content", "No Content"),
	MOVED_PERMANENTLY(301, "moved_permanently", "Moved Permanently"),
	MOVED_TEMPORARILY(302, "moved_temporarily", "Moved Temporarily"),
	UNAUTHORIZED(401, "unauthorized", "Unauthorized"),
	BAD_REQUEST(400, "bad_request", "Bad Request"),
	FORBIDDEN(403, "forbidden", "Forbidden"),
	NOT_FOUND(404, "not_found", "Not Found"),
	CONFLICT(409, "conflict", "Conflict"),
	INTERNAL_ERROR(500, "internal_error", "Internal Error");

	public static final HTTPResponseCode[] VALUES = values();

	public static HTTPResponseCode get(int code) {
		for (HTTPResponseCode responseCode : VALUES) {
			if (responseCode.code == code) {
				return responseCode;
			}
		}

		return UNKNOWN;
	}

	public final int code;
	public final String message;
	public final String prettyMessage;

	HTTPResponseCode(int c, String s, String pm) {
		code = c;
		message = s;
		prettyMessage = pm;
	}

	public boolean isOK() {
		return code >= 200 && code < 300;
	}

	public HTTPCodeException error(String msg) {
		return new HTTPCodeException(this, msg);
	}
}