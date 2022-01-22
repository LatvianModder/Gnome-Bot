package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.server.HTTPResponseCode;
import io.javalin.http.Context;

public class ResponseWithCookie implements Response {
	public final Response original;
	public final String key;
	public final String value;
	public final int maxAge;

	ResponseWithCookie(Response r, String k, String v, int a) {
		original = r;
		key = k;
		value = v;
		maxAge = a;
	}

	@Override
	public HTTPResponseCode getCode() {
		return original.getCode();
	}

	@Override
	public void result(Context ctx) throws Exception {
		original.result(ctx);
		ctx.cookie(key, value, maxAge);
	}
}
