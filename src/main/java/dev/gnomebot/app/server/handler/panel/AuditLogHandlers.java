package dev.gnomebot.app.server.handler.panel;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.CollectionQuery;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.UserCache;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.ansi.Table;
import dev.latvian.apps.webutils.net.Response;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.BanData;
import discord4j.rest.route.Routes;
import discord4j.rest.util.PaginationUtil;
import io.javalin.http.HttpStatus;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AuditLogHandlers {
	public static Response offenses(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Offenses");
		root.content.p().string("Uh... nothing for now...");
		return root.asResponse();
	}

	public static Response offensesOf(ServerRequest request) {
		var userId = Snowflake.of(request.variable("user"));
		var user = request.app.discordHandler.getUser(userId);
		var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Offenses of " + (user == null ? "Unknown User" : user.getUsername()));

		if (user == null) {
			root.content.p().classes("red").string("User not found!");
			return root.asResponse(HttpStatus.NOT_FOUND, true);
		}

		root.content.p().string("Uh... nothing for now...");
		return root.asResponse();
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

		var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Audit Log");

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

		var table = new Table("Timestamp", "Type", "Channel", "User", "Old Content", "Content", "Source");

		for (GnomeAuditLogEntry entry : entryQuery.limit(limit).skip(skip).descending("timestamp")) {
			GnomeAuditLogEntry.Type t = entry.getType();

			if (t.has(GnomeAuditLogEntry.Flags.BOT_USER_IGNORED) && entry.getUser() != 0L && userCache.get(Snowflake.of(entry.getUser())).map(User::isBot).orElse(false)) {
				continue;
			}

			Table.Cell[] cells = table.addRow();

			cells[0].value(entry.getDate().toInstant().toString());
			cells[1].value(t.name);

			if (entry.getChannel() != 0L) {
				Snowflake channelId = Snowflake.of(entry.getChannel());

				if (!availableChannels.contains(channelId)) {
					continue;
				}

				cells[2].value("#" + request.gc.getChannelName(channelId));
			}

			if (entry.getUser() != 0L) {
				cells[3].value(userCache.getUsername(Snowflake.of(entry.getUser())));
			}

			if (t.has(GnomeAuditLogEntry.Flags.OLD_CONTENT)) {
				cells[4].value(entry.getOldContent());
			}

			if (t.has(GnomeAuditLogEntry.Flags.CONTENT)) {
				cells[5].value(entry.getContent());
			}

			if (entry.getSource() != 0L) {
				cells[6].value(userCache.getUsername(Snowflake.of(entry.getSource())));
			}
		}

		root.content.add(table.toTag().classes("auditlogtable"));
		return root.asResponse();
	}

	private record BanEntry(String id, String name, String displayName, String reason) {
	}

	public static Response bans(ServerRequest request) {
		var list = new ArrayList<BanEntry>();
		// int count = 0;

		final Guild guild = request.gc.getGuild();

		// Required until D4J fixes ban pagination
		for (var entry : PaginationUtil.paginateAfter(params -> Routes.GUILD_BANS_GET.newRequest(guild.getId().asLong())
				.query(params)
				.exchange(guild.getClient().getCoreResources().getRouter())
				.bodyToMono(BanData[].class)
				.flatMapMany(Flux::fromArray), data -> data.user().id().asLong(), 0L, 1000).toIterable()) {
			var u = entry.user();
			// count++;
			// App.info("Ban #%05d %s: %s".formatted(count, u.username(), entry.reason().orElse("Unknown")));

			list.add(new BanEntry(
							u.id().asString(),
							u.username(),
					u.globalName().orElse(u.username()),
							entry.reason().orElse("")
					)
			);
		}

		list.sort((o1, o2) -> o1.displayName.compareToIgnoreCase(o2.displayName));

		var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc + " - Bans");
		var table = new Table("#", "ID", "Name", "Reason");

		String indexFormat = "%0" + String.valueOf(list.size()).length() + "d";

		for (int i = 0; i < list.size(); i++) {
			var entry = list.get(i);
			var cells = table.addRow();

			cells[0].value(String.format(indexFormat, i + 1));
			cells[1].value(entry.id);

			if (entry.displayName.equals(entry.name)) {
				cells[2].value(entry.displayName);
			} else {
				cells[2].tag().span().title(entry.name).string(entry.displayName);
			}

			cells[3].value(entry.reason);
		}

		root.content.add(table.toTag().classes("bantable"));
		return root.asResponse();
	}
}