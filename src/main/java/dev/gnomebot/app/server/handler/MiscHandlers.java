package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.json.JSONResponse;
import dev.latvian.apps.webutils.net.Response;

import java.util.HashMap;

public class MiscHandlers {
	public static Response signOut(ServerRequest request) {
		App.instance.db.invalidateToken(request.token.userId.asLong());
		return JSONResponse.SUCCESS;
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