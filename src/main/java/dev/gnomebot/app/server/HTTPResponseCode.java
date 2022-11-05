package dev.gnomebot.app.server;

import dev.gnomebot.app.server.handler.FileResponse;
import dev.gnomebot.app.server.handler.HTTPCodeException;
import dev.gnomebot.app.server.handler.Response;

import java.nio.charset.StandardCharsets;

/**
 * @author LatvianModder
 */
public enum HTTPResponseCode {
	UNKNOWN(0, "Unknown Error"),
	OK(200, "OK"),
	NO_CONTENT(204, "No Content"),
	MOVED_PERMANENTLY(301, "Moved Permanently"),
	MOVED_TEMPORARILY(302, "Moved Temporarily"),
	UNAUTHORIZED(401, "Unauthorized"),
	BAD_REQUEST(400, "Bad Request"),
	FORBIDDEN(403, "Forbidden"),
	NOT_FOUND(404, "Not Found"),
	CONFLICT(409, "Conflict"),
	INTERNAL_ERROR(500, "Internal Error");

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
	public final Response response;

	HTTPResponseCode(int c, String pm) {
		code = c;
		message = pm;
		response = FileResponse.of(this, "text/plain; charset=utf-8", message.getBytes(StandardCharsets.UTF_8));
	}

	public boolean isOK() {
		return code >= 200 && code < 300;
	}

	public HTTPCodeException error(String msg) {
		return new HTTPCodeException(this, msg);
	}

	public Response response(String customMessage) {
		return FileResponse.of(this, "text/plain; charset=utf-8", customMessage.getBytes(StandardCharsets.UTF_8));
	}
}