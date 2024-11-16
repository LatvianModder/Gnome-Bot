package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.latvian.apps.tinyserver.http.HTTPRequest;

public class AppRequest extends HTTPRequest {
	public final App app;

	public AppRequest(App app) {
		this.app = app;
	}
}
