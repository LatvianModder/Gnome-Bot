package dev.gnomebot.app.server.handler;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.data.ExportedMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.server.AppRequest;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.json.JSONArray;
import dev.latvian.apps.json.JSONObject;
import dev.latvian.apps.json.JSONResponse;
import dev.latvian.apps.tinyserver.http.response.HTTPResponse;
import dev.latvian.apps.tinyserver.http.response.error.client.BadRequestError;
import dev.latvian.apps.tinyserver.http.response.error.client.NotFoundError;
import dev.latvian.apps.webutils.ImageUtils;
import dev.latvian.apps.webutils.html.Tag;
import org.bson.conversions.Bson;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GuildAPIHandlers {
	public static Tag channel(Tag parent, GuildCollections gc, long id) {
		return parent.a(gc.url() + "/channels/" + Long.toUnsignedString(id), gc.channels().displayName(id));
	}

	public static Tag user(Tag parent, long id) {
		return parent.a("/users/" + id).attr("data-lookup", "u/" + Long.toUnsignedString(id));
	}

	public static Tag member(Tag parent, GuildCollections gc, long id) {
		return parent.a(gc.url() + "/members/" + id).attr("data-lookup", "m/" + gc.paths.key + "/" + Long.toUnsignedString(id));
	}

	public static Tag role(Tag parent, GuildCollections gc, long id) {
		return parent.a(gc.url() + "/roles/" + Long.toUnsignedString(id)).attr("data-lookup", "r/" + gc.paths.key + "/" + Long.toUnsignedString(id));
	}

	public static HTTPResponse guilds(AppRequest req) throws Exception {
		req.checkLoggedIn();

		var futures = new ArrayList<CompletableFuture<PanelGuildData>>();

		for (var gc : req.app.db.allGuilds()) {
			futures.add(CompletableFuture.supplyAsync(() -> {
				var authLevel = gc.getAuthLevel(req.token.userId);

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
			o1.put("auth_level", g.authLevel().name);
		}

		return JSONResponse.of(json).publicCache(Duration.ofMinutes(1L));
	}

	public static HTTPResponse info(AppRequest req) {
		req.checkMember();

		var json = JSONObject.of();
		json.put("id", SnowFlake.str(req.gc.guildId));
		json.put("name", req.gc.name);
		json.put("owner", req.gc.getGuild().getOwnerId().asString());
		json.put("prefix", req.gc.legacyPrefix.get());
		json.put("invite", req.gc.getInviteUrl());
		return JSONResponse.of(json).publicCache(Duration.ofHours(1L));
	}

	public static HTTPResponse banner(AppRequest req) throws Exception {
		var image = new BufferedImage(320, 76, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(1, 1, 0xFFFF0000);
		return HTTPResponse.ok().png(image).publicCache(Duration.ofSeconds(1L));
	}

	public static HTTPResponse getSettings(AppRequest req) {
		req.checkMember();

		var channels = req.gc.channels();
		var roles = req.gc.roles();

		var bs = JSONArray.of();

		for (var setting : req.gc.configHolders.values()) {
			var o = bs.addObject();
			o.put("id", setting.key.id());
			o.put("name", setting.key.title());
			o.put("index", bs.size());
			o.put("type", setting.key.type().getTypeName());
			o.put("value", setting.serialize(1));
		}

		var json = JSONObject.of();
		json.put("id", SnowFlake.str(req.gc.guildId));
		json.put("name", req.gc.name);
		json.put("canEdit", req.authLevel().isOwner());

		var owner = req.gc.getMember(req.gc.ownerId);

		if (owner != null) {
			var ow = json.addObject("owner");
			ow.put("id", owner.getId().asString());
			ow.put("name", owner.getUsername());
			ow.put("nickname", owner.getNickname().orElse(""));
		}

		var c = json.addArray("channels");

		for (var channelInfo : channels) {
			var o = c.addObject();
			o.put("id", SnowFlake.str(channelInfo.id));
			o.put("name", channelInfo.getName());
			o.put("visible", channelInfo.canViewChannel(req.token.userId));
			o.put("xp", channelInfo.getXp());
		}

		var r = json.addArray("roles");

		for (var role : roles) {
			if (role.id == req.gc.guildId) {
				continue;
			}

			var o = r.addObject();
			o.put("id", SnowFlake.str(role.id));
			o.put("name", role.name);
			o.put("color", String.format("#%06X", role.getRGB()));
		}

		if (req.authLevel().isAdmin()) {
			json.put("basicSettings", bs);
		}

		return JSONResponse.of(json);
	}

	public static HTTPResponse updateSetting(AppRequest req) throws Exception {
		req.checkOwner();

		var holder = req.gc.getConfigHolder(req.variable("key").asString());

		if (holder == null) {
			throw new NotFoundError("Setting ID not found!");
		}

		var body = req.mainBody().text().trim();
		var error = holder.validate(1, body);

		if (!error.isEmpty()) {
			throw new BadRequestError(error);
		}

		try {
			holder.deserialize(1, body);
		} catch (Exception ex) {
			throw new BadRequestError("Failed to parse value!");
		}

		return JSONResponse.SUCCESS;
	}

	public static HTTPResponse icon(AppRequest req) throws Exception {
		var size = req.variable("size").asInt();

		if (size <= 0 || size > 1024) {
			throw new BadRequestError("Size " + size + " too large!");
		}

		var url = req.gc.iconUrl;
		BufferedImage img = null;

		if (!url.isEmpty()) {
			if (size > 128) {
				url += "?size=1024";
			}

			try {
				img = ImageUtils.resize(URLRequest.of(url).toImage().block(), size, size);
			} catch (Exception ignored) {
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

		return HTTPResponse.ok().png(img).publicCache(Duration.ofDays(1L));
	}

	public static HTTPResponse feedbackList(AppRequest req) {
		req.checkMember();

		var list = req.gc.feedback.query()
				.toStream()
				.sorted((o1, o2) -> Integer.compare(o2.number(), o1.number()))
				.toList();

		var memberCache = req.gc.createMemberCache();

		var object = JSONObject.of();
		object.put("id", SnowFlake.str(req.gc.guildId));
		object.put("name", req.gc.name);
		var array = object.addArray("feedback");
		var canSee = DiscordFeedback.canSee(req.gc, req.authLevel());
		var owner = req.authLevel().isOwner();

		for (var feedback : list) {
			feedback.toJson(array.addObject(), memberCache, canSee, owner);
		}

		return JSONResponse.of(object).publicCache(Duration.ofMinutes(1L));
	}

	public static HTTPResponse feedback(AppRequest req) throws Exception {
		req.checkMember();

		var id = req.variable("id").asInt();
		var feedback = req.gc.feedback.query().eq("number", id).first();

		if (feedback == null) {
			throw new NotFoundError("Feedback not found!");
		}

		var memberCache = req.gc.createMemberCache();
		var json = JSONObject.of();
		var canSee = DiscordFeedback.canSee(req.gc, req.authLevel());
		feedback.toJson(json, memberCache, canSee, req.authLevel().isOwner());
		return JSONResponse.of(json);
	}

	public static HTTPResponse pollList(AppRequest req) throws Exception {
		req.checkMember();

		return JSONResponse.of(JSONArray.of());
	}

	public static HTTPResponse poll(AppRequest req) throws Exception {
		req.checkMember();

		return JSONResponse.of(JSONObject.of());
	}

	public static HTTPResponse members(AppRequest req) throws Exception {
		req.checkMember();

		return JSONResponse.of(JSONArray.of()).publicCache(Duration.ofMinutes(5L));
	}

	public static HTTPResponse member(AppRequest req) {
		req.checkMember();

		var json = JSONObject.of();
		var id = req.getSnowflake("member");
		json.put("id", SnowFlake.str(id));
		var member = req.gc.getMember(id);

		if (member != null) {
			json.put("name", member.getUsername());
			json.put("nickname", member.getNickname().orElse(""));
			var col = member.getColor().block().getRGB();
			json.put("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
		} else {
			json.put("name", req.app.discordHandler.getUserName(id).orElse("Unknown"));
			json.put("nickname", "");
			json.put("color", "#FFFFFF");
		}

		return JSONResponse.of(json).publicCache(Duration.ofMinutes(5L));
	}

	public static HTTPResponse auditLog(AppRequest req) {
		req.checkAdmin();

		var array = JSONArray.of();
		var limit = Math.max(1, Math.min(500, req.query("limit").asInt(200)));
		var skip = Math.max(0, req.query("skip").asInt());
		var type = req.query("type").asString();
		var user = req.query("user").asLong();
		var source = req.query("source").asLong();
		var channel = req.query("channel").asLong();
		var message = req.query("message").asLong();
		var level = req.query("level").asInt();

		var userCache = req.app.discordHandler.createUserCache();
		var entryQuery = req.gc.auditLog.query();
		var availableChannels = req.gc.channels().list.stream().filter(ci -> ci.canViewChannel(req.member().getId().asLong())).map(ci -> ci.id).collect(Collectors.toSet());

		if (!type.isEmpty()) {
			List<Bson> types = new ArrayList<>();

			for (var s : type.split(",")) {
				types.add(Filters.eq("type", s));
			}

			entryQuery.filter(types.size() == 1 ? types.getFirst() : Filters.or(types));
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
			var t = entry.type();

			var o = array.addObject();
			o.put("id", entry.document.getObjectId("_id").toString());
			o.put("type", t.name);
			o.put("flags", entry.flags());
			o.put("timestamp", entry.timestamp().toInstant().toString());
			o.put("revocable", t.has(GnomeAuditLogEntry.Flags.REVOCABLE));

			if (entry.channel() != 0L) {
				var channelId = entry.channel();

				if (!availableChannels.contains(channelId)) {
					continue;
				}

				o.put("channel", req.gc.channels().json(channelId));
			}

			if (entry.message() != 0L) {
				o.put("message", SnowFlake.str(entry.message()));
			}

			if (entry.user() != 0L) {
				o.put("user", userCache.getJson(entry.user()));
			}

			if (entry.source() != 0L) {
				o.put("source", userCache.getJson(entry.source()));
			}

			if (t.has(GnomeAuditLogEntry.Flags.CONTENT)) {
				o.put("content", entry.content());
			}

			if (t.has(GnomeAuditLogEntry.Flags.EXTRA)) {
				var mw = entry.extra();

				if (!mw.map.isEmpty()) {
					o.put("extra", mw.toJSON());
				}
			}
		}

		return JSONResponse.of(array);
	}

	public static HTTPResponse exportMessages(AppRequest req) {
		req.checkAdmin();

		var id = req.getSnowflake("member");
		var list = new LinkedList<ExportedMessage>();

		for (var m : req.gc.messages.query().eq("user", id)) {
			var message = new ExportedMessage();
			message.timestamp = m.timestamp().getTime();
			message.channel = m.getChannelID();
			message.flags = m.flags;
			message.content = m.getContent();
			list.add(message);
		}

		return HTTPResponse.ok().text(list.stream()
				.sorted(ExportedMessage.COMPARATOR)
				.map(ExportedMessage::toString)
				.collect(Collectors.joining("\n"))
		);
	}
}