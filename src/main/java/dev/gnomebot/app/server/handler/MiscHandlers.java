package dev.gnomebot.app.server.handler;

import com.google.gson.JsonObject;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
					return FileResponse.of(a.contentType, Files.readAllBytes(path));
				} else {
					App.warn("Asset " + path.toAbsolutePath() + " doesn't exist!");
				}
			}
		} catch (Exception ex) {
		}

		throw HTTPResponseCode.NOT_FOUND.error("File not found!");
	}

	public static Response signIn(ServerRequest request) throws Exception {
		String code = request.query("code").asString();

		if (code.isEmpty()) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid Code");
		}

		Map<String, String> map = new HashMap<>();
		map.put("client_id", request.app.config.discord_client_id);
		map.put("client_secret", request.app.config.discord_client_secret);
		map.put("grant_type", "authorization_code");
		map.put("code", code);
		map.put("redirect_uri", "https://gnomebot.dev/api/account/sign-in");
		map.put("scope", "identify email");

		JsonObject codeResponse = URLRequest.of("https://discordapp.com/api/oauth2/token")
				.toJsonObject()
				.contentType("application/x-www-form-urlencoded")
				.outForm(map)
				.block();

		String token = codeResponse.get("access_token").getAsString();

		if (token.isEmpty()) {
			throw HTTPResponseCode.BAD_REQUEST.error("Empty Token");
		}

		JsonObject userResponse = URLRequest.of("https://discordapp.com/api/users/@me")
				.addHeader("Authorization", "Bearer " + token)
				.toJsonObject()
				.block();

		Snowflake discordUserId = Snowflake.of(userResponse.get("id").getAsString());
		String userName = userResponse.get("username").getAsString();
		String disc = userResponse.get("discriminator").getAsString();

		String loginToken = Utils.createToken();
		Document tokenDoc = new Document();
		tokenDoc.put("_id", loginToken);
		tokenDoc.put("created", new Date());
		tokenDoc.put("user", discordUserId.asLong());
		tokenDoc.put("name", userName + "#" + disc);
		tokenDoc.put("user_agent", request.header("User-Agent", ""));
		App.instance.db.webTokens.insert(tokenDoc);

		JsonObject json = new JsonObject();
		//json.addProperty("token", loginToken);
		json.addProperty("id", discordUserId.asString());
		json.addProperty("name", userName);
		json.addProperty("disc", disc);
		String base64 = Base64.getUrlEncoder().encodeToString(Utils.GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
		return Redirect.temporarily("https://gnomebot.dev/sign-in/" + base64).withCookie("gnometoken", loginToken, 2592000);
	}

	public static Response signOut(ServerRequest request) {
		App.instance.db.webTokens.query(request.token.document.getString("_id")).delete();
		return Response.SUCCESS_JSON;
	}
}