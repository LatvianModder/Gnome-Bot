package dev.gnomebot.app.server.handler;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.CollectionQuery;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.data.ExportedMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.config.BaseConfig;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.UserCache;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.json.JsonRequest;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.webutils.ImageUtils;
import dev.latvian.apps.webutils.json.JSONArray;
import dev.latvian.apps.webutils.json.JSONObject;
import dev.latvian.apps.webutils.json.JSONResponse;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import org.bson.conversions.Bson;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GuildHandlers {
	public static Response guilds(ServerRequest request) throws Exception {
		List<PanelGuildData> guilds = new ArrayList<>();

		for (long guildId0 : request.token.getGuildIds(request.app.discordHandler)) {
			Snowflake guildId = Snowflake.of(guildId0);
			GuildCollections gc = request.app.db.guild(guildId);
			AuthLevel authLevel = gc.getAuthLevel(request.token.userId);

			if (authLevel.is(AuthLevel.MEMBER)) {
				guilds.add(new PanelGuildData(guildId, gc.toString(), gc.ownerId.get(), authLevel));
			}
		}

		guilds.sort((o1, o2) -> o1.name().compareToIgnoreCase(o2.name()));

		var json = JSONArray.of();

		for (var g : guilds) {
			var o1 = json.addObject();
			o1.put("id", g.id().asString());
			o1.put("name", g.name());
			o1.put("owner", g.owner().asString());
			o1.put("authLevel", g.authLevel().name);
		}

		return JSONResponse.of(json);
	}

	public static Response info(ServerRequest request) {
		var json = JSONObject.of();
		json.put("id", request.gc.guildId.asString());
		json.put("name", request.gc.name.get());
		json.put("owner", request.gc.getGuild().getOwnerId().asString());
		json.put("prefix", request.gc.legacyPrefix.get());
		json.put("invite", request.gc.inviteCode.get());
		return JSONResponse.of(json);
	}

	public static Response banner(ServerRequest request) throws Exception {
		BufferedImage image = new BufferedImage(320, 76, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(1, 1, 0xFFFF0000);
		return FileResponse.png(image);
	}

	public static Response getSettings(ServerRequest request) {
		List<ChannelInfo> channels = request.gc.getChannelList();
		List<CachedRole> roles = request.gc.getRoleList();

		var bs = JSONArray.of();

		for (var setting : request.gc.config.map.values()) {
			if (!setting.internal) {
				var o = bs.addObject();
				o.put("id", setting.id);
				o.put("name", setting.title);
				o.put("index", bs.size());
				o.put("type", setting.getType());
				o.put("value", setting.serialize());
			}
		}

		Member owner = request.gc.getMember(request.gc.getGuild().getOwnerId());

		var json = JSONObject.of();
		json.put("id", request.gc.guildId.asString());
		json.put("name", request.gc.name.get());
		json.put("canEdit", request.getAuthLevel().is(AuthLevel.OWNER));

		var ow = json.addObject("owner");
		ow.put("id", owner.getId().asString());
		ow.put("name", owner.getUsername());
		ow.put("nickname", owner.getNickname().orElse(""));

		var c = json.addArray("channels");

		for (var channelInfo : channels) {
			var o = c.addObject();
			o.put("id", channelInfo.id.asString());
			o.put("name", channelInfo.getName());
			o.put("visible", channelInfo.canViewChannel(request.token.userId));
			o.put("xp", channelInfo.getXp());
		}

		var r = json.addArray("roles");

		for (var role : roles) {
			if (role.id.asLong() == request.gc.guildId.asLong()) {
				continue;
			}

			var o = r.addObject();
			o.put("id", role.id.asString());
			o.put("name", role.name);
			int col = role.color.getRGB();
			o.put("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
		}

		if (request.getAuthLevel().is(AuthLevel.ADMIN)) {
			json.put("basicSettings", bs);
		}

		return JSONResponse.of(json);
	}

	public static Response updateSetting(JsonRequest request) throws Exception {
		BaseConfig<?> setting = request.request.gc.config.map.get(request.request.variable("setting"));

		if (setting == null) {
			throw HTTPResponseCode.NOT_FOUND.error("Setting ID not found!");
		}

		try {
			setting.deserialize(request.getString("value"));
		} catch (Exception ex) {
			throw HTTPResponseCode.BAD_REQUEST.error("Failed to parse value!");
		}

		setting.save();
		return JSONResponse.SUCCESS;
	}

	public static Response icon(ServerRequest request) throws Exception {
		int size = Integer.parseInt(request.variable("size"));

		if (size <= 0 || size > 1024) {
			throw HTTPResponseCode.BAD_REQUEST.error("Size " + size + " too large!");
		}

		String url = request.gc.iconUrl.get();
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

			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
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
		object.put("id", request.gc.guildId.asString());
		object.put("name", request.gc.name.get());
		var array = object.addArray("feedback");

		for (var feedback : list) {
			feedback.toJson(request.gc, array.addObject(), memberCache, request.getAuthLevel());
		}

		return JSONResponse.of(object);
	}

	public static Response feedback(ServerRequest request) throws Exception {
		int id = (int) request.getUnsignedLong("id");
		DiscordFeedback feedback = request.gc.feedback.query().eq("number", id).first();

		if (feedback == null) {
			throw HTTPResponseCode.NOT_FOUND.error("Feedback not found!");
		}

		MemberCache memberCache = request.gc.createMemberCache();
		var json = JSONObject.of();
		feedback.toJson(request.gc, json, memberCache, request.getAuthLevel());
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
		var id = Snowflake.of(request.variable("member"));
		json.put("id", id.asString());
		var member = request.gc.getMember(id);

		if (member != null) {
			json.put("name", member.getUsername());
			json.put("nickname", member.getNickname().orElse(""));
			int col = member.getColor().block().getRGB();
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
		int limit = Math.max(1, Math.min(500, request.query("limit").asInt(200)));
		int skip = Math.max(0, request.query("skip").asInt());
		String type = request.query("type").asString();
		long user = request.query("user").asLong();
		long source = request.query("source").asLong();
		long channel = request.query("channel").asLong();
		long message = request.query("message").asLong();
		int level = request.query("level").asInt();

		UserCache userCache = request.app.discordHandler.createUserCache();
		CollectionQuery<GnomeAuditLogEntry> entryQuery = request.gc.auditLog.query();
		Set<Snowflake> availableChannels = request.gc.getChannelList().stream().filter(ci -> ci.canViewChannel(request.member.getId())).map(ci -> ci.id).collect(Collectors.toSet());

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
				Snowflake channelId = Snowflake.of(entry.getChannel());

				if (!availableChannels.contains(channelId)) {
					continue;
				}

				o.put("channel", request.gc.getChannelJson(channelId));
			}

			if (entry.getMessage() != 0L) {
				o.put("message", Snowflake.of(entry.getMessage()).asString());
			}

			if (entry.getUser() != 0L) {
				o.put("user", userCache.getJson(Snowflake.of(entry.getUser())));
			}

			if (entry.getSource() != 0L) {
				o.put("source", userCache.getJson(Snowflake.of(entry.getSource())));
			}

			if (t.has(GnomeAuditLogEntry.Flags.OLD_CONTENT)) {
				o.put("old_content", entry.getOldContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.CONTENT)) {
				o.put("content", entry.getContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.EXTRA)) {
				MapWrapper mw = entry.getExtra();

				if (!mw.map.isEmpty()) {
					o.put("extra", mw.toJSON());
				}
			}
		}

		return JSONResponse.of(array);
	}

	public static Response exportMessages(ServerRequest request) {
		Snowflake id = Snowflake.of(request.variable("member"));
		LinkedList<ExportedMessage> list = new LinkedList<>();

		for (var m : request.gc.messages.query().eq("user", id.asLong())) {
			ExportedMessage message = new ExportedMessage();
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