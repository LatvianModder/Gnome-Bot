package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.gson.JsonResponse;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import io.javalin.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

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
					return FileResponse.of(HttpStatus.OK, a.contentType, Files.readAllBytes(path));
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
		return JsonResponse.SUCCESS;
	}

	// To be moved somewhere else later lol
	public static Response rustPlus(ServerRequest request) {
		/*
		Type:killed
		ServerName:{{ServerName}}
		ServerLogoUrl:{{ServerLogoUrl}}
		PlayerSteamId:{{PlayerSteamId}}
		PlayerName:{{PlayerName}}
		 */

		// Config.get().rust_plus_webhook

		var map = new HashMap<String, String>();

		for (var s : request.getMainBody().getText().split("\n")) {
			var s1 = s.split(":", 2);
			map.put(s1[0], s1[1]);
		}

		App.info(map);

		var from = request.variable("from");
		var user = "[" + map.get("PlayerName") + "](<https://steamcommunity.com/profiles/" + map.get("PlayerSteamId") + ">)";

		var message = switch (map.get("Type")) {
			case "killed" -> from + " was killed by " + user;
			case "online" -> user + " is now online";
			case "smart_alarm" -> "Smart alarm '" + map.get("Title") + "' triggered: " + map.get("Body");
			default -> "unknown event " + map.get("Type") + " from " + user + ": " + map;
		};

		Config.get().rust_plus_webhook.execute(MessageBuilder.create(message)
				.webhookName(map.get("ServerName"))
				.webhookAvatarUrl(map.get("ServerLogoUrl"))
		);

		return Response.NO_CONTENT;
	}
}