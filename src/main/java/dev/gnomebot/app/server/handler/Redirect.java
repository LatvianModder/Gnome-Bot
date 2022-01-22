package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.server.HTTPResponseCode;
import io.javalin.http.Context;

public class Redirect implements Response {
	public static Redirect temporarily(String location) {
		return new Redirect(HTTPResponseCode.MOVED_TEMPORARILY.code, location);
	}

	public static Redirect permanently(String location) {
		return new Redirect(HTTPResponseCode.MOVED_PERMANENTLY.code, location);
	}

	public final int code;
	public final String location;

	private Redirect(int c, String l) {
		code = c;
		location = l;
	}

	@Override
	public void result(Context ctx) throws Exception {
		ctx.status(code);
		ctx.header("Location", location);
	}
}
