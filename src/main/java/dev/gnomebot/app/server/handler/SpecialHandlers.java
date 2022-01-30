package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author LatvianModder
 */
public class SpecialHandlers {
	public static Response publicfile(ServerRequest request) throws Exception {
		String filename = request.variable("file");

		if (filename.length() < 3 || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid file name!");
		}

		Path path = AppPaths.DATA_PUBLIC.resolve(filename);

		if (Files.exists(path)) {
			String type = request.header("Content-Type", "text/plain");
			byte[] data = Files.readAllBytes(path);
			return FileResponse.of(type, data);
		}

		throw HTTPResponseCode.NOT_FOUND.error("File not found!");
	}
}
