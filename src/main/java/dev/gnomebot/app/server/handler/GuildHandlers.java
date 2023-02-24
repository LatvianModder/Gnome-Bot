package dev.gnomebot.app.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.CollectionQuery;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.ExportedMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.config.BaseConfig;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.UserCache;
import dev.gnomebot.app.discord.command.ForcePingableNameCommand;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.json.JsonRequest;
import dev.gnomebot.app.server.json.JsonResponse;
import dev.gnomebot.app.util.CharMap;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.Table;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import org.bson.conversions.Bson;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
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

		return JsonResponse.array(json -> {
			for (PanelGuildData g : guilds) {
				JsonObject o1 = new JsonObject();
				o1.addProperty("id", g.id().asString());
				o1.addProperty("name", g.name());
				o1.addProperty("owner", g.owner().asString());
				o1.addProperty("authLevel", g.authLevel().name);
				json.add(o1);
			}
		});
	}

	public static Response info(ServerRequest request) {
		return JsonResponse.object(json -> {
			json.addProperty("id", request.gc.guildId.asString());
			json.addProperty("name", request.gc.name.get());
			json.addProperty("owner", request.gc.getGuild().getOwnerId().asString());
			json.addProperty("prefix", request.gc.legacyPrefix.get());
			json.addProperty("invite", request.gc.inviteCode.get());
		});
	}

	public static Response banner(ServerRequest request) throws Exception {
		BufferedImage image = new BufferedImage(320, 76, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(1, 1, 0xFFFF0000);
		return FileResponse.image(image);
	}

	public static Response getSettings(ServerRequest request) {
		List<ChannelInfo> channels = request.gc.getChannelList();
		List<CachedRole> roles = request.gc.getRoleList();

		JsonArray bs = new JsonArray();

		for (BaseConfig<?> setting : request.gc.config.map.values()) {
			if (setting.internal) {
				continue;
			}

			JsonElement e = setting.toJson();

			if (!e.isJsonNull()) {
				JsonObject o = new JsonObject();
				o.addProperty("id", setting.id);
				o.addProperty("name", setting.title);
				o.addProperty("index", bs.size());
				o.addProperty("type", setting.getType());
				o.add("value", e);
				bs.add(o);
			}
		}

		Member owner = request.gc.getMember(request.gc.getGuild().getOwnerId());

		return JsonResponse.object(json -> {
			json.addProperty("id", request.gc.guildId.asString());
			json.addProperty("name", request.gc.name.get());
			json.addProperty("canEdit", request.getAuthLevel().is(AuthLevel.OWNER));

			JsonObject ow = new JsonObject();
			ow.addProperty("id", owner.getId().asString());
			ow.addProperty("name", owner.getUsername());
			ow.addProperty("nickname", owner.getNickname().orElse(""));
			json.add("owner", ow);

			JsonArray c = new JsonArray();

			for (ChannelInfo channelInfo : channels) {
				JsonObject o = new JsonObject();
				o.addProperty("id", channelInfo.id.asString());
				o.addProperty("name", channelInfo.getName());
				o.addProperty("visible", channelInfo.canViewChannel(request.token.userId));
				o.addProperty("xp", channelInfo.xp);
				c.add(o);
			}

			json.add("channels", c);

			JsonArray r = new JsonArray();

			for (CachedRole role : roles) {
				if (role.id.asLong() == request.gc.guildId.asLong()) {
					continue;
				}

				JsonObject o = new JsonObject();
				o.addProperty("id", role.id.asString());
				o.addProperty("name", role.name);
				int col = role.color.getRGB();
				o.addProperty("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
				r.add(o);
			}

			json.add("roles", r);

			if (request.getAuthLevel().is(AuthLevel.ADMIN)) {
				json.add("basicSettings", bs);
			}
		});
	}

	public static Response updateSetting(JsonRequest request) throws Exception {
		BaseConfig<?> setting = request.request.gc.config.map.get(request.request.variable("setting"));

		if (setting == null) {
			throw HTTPResponseCode.NOT_FOUND.error("Setting ID not found!");
		}

		try {
			setting.fromJson(request.get("value"));
		} catch (Exception ex) {
			throw HTTPResponseCode.BAD_REQUEST.error("Failed to parse value!");
		}

		setting.save();
		return Response.SUCCESS_JSON;
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
				img = Utils.resize(URLRequest.of(url).toImage().block(), size, size);
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

		return FileResponse.image(img);
	}

	public static Response feedbackList(ServerRequest request) {
		List<DiscordFeedback> list = request.gc.feedback.query()
				.toStream()
				.sorted((o1, o2) -> Integer.compare(o2.getNumber(), o1.getNumber()))
				.toList();

		MemberCache memberCache = request.gc.createMemberCache();

		return JsonResponse.object(object -> {
			object.addProperty("id", request.gc.guildId.asString());
			object.addProperty("name", request.gc.name.get());
			JsonArray array = new JsonArray();

			for (DiscordFeedback feedback : list) {
				JsonObject json = new JsonObject();
				feedback.toJson(request.gc, json, memberCache, request.getAuthLevel());
				array.add(json);
			}

			object.add("feedback", array);
		});
	}

	public static Response feedback(ServerRequest request) throws Exception {
		int id = (int) request.getUnsignedLong("id");
		DiscordFeedback feedback = request.gc.feedback.query().eq("number", id).first();

		if (feedback == null) {
			throw HTTPResponseCode.NOT_FOUND.error("Feedback not found!");
		}

		MemberCache memberCache = request.gc.createMemberCache();
		return JsonResponse.object(json -> feedback.toJson(request.gc, json, memberCache, request.getAuthLevel()));
	}

	public static Response pollList(ServerRequest request) throws Exception {
		return JsonResponse.array(json -> {

		});
	}

	public static Response poll(ServerRequest request) throws Exception {
		return JsonResponse.object(json -> {

		});
	}

	public static Response members(ServerRequest request) throws Exception {
		return JsonResponse.array(json -> {

		});
	}

	public static Response member(ServerRequest request) {
		return JsonResponse.object(json -> {
			Snowflake id = Snowflake.of(request.variable("member"));
			json.addProperty("id", id.asString());
			Member member = request.gc.getMember(id);

			if (member != null) {
				json.addProperty("name", member.getUsername());
				json.addProperty("nickname", member.getNickname().orElse(""));
				int col = member.getColor().block().getRGB();
				json.addProperty("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
			} else {
				json.addProperty("name", request.app.discordHandler.getUserName(id).orElse("Unknown"));
				json.addProperty("nickname", "");
				json.addProperty("color", "#FFFFFF");
			}
		});
	}

	public static Response unpingableNames(ServerRequest request) throws Exception {
		Table table = new Table("Order", "ID", "Original Name", "Pingable Name");

		for (Member m : request.gc.getGuild().getMembers().filter(m -> !CharMap.isPingable(m.getUsername()) && !CharMap.isPingable(m.getDisplayName())).sort((o1, o2) -> o1.getUsername().compareToIgnoreCase(o2.getUsername())).toIterable()) {
			String s = m.getUsername();
			String n = ForcePingableNameCommand.makePingable(s, m.getId().asLong());

			if (!s.equals(n)) {
				table.addRow(table.rows.size() + 1, m.getId().asString(), m.getTag(), n);
			}
		}

		return FileResponse.plainText(String.join("\n", table.getCSVLines(false)));
	}

	public static Response auditLog(ServerRequest request) {
		JsonArray array = new JsonArray();
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

			for (String s : type.split(",")) {
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

		for (GnomeAuditLogEntry entry : entryQuery.limit(limit).skip(skip).descending("timestamp")) {
			GnomeAuditLogEntry.Type t = entry.getType();

			JsonObject o = new JsonObject();
			o.addProperty("id", entry.document.getObjectId("_id").toString());
			o.addProperty("type", t.name);
			o.addProperty("timestamp", entry.getDate().toInstant().toString());
			o.addProperty("level", t.level);
			o.addProperty("revocable", t.has(GnomeAuditLogEntry.Flags.REVOCABLE));

			if (t.has(GnomeAuditLogEntry.Flags.CHANNEL)) {
				Snowflake channelId = Snowflake.of(entry.getChannel());

				if (!availableChannels.contains(channelId)) {
					continue;
				}

				o.add("channel", request.gc.getChannelJson(channelId));
			}

			if (t.has(GnomeAuditLogEntry.Flags.MESSAGE)) {
				o.addProperty("message", Snowflake.of(entry.getMessage()).asString());
			}

			if (t.has(GnomeAuditLogEntry.Flags.USER)) {
				o.add("user", userCache.getJson(Snowflake.of(entry.getUser())));
			}

			if (t.has(GnomeAuditLogEntry.Flags.SOURCE)) {
				o.add("source", userCache.getJson(Snowflake.of(entry.getSource())));
			}

			if (t.has(GnomeAuditLogEntry.Flags.OLD_CONTENT)) {
				o.addProperty("old_content", entry.getOldContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.CONTENT)) {
				o.addProperty("content", entry.getContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.EXTRA)) {
				MapWrapper mw = entry.getExtra();

				if (!mw.map.isEmpty()) {
					o.add("extra", mw.toJson());
				}
			}

			array.add(o);
		}

		return JsonResponse.array(array);
	}

	public static Response slurRegex(ServerRequest request) {
		if (request.gc.badWordRegex == null) {
			return Response.NO_CONTENT;
		}

		return FileResponse.plainText(request.gc.badWordRegex.toString());
	}

	public static Response exportMessages(ServerRequest request) {
		Snowflake id = Snowflake.of(request.variable("member"));
		LinkedList<ExportedMessage> list = new LinkedList<>();

		for (DiscordMessage m : request.gc.messages.query().eq("user", id.asLong())) {
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