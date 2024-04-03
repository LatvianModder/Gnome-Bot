package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import io.javalin.http.HttpStatus;

import java.nio.file.Files;

public class RootHandlers {
	public static Response root(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Bot");

		if (!request.getAuthLevel().is(AuthLevel.LOGGED_IN)) {
			root.content.p().string("You are not logged in! Run ").code().string("/gnome panel login").end().string(" command on discord to continue.");
			return root.asResponse();
		}

		var ul = root.content.ul();
		ul.li().a("/guild").string("Browse Guilds");
		ul.li().a("/logout").string("Log Out");
		return root.asResponse();
	}

	public static Response login(ServerRequest request) {
		if (!request.query("logintoken").isPresent()) {
			var root = GnomeRootTag.createSimple(request.getPath(), "Logged In");

			if (request.token == null) {
				root.content.p().string("What the heck? You shouldn't be here, shoo!");
			} else {
				root.content.p().string("You've successfully logged in, " + request.token.getName() + "!");
				root.content.p().string("You can now close this page.");
				root.content.p().a("/panel").string("You can click here to browse guild list.");
			}

			return root.asResponse();
		}

		return Response.redirect(App.url("panel/login"));
	}

	public static Response logout(ServerRequest request) {
		App.instance.db.invalidateToken(request.token.userId);
		return Response.redirect("/");
	}

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

	public static Response robots(ServerRequest request) {
		return FileResponse.plainText("""
				User-agent: *
				Disallow: /paste/*
				Disallow: /guild/*
				Allow: /""");
	}
}