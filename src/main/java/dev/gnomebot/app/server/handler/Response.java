package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.json.JsonResponse;
import io.javalin.http.Context;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface Response {
	Response SUCCESS_JSON = JsonResponse.object(json -> json.addProperty("success", true));

	Response NO_CONTENT = new Response() {
		@Override
		public HTTPResponseCode getCode() {
			return HTTPResponseCode.NO_CONTENT;
		}

		@Override
		public void result(Context ctx) {
			ctx.status(204);
		}
	};

	default HTTPResponseCode getCode() {
		return HTTPResponseCode.OK;
	}

	default ResponseWithCookie withCookie(String key, String value, int maxAge) {
		return new ResponseWithCookie(this, key, value, maxAge);
	}

	default ResponseWithHeader withHeader(String key, String value) {
		return new ResponseWithHeader(this, key, value);
	}

	void result(Context ctx) throws Exception;
}