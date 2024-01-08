package dev.gnomebot.app.server.handler.panel;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.CollectionQuery;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.UserCache;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.ansi.Table;
import dev.latvian.apps.webutils.data.MutableInt;
import dev.latvian.apps.webutils.data.Pair;
import dev.latvian.apps.webutils.net.Response;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.BanData;
import discord4j.rest.route.Routes;
import discord4j.rest.util.PaginationUtil;
import io.javalin.http.HttpStatus;
import org.bson.conversions.Bson;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuditLogHandlers {
	public static Response offenses(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Offenses - " + request.gc);
		root.content.a("/panel/" + request.gc.guildId.asString(), "< Back").classes("back");
		root.content.p().string("Uh... nothing for now...");
		return root.asResponse();
	}

	public static Response offensesOf(ServerRequest request) {
		var userId = request.getSnowflake("user");
		var user = request.app.discordHandler.getUser(userId);
		var root = GnomeRootTag.createSimple(request.getPath(), "Offenses of " + (user == null ? "Unknown User" : user.getUsername()) + " - " + request.gc);
		root.content.a("/panel/" + request.gc.guildId.asString() + "/offenses", "< Back").classes("back");

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

		var root = GnomeRootTag.createSimple(request.getPath(), "Audit Log - " + request.gc);
		root.content.a("/panel/" + request.gc.guildId.asString(), "< Back").classes("back");

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
		root.content.a("/panel/" + request.gc.guildId.asString(), "< Back").classes("back");

		var spamList = new ArrayList<BanEntry>();
		var hackList = new ArrayList<BanEntry>();
		var lfgList = new ArrayList<BanEntry>();
		var otherList = new ArrayList<BanEntry>();
		var noReasonList = new ArrayList<BanEntry>();
		int deletedUsers = 0;
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

			for (int i = 0; i < list1.size(); i++) {
				var entry = list1.get(i);
				var cells = table.addRow();

				cells[0].value(String.format(indexFormat, i + 1));

				var name = badNamePattern.matcher(entry.name).find() ? "(Cringe Name)" : entry.name;
				var displayName = entry.displayName.isEmpty() ? "" : badNamePattern.matcher(entry.displayName).find() ? "(Cringe Name)" : entry.displayName;

				var nameTag = cells[1].tag().a("/panel/" + request.gc.guildId.asString() + "/members/" + entry.id);

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