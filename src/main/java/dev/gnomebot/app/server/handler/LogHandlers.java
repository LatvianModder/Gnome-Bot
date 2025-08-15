package dev.gnomebot.app.server.handler;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.ScheduledTask;
import dev.gnomebot.app.server.AppRequest;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.tinyserver.http.response.HTTPResponse;
import dev.latvian.apps.webutils.data.Pair;
import dev.latvian.apps.webutils.html.HTMLTable;
import discord4j.discordjson.json.BanData;
import discord4j.rest.route.Routes;
import discord4j.rest.util.PaginationUtil;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogHandlers {
	private static final Pattern USER_PATTERN = Pattern.compile("<@!?(\\d+)>", Pattern.CASE_INSENSITIVE);
	private static final Pattern ROLE_PATTERN = Pattern.compile("<@&(\\d+)>", Pattern.CASE_INSENSITIVE);

	public static HTTPResponse mutes(AppRequest req) {
		req.checkAdmin();

		var root = req.createRoot("Mutes - " + req.gc);
		root.content.a(req.gc.url(), "< Back").classes("back");

		var list = root.content.ul();
		var members = new ArrayList<Pair<Long, Long>>();

		for (var task : req.app.scheduledTasks) {
			if (task.guildId == req.gc.guildId && task.type.equals(ScheduledTask.UNMUTE)) {
				members.add(Pair.of(task.userId, task.end));
			}
		}

		members.sort(Comparator.comparingLong(Pair::b));

		for (var member : members) {
			GuildAPIHandlers.member(list.li(), req.gc, member.a());
		}

		return root.asResponse();
	}

	public static HTTPResponse auditLog(AppRequest req) {
		req.checkAdmin();

		var limit = Math.max(1, Math.min(500, req.query("limit").asInt(200)));
		var skip = Math.max(0, req.query("skip").asInt());
		var type = req.query("type").asString();
		var user = req.query("user").asLong();
		var source = req.query("source").asLong();
		var channel = req.query("channel").asLong();
		var message = req.query("message").asLong();
		var level = req.query("level").asInt();

		var root = req.createRoot("Audit Log - " + req.gc);
		root.content.a(req.gc.url(), "< Back").classes("back");

		var entryQuery = req.gc.auditLog.query();
		var availableChannels = req.gc.channels().list.stream().filter(ci -> ci.canViewChannel(req.member().getId().asLong())).map(ci -> ci.id).collect(Collectors.toSet());

		if (!type.isEmpty()) {
			var types = new ArrayList<Bson>();

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

		var table = new HTMLTable("#", "Timestamp", "Type", "Channel", "User", "Content", "Source");

		int i = 0;

		for (var entry : entryQuery.limit(limit).skip(skip).descending("_id")) {
			var t = entry.type();

			var cells = table.addRow();
			i++;
			int j = i + skip;

			cells.set(0, tag -> tag.string(j));
			cells.set(1, tag -> tag.string(entry.timestamp().toInstant()));
			cells.set(2, tag -> tag.string(t.displayName));

			if (entry.channel() != 0L) {
				var channelId = entry.channel();

				if (!availableChannels.contains(channelId)) {
					continue;
				}

				cells.set(3, tag -> GuildAPIHandlers.channel(tag, req.gc, channelId));
			}

			if (entry.user() != 0L) {
				cells.set(4, tag -> GuildAPIHandlers.member(tag, req.gc, entry.user()));
			}

			if (t.has(GnomeAuditLogEntry.Flags.CONTENT)) {
				cells.set(5, tag -> {
					tag.string(entry.content());
					tag.replace(USER_PATTERN, (tag1, matcher) -> GuildAPIHandlers.member(tag1, req.gc, SnowFlake.num(matcher.group(1))));
					tag.replace(ROLE_PATTERN, (tag1, matcher) -> GuildAPIHandlers.role(tag1, req.gc, SnowFlake.num(matcher.group(1))));
				});
			}

			if (entry.source() != 0L) {
				cells.set(6, tag -> GuildAPIHandlers.member(tag, req.gc, entry.source()));
			}
		}

		root.content.div("auditlogtable").add(table);
		return root.asResponse();
	}

	private record BanEntry(String id, String name, String reason, String icon) {
	}

	public static HTTPResponse bans(AppRequest req) {
		req.checkAdmin();

		var list = new ArrayList<BanEntry>();
		// int count = 0;

		var guild = req.gc.getGuild();

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
					entry.reason().orElse(""),
					Utils.getAvatarURL(u)
					)
			);
		}

		list.sort((o1, o2) -> o1.name.compareToIgnoreCase(o2.name));

		var root = req.createRoot("Bans - " + req.gc);
		root.content.style("width:100%");
		root.content.a(req.gc.url(), "< Back").classes("back");

		var spamList = new ArrayList<BanEntry>();
		var hackList = new ArrayList<BanEntry>();
		var lfgList = new ArrayList<BanEntry>();
		var otherList = new ArrayList<BanEntry>();
		var noReasonList = new ArrayList<BanEntry>();
		var deletedUsers = 0;

		var lists = List.of(Pair.of("Spam / Scam", spamList), Pair.of("Hacks", hackList), Pair.of("LFG", lfgList), Pair.of("Other", otherList), Pair.of("Unknown Reason", noReasonList));

		for (var entry : list) {
			var reason = entry.reason.trim().toLowerCase();

			if (entry.name.length() == 25 && entry.name.startsWith("deleted_user_")) {
				deletedUsers++;
			}

			if (reason.isEmpty()) {
				noReasonList.add(entry);
			} else if (reason.contains("lfg")) {
				lfgList.add(entry);
			} else if (reason.contains("hack") || reason.contains("crack") || reason.contains("launcher") || reason.contains("client")) {
				hackList.add(entry);
			} else if (reason.contains("spam") || reason.contains("scam") || reason.contains("@everyone") || reason.contains("@here") || reason.contains("nsfw") || reason.contains("shitpost") || reason.contains("porn")) {
				spamList.add(entry);
			} else {
				otherList.add(entry);
			}
		}

		for (var listEntry : lists) {
			var list1 = listEntry.b();

			if (list1.isEmpty()) {
				continue;
			}

			var table = new HTMLTable("#", "Name", "Reason");
			var indexFormat = "%0" + String.valueOf(list1.size()).length() + "d";

			for (var i = 0; i < list1.size(); i++) {
				var entry = list1.get(i);
				var cells = table.addRow();
				final int j = i;

				cells.set(0, tag -> tag.string(String.format(indexFormat, j + 1)));
				cells.set(1, tag -> GuildAPIHandlers.member(tag, req.gc, SnowFlake.num(entry.id)).attr("data-lookup").img(entry.icon + "?size=24").lazyLoading().end().string(entry.name));
				cells.set(2, tag -> tag.string(entry.reason));
			}

			root.content.paired("details").classes("bantable").paired("summary").string(listEntry.a() + " [" + list1.size() + "]").end().add(table);
		}

		root.content.p().string("Total: " + list.size() + ", Deleted Users: " + deletedUsers);
		return root.asResponse();
	}
}