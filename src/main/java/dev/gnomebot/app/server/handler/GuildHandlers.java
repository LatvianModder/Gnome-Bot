package dev.gnomebot.app.server.handler;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.data.ExportedMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.webutils.ImageUtils;
import dev.latvian.apps.webutils.json.JSONArray;
import dev.latvian.apps.webutils.json.JSONObject;
import dev.latvian.apps.webutils.json.JSONResponse;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import org.bson.conversions.Bson;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GuildHandlers {
	public static Response guilds(ServerRequest request) throws Exception {
		var futures = new ArrayList<CompletableFuture<PanelGuildData>>();

		for (var gc : request.app.db.allGuilds()) {
			futures.add(CompletableFuture.supplyAsync(() -> {
				var authLevel = gc.getAuthLevel(request.token.userId);

				if (authLevel.is(AuthLevel.MEMBER)) {
					return new PanelGuildData(gc, authLevel);
				} else {
					return null;
				}
			}));
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
		var guilds = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).sorted().toList();

		var json = JSONArray.of();

		for (var g : guilds) {
			var o1 = json.addObject();
			o1.put("id", SnowFlake.str(g.gc().guildId));
			o1.put("name", g.gc().toString());
			o1.put("owner", SnowFlake.str(g.gc().ownerId));
			o1.put("authLevel", g.authLevel().name);
		}

		return JSONResponse.of(json);
	}

	public static Response info(ServerRequest request) {
		var json = JSONObject.of();
		json.put("id", SnowFlake.str(request.gc.guildId));
		json.put("name", request.gc.name);
		json.put("owner", request.gc.getGuild().getOwnerId().asString());
		json.put("prefix", request.gc.legacyPrefix.get());
		json.put("invite", request.gc.getInviteUrl());
		return JSONResponse.of(json);
	}

	public static Response banner(ServerRequest request) throws Exception {
		var image = new BufferedImage(320, 76, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(1, 1, 0xFFFF0000);
		return FileResponse.png(image);
	}

	public static Response getSettings(ServerRequest request) {
		var channels = request.gc.getChannelList();
		var roles = request.gc.getRoleList();

		var bs = JSONArray.of();

		for (var setting : request.gc.configHolders.values()) {
			var o = bs.addObject();
			o.put("id", setting.key.id());
			o.put("name", setting.key.title());
			o.put("index", bs.size());
			o.put("type", setting.key.type().getTypeName());
			o.put("value", setting.serialize(1));
		}

		var owner = request.gc.getMember(request.gc.ownerId);

		var json = JSONObject.of();
		json.put("id", SnowFlake.str(request.gc.guildId));
		json.put("name", request.gc.name);
		json.put("canEdit", request.getAuthLevel().is(AuthLevel.OWNER));

		var ow = json.addObject("owner");
		ow.put("id", owner.getId().asString());
		ow.put("name", owner.getUsername());
		ow.put("nickname", owner.getNickname().orElse(""));

		var c = json.addArray("channels");

		for (var channelInfo : channels) {
			var o = c.addObject();
			o.put("id", SnowFlake.str(channelInfo.id));
			o.put("name", channelInfo.getName());
			o.put("visible", channelInfo.canViewChannel(request.token.userId));
			o.put("xp", channelInfo.getXp());
		}

		var r = json.addArray("roles");

		for (var role : roles) {
			if (role.id == request.gc.guildId) {
				continue;
			}

			var o = r.addObject();
			o.put("id", SnowFlake.str(role.id));
			o.put("name", role.name);
			var col = role.color.getRGB();
			o.put("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
		}

		if (request.getAuthLevel().is(AuthLevel.ADMIN)) {
			json.put("basicSettings", bs);
		}

		return JSONResponse.of(json);
	}

	public static Response updateSetting(ServerRequest request) throws Exception {
		var holder = request.gc.getConfigHolder(request.variable("key"));

		if (holder == null) {
			throw new NotFoundResponse("Setting ID not found!");
		}

		var body = request.getMainBody().getText().trim();
		var error = holder.validate(1, body);

		if (!error.isEmpty()) {
			throw new BadRequestResponse(error);
		}

		try {
			holder.deserialize(1, body);
		} catch (Exception ex) {
			throw new BadRequestResponse("Failed to parse value!");
		}

		return JSONResponse.SUCCESS;
	}

	public static Response icon(ServerRequest request) throws Exception {
		var size = Integer.parseInt(request.variable("size"));

		if (size <= 0 || size > 1024) {
			throw HTTPResponseCode.BAD_REQUEST.error("Size " + size + " too large!");
		}

		var url = request.gc.iconUrl;
		BufferedImage img = null;

		if (!url.isEmpty()) {
			if (size > 128) {
				url += "?size=1024";
			}

			try {
				img = ImageUtils.resize(URLRequest.of(url).toImage().block(), size, size);
			} catch (Exception ex) {
			}
		}

		if (img == null) {
			img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

			for (var x = 0; x < size; x++) {
				for (var y = 0; y < size; y++) {
					img.setRGB(x, y, 0xFF000000);
				}
			}
		}

		return FileResponse.png(img);
	}

	public static Response feedbackList(ServerRequest request) {
		var list = request.gc.feedback.query()
				.toStream()
				.sorted((o1, o2) -> Integer.compare(o2.getNumber(), o1.getNumber()))
				.toList();

		var memberCache = request.gc.createMemberCache();

		var object = JSONObject.of();
		object.put("id", SnowFlake.str(request.gc.guildId));
		object.put("name", request.gc.name);
		var array = object.addArray("feedback");
		var canSee = DiscordFeedback.canSee(request.gc, request.getAuthLevel());
		var owner = request.getAuthLevel().is(AuthLevel.OWNER);

		for (var feedback : list) {
			feedback.toJson(array.addObject(), memberCache, canSee, owner);
		}

		return JSONResponse.of(object);
	}

	public static Response feedback(ServerRequest request) throws Exception {
		var id = (int) request.getUnsignedLong("id");
		var feedback = request.gc.feedback.query().eq("number", id).first();

		if (feedback == null) {
			throw HTTPResponseCode.NOT_FOUND.error("Feedback not found!");
		}

		var memberCache = request.gc.createMemberCache();
		var json = JSONObject.of();
		var canSee = DiscordFeedback.canSee(request.gc, request.getAuthLevel());
		feedback.toJson(json, memberCache, canSee, request.getAuthLevel().is(AuthLevel.OWNER));
		return JSONResponse.of(json);
	}

	public static Response pollList(ServerRequest request) throws Exception {
		return JSONResponse.of(JSONArray.of());
	}

	public static Response poll(ServerRequest request) throws Exception {
		return JSONResponse.of(JSONObject.of());
	}

	public static Response members(ServerRequest request) throws Exception {
		return JSONResponse.of(JSONArray.of());
	}

	public static Response member(ServerRequest request) {
		var json = JSONObject.of();
		var id = request.getSnowflake("member");
		json.put("id", SnowFlake.str(id));
		var member = request.gc.getMember(id);

		if (member != null) {
			json.put("name", member.getUsername());
			json.put("nickname", member.getNickname().orElse(""));
			var col = member.getColor().block().getRGB();
			json.put("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
		} else {
			json.put("name", request.app.discordHandler.getUserName(id).orElse("Unknown"));
			json.put("nickname", "");
			json.put("color", "#FFFFFF");
		}

		return JSONResponse.of(json);
	}

	public static Response auditLog(ServerRequest request) {
		var array = JSONArray.of();
		var limit = Math.max(1, Math.min(500, request.query("limit").asInt(200)));
		var skip = Math.max(0, request.query("skip").asInt());
		var type = request.query("type").asString();
		var user = request.query("user").asLong();
		var source = request.query("source").asLong();
		var channel = request.query("channel").asLong();
		var message = request.query("message").asLong();
		var level = request.query("level").asInt();

		var userCache = request.app.discordHandler.createUserCache();
		var entryQuery = request.gc.auditLog.query();
		var availableChannels = request.gc.getChannelList().stream().filter(ci -> ci.canViewChannel(request.member.getId().asLong())).map(ci -> ci.id).collect(Collectors.toSet());

		if (!type.isEmpty()) {
			List<Bson> types = new ArrayList<>();

			for (var s : type.split(",")) {
				types.add(Filters.eq("type", s));
			}

			entryQuery.filter(types.size() == 1 ? types.get(0) : Filters.or(types));
		}

		if (user != 0L) {
			entryQuery.eq("user", user);
		}

		if (source != 0L) {
			entryQuery.eq("source", source);
		}

		if (channel != 0L) {
			entryQuery.eq("channel", channel);
		}

		if (message != 0L) {
			entryQuery.eq("message", message);
		}

		if (level > 0) {
			entryQuery.filter(Filters.gte("level", level));
		}

		for (var entry : entryQuery.limit(limit).skip(skip).descending("timestamp")) {
			var t = entry.getType();

			var o = array.addObject();
			o.put("id", entry.document.getObjectId("_id").toString());
			o.put("type", t.name);
			o.put("flags", entry.getFlags());
			o.put("timestamp", entry.getDate().toInstant().toString());
			o.put("revocable", t.has(GnomeAuditLogEntry.Flags.REVOCABLE));

			if (entry.getChannel() != 0L) {
				var channelId = entry.getChannel();

				if (!availableChannels.contains(channelId)) {
					continue;
				}

				o.put("channel", request.gc.getChannelJson(channelId));
			}

			if (entry.getMessage() != 0L) {
				o.put("message", SnowFlake.str(entry.getMessage()));
			}

			if (entry.getUser() != 0L) {
				o.put("user", userCache.getJson(entry.getUser()));
			}

			if (entry.getSource() != 0L) {
				o.put("source", userCache.getJson(entry.getSource()));
			}

			if (t.has(GnomeAuditLogEntry.Flags.OLD_CONTENT)) {
				o.put("old_content", entry.getOldContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.CONTENT)) {
				o.put("content", entry.getContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.EXTRA)) {
				var mw = entry.getExtra();

				if (!mw.map.isEmpty()) {
					o.put("extra", mw.toJSON());
				}
			}
		}

		return JSONResponse.of(array);
	}

	public static Response exportMessages(ServerRequest request) {
		var id = request.getSnowflake("member");
		var list = new LinkedList<ExportedMessage>();

		for (var m : request.gc.messages.query().eq("user", id)) {
			var message = new ExportedMessage();
			message.timestamp = m.getDate().getTime();
			message.channel = m.getChannelID();
			message.flags = m.flags;
			message.content = m.getContent();
			list.add(message);
		}

		return FileResponse.plainText(list.stream()
				.sorted(ExportedMessage.COMPARATOR)
				.map(ExportedMessage::toString)
				.collect(Collectors.joining("\n"))
		);
	}
}