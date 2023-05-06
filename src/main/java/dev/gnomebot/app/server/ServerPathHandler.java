package dev.gnomebot.app.server;

import dev.latvian.apps.webutils.net.Response;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface ServerPathHandler {
	Response handle(ServerRequest data) throws Exception;
}