package dev.gnomebot.app.server.handler.panel;

import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.net.Response;

/**
 * @author LatvianModder
 */
public class ScamWebHandlers {
	public static Response scams(ServerRequest request) {
		var content = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Scams");
		content.p().string("Uh... nothing for now...");
		return content.asResponse();
	}
}