package dev.gnomebot.app.server.handler.panel;

import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.handler.Response;
import dev.gnomebot.app.server.html.RootTag;
import dev.gnomebot.app.server.html.Tag;

/**
 * @author LatvianModder
 */
public class ScamWebHandlers {
	public static Response scams(ServerRequest request) {
		Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Scams");
		content.p().string("Uh... nothing for now...");
		return content.asResponse();
	}
}