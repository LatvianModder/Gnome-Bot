package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.server.AppRequest;
import dev.gnomebot.app.server.AuthLevel;
import dev.latvian.apps.tinyserver.http.response.CookieResponse;
import dev.latvian.apps.tinyserver.http.response.HTTPResponse;
import dev.latvian.apps.tinyserver.http.response.error.client.BadRequestError;
import dev.latvian.apps.tinyserver.http.response.error.client.NotFoundError;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;

public class RootHandlers {
	public static HTTPResponse root(AppRequest req) {
		var root = req.createRoot("Homepage");

		if (!req.authLevel().is(AuthLevel.LOGGED_IN)) {
			root.content.p().string("You are not logged in! Run ").code().string("/gnome panel login").end().string(" command on discord to continue.");
			return root.asResponse();
		}

		var ul = root.content.ul();
		ul.li().a("/guild").string("Browse Guilds");
		ul.li().a("/logout").string("Log Out");
		return root.asResponse();
	}

	public static HTTPResponse login(AppRequest req) {
		var token = req.query("logintoken").asString();

		if (token.isEmpty()) {
			var root = req.createRoot("Logged In");

			if (req.token == null) {
				root.content.p().string("What the heck? You shouldn't be here, shoo!");
			} else {
				root.content.p().string("You've successfully logged in, " + req.token.getName() + "!");
				root.content.p().string("You can now close this page.");
				root.content.p().a("/guild").string("You can click here to browse guild list.");
			}

			return root.asResponse();
		}

		return HTTPResponse.redirect("/login").cookie(AppRequest.COOKIE_TOKEN, new String(Base64.getUrlDecoder().decode(token.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8), CookieResponse.Builder::maxAgeYear);
	}

	public static HTTPResponse logout(AppRequest req) {
		req.checkLoggedIn();
		App.instance.db.invalidateTokens(req.token.userId);
		return HTTPResponse.redirect("/");
	}

	public static HTTPResponse publicfile(AppRequest req) {
		req.log = false;

		var filename = req.variable("file").asString();

		if (filename.length() < 3 || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
			throw new BadRequestError("Invalid file name!");
		}

		var path = AppPaths.PUBLIC_DATA.resolve(filename);

		if (Files.exists(path)) {
			return HTTPResponse.ok().content(path, req.header("Content-Type").asString()).publicCache(Duration.ofMinutes(5L));
		}

		throw new NotFoundError("File not found!");
	}

	public static HTTPResponse robots(AppRequest req) {
		return HTTPResponse.ok().text("""
				User-agent: *
				Disallow: /paste/*
				Disallow: /guild/*
				Allow: /""").publicCache(Duration.ofDays(1L));
	}
}