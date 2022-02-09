package dev.gnomebot.app.server.handler;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.CollectionQuery;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.UserCache;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.html.RootTag;
import dev.gnomebot.app.server.html.Tag;
import dev.gnomebot.app.util.Table;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class PanelHandlers {
	public static Response root(ServerRequest request) {
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

		Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel");

		for (PanelGuildData data : guilds) {
			Tag line = content.p().addClass("withicon");
			line.unpaired("img").attr("src", "/api/guild/icon/" + data.id().asString() + "/128");
			line.a("/panel/" + data.id().asString()).string(data.name());
		}

		return content.asResponse();
	}

	public static Response login(ServerRequest request) {
		if (!request.query("logintoken").isPresent()) {
			Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel");

			if (request.token == null) {
				content.p().string("What the heck? You shouldn't be here, shoo!");
			} else {
				content.p().string("You've successfully logged in, " + request.token.getName() + "!");
				content.p().string("You can now close this page.");
				content.p().a("/panel").string("You can click here to browse guild list.");
			}

			return content.asResponse();
		}

		return Redirect.temporarily(App.url("panel/login"));
	}

	public static Response guild(ServerRequest request) {
		Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc);
		content.p().string("Uh... nothing for now...");
		content.p().a("/guild/" + request.gc.guildId.asString()).string("For now you can go to old page.");
		return content.asResponse();
	}

	public static Response offenses(ServerRequest request) {
		Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Offenses");
		content.p().string("Uh... nothing for now...");
		return content.asResponse();
	}

	public static Response offensesOf(ServerRequest request) {
		Snowflake userId = Snowflake.of(request.variable("user"));
		User user = request.app.discordHandler.getUser(userId);
		Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Offenses of " + (user == null ? "Unknown User" : user.getUsername()));

		if (user == null) {
			content.p().addClass("red").string("User not found!");
			return content.asResponse(HTTPResponseCode.NOT_FOUND);
		}

		content.p().string("Uh... nothing for now...");
		return content.asResponse();
	}

	public static Response auditLog(ServerRequest request) {
		int limit = Math.max(1, Math.min(500, request.query("limit").asInt(200)));
		int skip = Math.max(0, request.query("skip").asInt());
		String type = request.query("type").asString();
		long user = request.query("user").asLong();
		long source = request.query("source").asLong();
		long channel = request.query("channel").asLong();
		long message = request.query("message").asLong();
		int level = request.query("level").asInt();

		Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Audit Log");

		UserCache userCache = request.app.discordHandler.createUserCache();
		CollectionQuery<GnomeAuditLogEntry> entryQuery = request.gc.auditLog.query();
		Set<Snowflake> availableChannels = request.gc.getChannelList().stream().filter(ci -> ci.getPermissions(request.member.getId()).contains(Permission.VIEW_CHANNEL)).map(ci -> ci.id).collect(Collectors.toSet());

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

		Table table = new Table("Timestamp", "Type", "Channel", "User", "Old Content", "Content", "Source");

		for (GnomeAuditLogEntry entry : entryQuery.limit(limit).skip(skip).descending("timestamp")) {
			GnomeAuditLogEntry.Type t = entry.getType();

			if (t.has(GnomeAuditLogEntry.Flags.BOT_USER_IGNORED) && t.has(GnomeAuditLogEntry.Flags.USER) && userCache.get(Snowflake.of(entry.getUser())).map(User::isBot).orElse(false)) {
				continue;
			}

			Table.Cell[] cells = table.addRow();

			cells[0].value(entry.getDate().toInstant().toString());
			cells[1].value(t.name);

			if (t.has(GnomeAuditLogEntry.Flags.CHANNEL)) {
				Snowflake channelId = Snowflake.of(entry.getChannel());

				if (!availableChannels.contains(channelId)) {
					continue;
				}

				cells[2].value("#" + request.gc.getChannelName(channelId));
			}

			if (t.has(GnomeAuditLogEntry.Flags.USER)) {
				cells[3].value(userCache.getUsername(Snowflake.of(entry.getUser())));
			}

			if (t.has(GnomeAuditLogEntry.Flags.OLD_CONTENT)) {
				cells[4].value(entry.getOldContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.CONTENT)) {
				cells[5].value(entry.getContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.SOURCE)) {
				cells[6].value(userCache.getUsername(Snowflake.of(entry.getSource())));
			}
		}

		content.add(table.toTag().addClass("auditlogtable"));
		return content.asResponse();
	}
}