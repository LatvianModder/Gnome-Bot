package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.json.JSONArray;
import dev.latvian.apps.webutils.json.JSONObject;
import dev.latvian.apps.webutils.json.JSONResponse;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import discord4j.common.util.Snowflake;
import discord4j.core.util.ImageUtil;
import discord4j.discordjson.json.UserData;
import discord4j.rest.util.Image;

import java.awt.image.BufferedImage;
import java.util.HashSet;

import static discord4j.rest.util.Image.Format.GIF;
import static discord4j.rest.util.Image.Format.PNG;

/**
 * @author LatvianModder
 */
public class InfoHandlers {
	public static final int[] VALID_SIZES = {16, 32, 64, 128, 256, 512, 1024, 2048, 4096};

	public static Response ping(ServerRequest request) {
		return JSONResponse.SUCCESS;
	}

	public static Response user(ServerRequest request) {
		var size = request.query("size").asString("128");
		var id = Snowflake.of(request.variable("user"));

		// App.info("Getting info for " + id.asString());

		var json = new JSONObject();
		json.put("id", id.asString());
		UserData userData = request.app.discordHandler.getUserData(id);

		if (userData != null) {
			json.put("name", userData.username());
			json.put("discriminator", userData.discriminator());
			json.put("avatar_url", App.url("api/info/avatar/" + id.asString() + "/" + size));
			json.put("bot", userData.bot().toOptional().orElse(false));
		} else {
			json.put("name", "Deleted User");
			json.put("discriminator", "0000");
			json.put("avatar_url", App.url("api/info/avatar/" + id.asString() + "/" + size));
			json.put("bot", false);
		}

		return JSONResponse.of(json);
	}

	public static Response avatar(ServerRequest request) throws Exception {
		int size = Integer.parseInt(request.variable("size"));

		if (size <= 0 || size > 4096) {
			throw HTTPResponseCode.BAD_REQUEST.error("size_too_large");
		}

		Snowflake id = Snowflake.of(request.variable("user"));
		String url;

		int sizeToRetrieve = 4096;

		for (int validSize : VALID_SIZES) {
			if (validSize >= size && validSize < sizeToRetrieve) {
				sizeToRetrieve = validSize;
			}
		}

		UserData userData = request.app.discordHandler.getUserData(id);
		String avatar = userData == null ? null : userData.avatar().orElse(null);

		if (avatar != null && avatar.startsWith("a_")) {
			url = ImageUtil.getUrl("avatars/" + id.asString() + "/" + avatar, GIF) + "?size=" + sizeToRetrieve;
		} else if (avatar != null) {
			url = ImageUtil.getUrl("avatars/" + id.asString() + "/" + avatar, PNG) + "?size=" + sizeToRetrieve;
		} else {
			url = ImageUtil.getUrl("embed/avatars/" + (userData == null ? 0 : (Integer.parseInt(userData.discriminator()) % 5)), PNG) + "?size=" + sizeToRetrieve;
		}

		BufferedImage img;

		try {
			img = Utils.resize(URLRequest.of(url).toImage().block(), size, size);
		} catch (Exception ex) {
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
					img.setRGB(x, y, 0xFF000000);
				}
			}
		}

		return FileResponse.image(img);
	}

	public static Response emoji(ServerRequest request) throws Exception {
		int size = Integer.parseInt(request.variable("size"));

		if (size <= 0 || size > 4096) {
			throw HTTPResponseCode.BAD_REQUEST.error("size_too_large");
		}

		Snowflake id = Snowflake.of(request.variable("emoji"));
		String url = ImageUtil.getUrl("emojis/" + id.asString(), Image.Format.PNG);

		int sizeToRetrieve = 4096;

		for (int validSize : VALID_SIZES) {
			if (validSize >= size && validSize < sizeToRetrieve) {
				sizeToRetrieve = validSize;
			}
		}

		BufferedImage img;

		try {
			img = Utils.resize(URLRequest.of(url).toImage().block(), size, size);
		} catch (Exception ex) {
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
					img.setRGB(x, y, 0xFF000000);
				}
			}
		}

		return FileResponse.image(img);
	}

	public static Response define(ServerRequest request) throws Exception {
		String word = request.variable("word");

		var json = new JSONObject();
		json.put("found", false);
		json.put("word", word);

		try {
			var data0 = URLRequest.of("https://api.dictionaryapi.dev/api/v2/entries/en/" + word).toJsonArray().blockEither();

			if (data0.isRight() && data0.right().getMessage().startsWith("Error 404")) {
				return JSONResponse.of(json);
			}

			var firstWord = data0.left().object(0).string("word");
			json.put("word", firstWord);
			var phonetics = new JSONArray();
			var meanings = new JSONArray();
			var phoneticsSet = new HashSet<String>();

			for (var data1 : data0.left()) {
				var data = (JSONObject) data1;

				if (data.string("word").equals(firstWord)) {
					for (var e : data.array("phonetics")) {
						var o0 = (JSONObject) e;
						String text = o0.string("text").trim();

						if (!phoneticsSet.contains(text)) {
							phoneticsSet.add(text);
							var o = new JSONObject();
							o.put("text", text);
							o.put("audio_url", o0.containsKey("audio") ? ("https:" + o0.string("audio").trim()) : "");
							phonetics.add(o);
						}
					}

					for (var e : data.array("meanings")) {
						var o0 = (JSONObject) e;
						String type = o0.string("partOfSpeech").trim();

						for (var e1 : o0.array("definitions")) {
							var o1 = (JSONObject) e1;
							var o = new JSONObject();
							o.put("type", type);
							o.put("definition", o1.string("definition").trim());
							o.put("example", o1.containsKey("example") ? o1.string("example").trim() : "");
							o.put("synonyms", o1.containsKey("synonyms") ? o1.get("synonyms") : new JSONArray());
							o.put("antonyms", o1.containsKey("antonyms") ? o1.get("antonyms") : new JSONArray());
							meanings.add(o);
						}
					}
				}
			}

			json.put("phonetics", phonetics);
			json.put("meanings", meanings);
			json.put("found", true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return JSONResponse.of(json);
	}
}