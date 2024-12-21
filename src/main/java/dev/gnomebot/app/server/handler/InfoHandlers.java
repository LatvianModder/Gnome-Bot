package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.server.AppRequest;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.json.JSONObject;
import dev.latvian.apps.json.JSONResponse;
import dev.latvian.apps.tinyserver.http.response.HTTPResponse;
import dev.latvian.apps.tinyserver.http.response.error.client.BadRequestError;
import dev.latvian.apps.tinyserver.http.response.error.client.NotFoundError;
import dev.latvian.apps.tinyserver.http.response.error.client.UnauthorizedError;
import dev.latvian.apps.webutils.ImageUtils;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.util.ImageUtil;
import discord4j.rest.util.Image;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.function.Function;

import static discord4j.rest.util.Image.Format.PNG;

public class InfoHandlers {
	public static final int[] VALID_SIZES = {16, 32, 64, 128, 256, 512, 1024, 2048, 4096};
	public static final Duration IMAGE_CACHE_TIME = Duration.ofDays(3L);

	public static int getClosestValidSize(int requestSize) {
		var size = 4096;

		for (var validSize : VALID_SIZES) {
			if (validSize >= requestSize && validSize < size) {
				size = validSize;
			}
		}

		return size;
	}

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

	public static BufferedImage getCachedImage(String url, Path cachePath, int size) {
		BufferedImage img = null;

		try {
			if (Files.exists(cachePath) && Files.getLastModifiedTime(cachePath).toInstant().isAfter(Instant.now().minus(IMAGE_CACHE_TIME))) {
				try (var in = new BufferedInputStream(Files.newInputStream(cachePath))) {
					img = ImageIO.read(in);
				}
			} else {
				img = ImageUtils.resize(URLRequest.of(url).toImage().block(), size, size);

				try (var out = new BufferedOutputStream(Files.newOutputStream(cachePath))) {
					ImageIO.write(img, "png", out);
				}
			}
		} catch (Exception ignore) {
		}

		if (img == null) {
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

			for (var x = 0; x < size; x++) {
				for (var y = 0; y < size; y++) {
					img.setRGB(x, y, 0xFF000000);
				}
			}
		}

		return img;
	}

	public static BufferedImage getUserAvatarImage(App app, long id, int requestSize) {
		var size = getClosestValidSize(requestSize);
		var userData = app.discordHandler.getUserData(id);
		var avatar = userData == null ? null : userData.avatar().orElse(null);

		String url;
		Path cachePath;

		if (avatar != null && avatar.startsWith("a_")) {
			var u = SnowFlake.str(id);
			url = ImageUtil.getUrl("avatars/" + id + "/" + avatar.substring(2), PNG) + "?size=" + size;
			cachePath = AppPaths.makeDir(AppPaths.USER_AVATAR_CACHE.resolve(size + "/" + u.substring(0, 2))).resolve(u + ".png");
		} else if (avatar != null) {
			var u = SnowFlake.str(id);
			url = ImageUtil.getUrl("avatars/" + id + "/" + avatar, PNG) + "?size=" + size;
			cachePath = AppPaths.makeDir(AppPaths.USER_AVATAR_CACHE.resolve(size + "/" + u.substring(0, 2))).resolve(u + ".png");
		} else {
			int d = userData == null ? 0 : (Integer.parseInt(userData.discriminator()) % 5);
			url = ImageUtil.getUrl("embed/avatars/" + d, PNG) + "?size=" + size;
			cachePath = AppPaths.makeDir(AppPaths.USER_AVATAR_CACHE.resolve(size + "/default")).resolve(d + ".png");
		}

		return getCachedImage(url, cachePath, size);
	}

	public static HTTPResponse avatar(AppRequest req) throws Exception {
		var size = req.variable("size").asInt();

		if (size <= 0 || size > 4096) {
			throw new BadRequestError("Size too large");
		}

		var id = req.getSnowflake("user");
		return HTTPResponse.ok().png(getUserAvatarImage(req.app, id, size)).publicCache(IMAGE_CACHE_TIME);
	}

	public static BufferedImage getEmojiImage(long id, int requestSize) {
		var size = getClosestValidSize(requestSize);
		var url = ImageUtil.getUrl("emojis/" + id, Image.Format.PNG);
		var cachePath = AppPaths.makeDir(AppPaths.EMOJI_CACHE.resolve(Integer.toString(size))).resolve(id + ".png");

		return getCachedImage(url, cachePath, size);
	}

	public static HTTPResponse emoji(AppRequest req) throws Exception {
		var size = req.variable("size").asInt();

		if (size <= 0 || size > 4096) {
			throw new BadRequestError("Size too large");
		}

		var id = req.getSnowflake("emoji");
		return HTTPResponse.ok().png(getEmojiImage(id, size)).publicCache(IMAGE_CACHE_TIME);
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

	private static void appendUserData(JSONObject json, User user, boolean removed) {
		json.put("id", user.getId().asString());
		json.put("name", user.getUsername());
		json.put("display_name", user instanceof Member m ? m.getDisplayName() : user.getGlobalName().orElse(user.getUsername()));
		json.put("icon", user instanceof Member m ? m.getEffectiveAvatarUrl() : user.getAvatarUrl());

		if (removed) {
			json.put("removed", true);
		}

		if (user.isBot()) {
			json.put("bot", true);
		}
	}

	public static HTTPResponse lookup(AppRequest req) {
		req.checkLoggedIn();
		var result = JSONObject.of();

		var gcs = new HashMap<String, GuildCollections>();
		var gcLookup = (Function<String, GuildCollections>) id -> {
			var gc1 = req.guild(id);

			if (!gc1.getAuthLevel(req.token.userId).isMember()) {
				throw new UnauthorizedError("You do not have permission to view this guild");
			}

			return gc1;
		};

		var data = req.variable("data").asString();
		var s = data.split("/");

		switch (s[0]) {
			case "u" -> {
				var user = req.app.discordHandler.getUser(Long.parseUnsignedLong(s[1]));
				appendUserData(result, user, false);
			}
			case "m" -> {
				var gc = gcs.computeIfAbsent(s[1], gcLookup);

				try {
					var mem = gc.getMember(Long.parseUnsignedLong(s[2]));
					appendUserData(result, mem, false);

					int c = mem.getColor().block().getRGB();

					if (c != 0) {
						result.put("color", "%06X".formatted(c));
					}
				} catch (Exception ignore) {
					var user = req.app.discordHandler.getUser(Long.parseUnsignedLong(s[2]));
					appendUserData(result, user, true);
				}
			}
			case "r" -> {
				var gc = gcs.computeIfAbsent(s[1], gcLookup);
				var role = gc.getRoleMap().get(Long.parseUnsignedLong(s[2]));

				if (role == null) {
					throw new NotFoundError();
				}

				result.put("id", SnowFlake.str(role.id));
				result.put("name", role.name);
				result.put("display_name", "@" + role.name);
				result.put("color", "%06X".formatted(role.color.getRGB()));
			}
		}

		return JSONResponse.of(result);
	}
}