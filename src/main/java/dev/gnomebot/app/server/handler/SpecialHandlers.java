package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import io.javalin.http.HttpStatus;

import java.nio.file.Files;

public class SpecialHandlers {
	public static Response publicfile(ServerRequest request) throws Exception {
		var filename = request.variable("file");

		if (filename.length() < 3 || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid file name!");
		}

		var path = AppPaths.PUBLIC_DATA.resolve(filename);

		if (Files.exists(path)) {
			var type = request.header("Content-Type", "text/plain");
			var data = Files.readAllBytes(path);
			return FileResponse.of(HttpStatus.OK, type, data);
		}

		throw HTTPResponseCode.NOT_FOUND.error("File not found!");
	}
}
