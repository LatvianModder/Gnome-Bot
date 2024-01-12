package dev.gnomebot.app.server.handler.panel;

import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.net.Response;

public class ScamWebHandlers {
	public static Response scamDetection(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Scam Detection - " + request.gc);
		root.content.a("/panel/" + request.gc.guildId, "< Back").classes("back");
		root.content.p().string("Uh... nothing for now...");
		return root.asResponse();
	}
}