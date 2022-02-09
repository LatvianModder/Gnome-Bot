package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author LatvianModder
 */
public class MiscHandlers {
	public static Response assets(ServerRequest request) throws Exception {
		String filename = request.variable("filename");

		try {
			Assets.Asset a = Assets.MAP.get(filename);

			if (a != null) {
				Path path = AppPaths.ASSETS.resolve(a.filename);

				if (Files.exists(path)) {
					return FileResponse.of(HTTPResponseCode.OK, a.contentType, Files.readAllBytes(path));
				} else {
					App.warn("Asset " + path.toAbsolutePath() + " doesn't exist!");
				}
			}
		} catch (Exception ex) {
		}

		throw HTTPResponseCode.NOT_FOUND.error("File not found!");
	}

	public static Response signIn(ServerRequest request) throws Exception {
		throw HTTPResponseCode.BAD_REQUEST.error("Use bot command '/panel login' instead!");
	}

	public static Response signOut(ServerRequest request) {
		App.instance.db.invalidateToken(request.token.userId.asLong());
		return Response.SUCCESS_JSON;
	}
}