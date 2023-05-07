package dev.gnomebot.app.server.json;

import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerPathHandler;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.json.JSON;
import dev.latvian.apps.webutils.net.Response;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface JsonServerPathHandler extends ServerPathHandler {
	Response handleJson(JsonRequest request) throws Exception;

	@Override
	default Response handle(ServerRequest request) throws Exception {
		String body = request.getMainBody().getText();

		try {
			var in = body.isEmpty() ? null : JSON.DEFAULT.read(body).readObject();
			return handleJson(new JsonRequest(request, in));
		} catch (MissingValueException | WrongJsonTypeException ex) {
			throw HTTPResponseCode.BAD_REQUEST.error(ex.toString());
		}
	}
}