package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.Assets;
import dev.gnomebot.app.server.AppRequest;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.json.JSONArray;
import dev.latvian.apps.json.JSONObject;
import dev.latvian.apps.json.JSONResponse;
import dev.latvian.apps.tinyserver.http.response.HTTPResponse;
import dev.latvian.apps.tinyserver.http.response.error.client.BadRequestError;
import dev.latvian.apps.tinyserver.http.response.error.client.NotFoundError;
import dev.latvian.apps.webutils.ImageUtils;
import discord4j.core.util.ImageUtil;
import discord4j.rest.util.Image;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashSet;

import static discord4j.rest.util.Image.Format.GIF;
import static discord4j.rest.util.Image.Format.PNG;

public class InfoHandlers {
	public static final int[] VALID_SIZES = {16, 32, 64, 128, 256, 512, 1024, 2048, 4096};

	public static HTTPResponse ping(AppRequest req) {
		return HTTPResponse.ok();
	}

	public static HTTPResponse user(AppRequest req) {
		var size = req.query("size").asInt(128);
		var id = req.getSnowflake("user");

		// App.info("Getting info for " + id.asString());

		var json = JSONObject.of();
		json.put("id", SnowFlake.str(id));
		var userData = req.app.discordHandler.getUserData(id);

		if (userData != null) {
			json.put("name", userData.username());
			json.put("discriminator", userData.discriminator());
			json.put("avatar_url", req.app.url("api/info/avatar/" + id + "/" + size));
			json.put("bot", userData.bot().toOptional().orElse(false));
		} else {
			json.put("name", "Deleted User");
			json.put("discriminator", "0000");
			json.put("avatar_url", req.app.url("api/info/avatar/" + id + "/" + size));
			json.put("bot", false);
		}

		return JSONResponse.of(json).publicCache(Duration.ofHours(1L));
	}

	public static HTTPResponse avatar(AppRequest req) throws Exception {
		var size = req.variable("size").asInt();

		if (size <= 0 || size > 4096) {
			throw new BadRequestError("Size too large");
		}

		var id = req.getSnowflake("user");
		String url;

		var sizeToRetrieve = 4096;

		for (var validSize : VALID_SIZES) {
			if (validSize >= size && validSize < sizeToRetrieve) {
				sizeToRetrieve = validSize;
			}
		}

		var userData = req.app.discordHandler.getUserData(id);
		var avatar = userData == null ? null : userData.avatar().orElse(null);

		if (avatar != null && avatar.startsWith("a_")) {
			url = ImageUtil.getUrl("avatars/" + id + "/" + avatar, GIF) + "?size=" + sizeToRetrieve;
		} else if (avatar != null) {
			url = ImageUtil.getUrl("avatars/" + id + "/" + avatar, PNG) + "?size=" + sizeToRetrieve;
		} else {
			url = ImageUtil.getUrl("embed/avatars/" + (userData == null ? 0 : (Integer.parseInt(userData.discriminator()) % 5)), PNG) + "?size=" + sizeToRetrieve;
		}

		BufferedImage img;

		try {
			img = ImageUtils.resize(URLRequest.of(url).toImage().block(), size, size);
		} catch (Exception ex) {
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

			for (var x = 0; x < size; x++) {
				for (var y = 0; y < size; y++) {
					img.setRGB(x, y, 0xFF000000);
				}
			}
		}

		return HTTPResponse.ok().png(img).publicCache(Duration.ofDays(1L));
	}

	public static HTTPResponse emoji(AppRequest req) throws Exception {
		var size = req.variable("size").asInt();

		if (size <= 0 || size > 4096) {
			throw new BadRequestError("Size too large");
		}

		var id = req.getSnowflake("emoji");
		var url = ImageUtil.getUrl("emojis/" + id, Image.Format.PNG);

		var sizeToRetrieve = 4096;

		for (var validSize : VALID_SIZES) {
			if (validSize >= size && validSize < sizeToRetrieve) {
				sizeToRetrieve = validSize;
			}
		}

		BufferedImage img;

		try {
			img = ImageUtils.resize(URLRequest.of(url).toImage().block(), size, size);
		} catch (Exception ex) {
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

			for (var x = 0; x < size; x++) {
				for (var y = 0; y < size; y++) {
					img.setRGB(x, y, 0xFF000000);
				}
			}
		}

		return HTTPResponse.ok().png(img).publicCache(Duration.ofDays(1L));
	}

	public static HTTPResponse define(AppRequest req) throws Exception {
		var word = req.variable("word");

		var json = JSONObject.of();
		json.put("found", false);
		json.put("word", word);

		try {
			var data0 = URLRequest.of("https://api.dictionaryapi.dev/api/v2/entries/en/" + word).toJsonArray().blockEither();

			if (data0.isRight() && data0.right().getMessage().startsWith("Error 404")) {
				return JSONResponse.of(json);
			}

			var firstWord = data0.left().asObject(0).asString("word");
			json.put("word", firstWord);
			var phonetics = json.addArray("phonetics");
			var meanings = json.addArray("meanings");
			var phoneticsSet = new HashSet<String>();

			for (var data1 : data0.left()) {
				var data = (JSONObject) data1;

				if (data.asString("word").equals(firstWord)) {
					for (var o0 : data.asArray("phonetics").ofObjects()) {
						var text = o0.asString("text").trim();

						if (!phoneticsSet.contains(text)) {
							phoneticsSet.add(text);
							var o = phonetics.addObject();
							o.put("text", text);
							o.put("audio_url", o0.containsKey("audio") ? ("https:" + o0.asString("audio").trim()) : "");
						}
					}

					for (var o0 : data.asArray("meanings").ofObjects()) {
						var type = o0.asString("partOfSpeech").trim();

						for (var o1 : o0.asArray("definitions").ofObjects()) {
							var o = meanings.addObject();
							o.put("type", type);
							o.put("definition", o1.asString("definition").trim());
							o.put("example", o1.asString("example").trim());
							o.put("synonyms", o1.containsKey("synonyms") ? o1.get("synonyms") : JSONArray.of());
							o.put("antonyms", o1.containsKey("antonyms") ? o1.get("antonyms") : JSONArray.of());
						}
					}
				}
			}

			json.put("found", true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return JSONResponse.of(json).publicCache(Duration.ofMinutes(1L));
	}

	public static HTTPResponse videoThumbnail(AppRequest req) throws Exception {
		var message = req.app.discordHandler.client.getMessageById(SnowFlake.convert(req.getSnowflake("channel")), SnowFlake.convert(req.getSnowflake("message"))).block();
		var attachmentId = req.getSnowflake("attachment");

		for (var a : message.getAttachments()) {
			if (a.getId().asLong() == attachmentId) {
				var img = URLRequest.of(a.getProxyUrl() + "?format=jpeg").toImage().block();
				var img1 = ImageIO.read(new ByteArrayInputStream(Files.readAllBytes(Assets.VIDEO.getFilePath())));
				var g = img.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				var tsize = Math.min(img.getWidth(), img.getHeight()) / 4;
				// draw img1 at center of img, scaled 0.25x the height of img
				g.drawImage(img1, (img.getWidth() - tsize) / 2, (img.getHeight() - tsize) / 2, tsize, tsize, null);
				g.dispose();

				return HTTPResponse.ok().jpeg(img).publicCache(Duration.ofDays(1L));
			}
		}

		throw new NotFoundError("Attachment not found");
	}
}