package dev.gnomebot.app.server;

import dev.gnomebot.app.server.handler.Response;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface ServerPathHandler {
	Response handle(ServerRequest data) throws Exception;
}