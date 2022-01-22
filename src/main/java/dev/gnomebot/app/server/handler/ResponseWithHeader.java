package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.server.HTTPResponseCode;
import io.javalin.http.Context;

public class ResponseWithHeader implements Response {
	public final Response original;
	public final String key;
	public final String value;

	ResponseWithHeader(Response r, String k, String v) {
		original = r;
		key = k;
		value = v;
	}

	@Override
	public HTTPResponseCode getCode() {
		return original.getCode();
	}

	@Override
	public void result(Context ctx) throws Exception {
		original.result(ctx);
		ctx.header(key, value);
	}
}
