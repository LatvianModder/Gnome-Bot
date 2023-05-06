package dev.gnomebot.app.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gnomebot.app.App;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.gson.JsonResponse;
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
		return JsonResponse.SUCCESS;
	}

	public static Response user(ServerRequest request) {
		var size = request.query("size").asString("128");
		var id = Snowflake.of(request.variable("user"));

		// App.info("Getting info for " + id.asString());

		return JsonResponse.object(json -> {
			json.addProperty("id", id.asString());
			UserData userData = request.app.discordHandler.getUserData(id);

			if (userData != null) {
				json.addProperty("name", userData.username());
				json.addProperty("discriminator", userData.discriminator());
				json.addProperty("avatar_url", App.url("api/info/avatar/" + id.asString() + "/" + size));
				json.addProperty("bot", userData.bot().toOptional().orElse(false));
			} else {
				json.addProperty("name", "Deleted User");
				json.addProperty("discriminator", "0000");
				json.addProperty("avatar_url", App.url("api/info/avatar/" + id.asString() + "/" + size));
				json.addProperty("bot", false);
			}
		});
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

		return JsonResponse.object(json -> {
			json.addProperty("found", false);
			json.addProperty("word", word);

			try {
				var data0 = URLRequest.of("https://api.dictionaryapi.dev/api/v2/entries/en/" + word).toJson().blockEither();

				if (data0.isRight() && data0.right().getMessage().startsWith("Error 404") || !data0.left().isJsonArray()) {
					return;
				}

				var firstWord = data0.left().getAsJsonArray().get(0).getAsJsonObject().get("word").getAsString();
				json.addProperty("word", firstWord);
				var phonetics = new JsonArray();
				var meanings = new JsonArray();
				var phoneticsSet = new HashSet<String>();

				for (JsonElement data1 : data0.left().getAsJsonArray()) {
					JsonObject data = data1.getAsJsonObject();

					if (data.get("word").getAsString().equals(firstWord)) {
						for (JsonElement e : data.get("phonetics").getAsJsonArray()) {
							JsonObject o0 = e.getAsJsonObject();
							String text = o0.get("text").getAsString().trim();

							if (!phoneticsSet.contains(text)) {
								phoneticsSet.add(text);
								JsonObject o = new JsonObject();
								o.addProperty("text", text);
								o.addProperty("audio_url", o0.has("audio") ? ("https:" + o0.get("audio").getAsString().trim()) : "");
								phonetics.add(o);
							}
						}

						for (JsonElement e : data.get("meanings").getAsJsonArray()) {
							JsonObject o0 = e.getAsJsonObject();
							String type = o0.get("partOfSpeech").getAsString().trim();

							for (JsonElement e1 : o0.get("definitions").getAsJsonArray()) {
								JsonObject o1 = e1.getAsJsonObject();
								JsonObject o = new JsonObject();
								o.addProperty("type", type);
								o.addProperty("definition", o1.get("definition").getAsString().trim());
								o.addProperty("example", o1.has("example") ? o1.get("example").getAsString().trim() : "");
								o.add("synonyms", o1.has("synonyms") ? o1.get("synonyms") : new JsonArray());
								o.add("antonyms", o1.has("antonyms") ? o1.get("antonyms") : new JsonArray());
								meanings.add(o);
							}
						}
					}
				}

				json.add("phonetics", phonetics);
				json.add("meanings", meanings);
				json.addProperty("found", true);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}
}