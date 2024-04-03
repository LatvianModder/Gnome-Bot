package dev.gnomebot.app.server.handler;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.ScheduledTask;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.ansi.Table;
import dev.latvian.apps.webutils.data.MutableInt;
import dev.latvian.apps.webutils.data.Pair;
import dev.latvian.apps.webutils.net.Response;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.BanData;
import discord4j.rest.route.Routes;
import discord4j.rest.util.PaginationUtil;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogHandlers {
	public static Response mutes(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Mutes - " + request.gc);
		root.content.a("/guild/" + request.gc.guildId, "< Back").classes("back");

		var list = root.content.ul();
		var members = new ArrayList<Pair<Long, String>>();

		for (var task : request.app.scheduledTasks) {
			if (task.guildId == request.gc.guildId && task.type.equals(ScheduledTask.UNMUTE)) {
				var m = request.gc.getMember(task.userId);

				if (m != null) {
					members.add(Pair.of(task.userId, m.getDisplayName()));
				} else {
					members.add(Pair.of(task.userId, request.app.discordHandler.getUserName(task.userId).orElse("Unknown")));
				}
			}
		}

		members.sort((o1, o2) -> o1.b().compareToIgnoreCase(o2.b()));

		for (var member : members) {
			list.li().a("/guild/" + request.gc.guildId + "/members/" + member.a(), member.b());
		}

		return root.asResponse();
	}

	public static Response auditLog(ServerRequest request) {
		var limit = Math.max(1, Math.min(500, request.query("limit").asInt(200)));
		var skip = Math.max(0, request.query("skip").asInt());
		var type = request.query("type").asString();
		var user = request.query("user").asLong();
		var source = request.query("source").asLong();
		var channel = request.query("channel").asLong();
		var message = request.query("message").asLong();
		var level = request.query("level").asInt();

		var root = GnomeRootTag.createSimple(request.getPath(), "Audit Log - " + request.gc);
		root.content.a("/guild/" + request.gc.guildId, "< Back").classes("back");

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

		var table = new Table("Timestamp", "Type", "Channel", "User", "Content", "Source");

		for (var entry : entryQuery.limit(limit).skip(skip).descending("timestamp")) {
			var t = entry.getType();

			if (t.has(GnomeAuditLogEntry.Flags.BOT_USER_IGNORED) && entry.getUser() != 0L && userCache.get(entry.getUser()).map(User::isBot).orElse(false)) {
				continue;
			}

			var cells = table.addRow();

			cells[0].value(entry.getDate().toInstant().toString());
			cells[1].value(t.displayName);

			if (entry.getChannel() != 0L) {
				var channelId = entry.getChannel();

				if (!availableChannels.contains(channelId)) {
					continue;
				}

				cells[2].value("#" + request.gc.getChannelName(channelId));
			}

			if (entry.getUser() != 0L) {
				cells[3].value(userCache.getUsername(entry.getUser()));
			}

			if (t.has(GnomeAuditLogEntry.Flags.CONTENT)) {
				cells[4].value(entry.getContent());
			}

			if (entry.getSource() != 0L) {
				cells[5].value(userCache.getUsername(entry.getSource()));
			}
		}

		root.content.add(table.toTag().classes("auditlogtable"));
		return root.asResponse();
	}

	private record BanEntry(String id, String name, String displayName, String reason, String discriminator) {
	}

	public static Response bans(ServerRequest request) {
		var list = new ArrayList<BanEntry>();
		// int count = 0;

		var guild = request.gc.getGuild();

		var badNamePattern = Pattern.compile("[^\\w-.()!$^*+=~\\s]");

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
					u.globalName().orElse(""),
					entry.reason().orElse(""),
					u.discriminator().equals("0") ? "" : u.discriminator()
					)
			);
		}

		list.sort((o1, o2) -> o1.name.compareToIgnoreCase(o2.name));

		var root = GnomeRootTag.createSimple(request.getPath(), "Bans - " + request.gc);
		root.content.style("width:100%");
		root.content.a("/guild/" + request.gc.guildId, "< Back").classes("back");

		var spamList = new ArrayList<BanEntry>();
		var hackList = new ArrayList<BanEntry>();
		var lfgList = new ArrayList<BanEntry>();
		var otherList = new ArrayList<BanEntry>();
		var noReasonList = new ArrayList<BanEntry>();
		var deletedUsers = 0;
		var deletedUserReasons = new HashMap<String, Pair<String, MutableInt>>();

		var lists = List.of(Pair.of("Spam / Scam", spamList), Pair.of("Hacks", hackList), Pair.of("LFG", lfgList), Pair.of("Other", otherList), Pair.of("Unknown Reason", noReasonList));

		for (var entry : list) {
			var reason = entry.reason.trim().toLowerCase();

			if (!entry.discriminator.isEmpty() && entry.name.length() == 21 && entry.name.startsWith("Deleted User ")) {
				deletedUsers++;
				deletedUserReasons.computeIfAbsent(reason, r -> Pair.of(entry.reason, new MutableInt())).b().add(1);
			} else if (reason.isEmpty()) {
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

			var table = new Table("#", "Name", "Reason");
			var indexFormat = "%0" + String.valueOf(list1.size()).length() + "d";

			for (var i = 0; i < list1.size(); i++) {
				var entry = list1.get(i);
				var cells = table.addRow();

				cells[0].value(String.format(indexFormat, i + 1));

				var name = badNamePattern.matcher(entry.name).find() ? "(Cringe Name)" : entry.name;
				var displayName = entry.displayName.isEmpty() ? "" : badNamePattern.matcher(entry.displayName).find() ? "(Cringe Name)" : entry.displayName;

				var nameTag = cells[1].tag().a("/guild/" + request.gc.guildId + "/members/" + entry.id);

				if (name.equals("(Cringe Name)") && displayName.equals("(Cringe Name)")) {
					nameTag.string("(Cringe Name)");
				} else if (displayName.equals("(Cringe Name)") || displayName.isEmpty()) {
					nameTag.string(name + (entry.discriminator.isEmpty() ? "" : "#" + entry.discriminator));
				} else {
					nameTag.string(displayName + (entry.discriminator.isEmpty() ? "" : "#" + entry.discriminator));
				}

				cells[2].value(entry.reason);
			}

			root.content.paired("details").classes("bantable").paired("summary").string(listEntry.a() + " [" + list1.size() + "]").end().add(table.toTag());
		}

		deletedUserReasons.remove("");

		if (deletedUsers > 0) {
			var ul = root.content.paired("details").classes("bantable").paired("summary").string("Deleted Users [" + deletedUsers + "]").end().ul();

			for (var reason : deletedUserReasons.values().stream().sorted((o1, o2) -> Integer.compare(o2.b().value, o1.b().value)).toList()) {
				ul.li().string(reason.a() + " [" + reason.b().value + "]");
			}
		}

		return root.asResponse();
	}
}