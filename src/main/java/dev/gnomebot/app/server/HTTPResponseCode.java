package dev.gnomebot.app.server;

import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;

import java.nio.charset.StandardCharsets;

public enum HTTPResponseCode {
	UNKNOWN(HttpStatus.UNKNOWN),
	OK(HttpStatus.OK),
	NO_CONTENT(HttpStatus.NO_CONTENT),
	MOVED_PERMANENTLY(HttpStatus.MOVED_PERMANENTLY),
	FOUND(HttpStatus.FOUND),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
	BAD_REQUEST(HttpStatus.BAD_REQUEST),
	FORBIDDEN(HttpStatus.FORBIDDEN),
	NOT_FOUND(HttpStatus.NOT_FOUND),
	CONFLICT(HttpStatus.CONFLICT),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

	public static final HTTPResponseCode[] VALUES = values();

	public static HTTPResponseCode get(int code) {
		for (HTTPResponseCode responseCode : VALUES) {
			if (responseCode.status.getCode() == code) {
				return responseCode;
			}
		}

		return UNKNOWN;
	}

	public final HttpStatus status;
	public final Response response;

	HTTPResponseCode(HttpStatus status) {
		this.status = status;
		this.response = FileResponse.of(status, "text/plain; charset=utf-8", status.getMessage().getBytes(StandardCharsets.UTF_8));
	}

	public boolean isOK() {
		return status.getCode() / 100 == 2;
	}

	public HttpResponseException error(String msg) {
		return new HttpResponseException(status.getCode(), msg);
	}

	public Response response(String customMessage) {
		return FileResponse.of(status, "text/plain; charset=utf-8", customMessage.getBytes(StandardCharsets.UTF_8));
	}
}