package dev.gnomebot.app.discord.command.admin;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.ExportedMessage;
import dev.gnomebot.app.data.complex.ComplexMessage;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.handler.PasteHandlers;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.tinyserver.http.response.HTTPStatus;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import javax.imageio.ImageIO;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DisplayCommands extends ApplicationCommands {
	public static void members(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		List<Pair<String, String>> list = new ArrayList<>();
		Predicate<Member> predicate = member -> true;
		var length = 0;

		if (event.has("name-regex")) {
			var pattern = Pattern.compile(event.get("name-regex").asString(".*"), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			predicate = predicate.and(m -> m.getNickname().isPresent() ? (pattern.matcher(m.getUsername()).find() || pattern.matcher(m.getNickname().get()).find()) : pattern.matcher(m.getUsername()).find());
		}

		if (event.has("role")) {
			var role = event.get("role").asRole().get().id;
			predicate = predicate.and(m -> m.getRoleIds().contains(SnowFlake.convert(role)));
		}

		for (var member : event.context.gc.getGuild().getMembers().filter(predicate).toIterable(5)) {
			var s = member.getMention() + "(" + member.getTag() + ")";
			list.add(Pair.of(s, member.getDisplayName()));
			length += s.length() + 1;

			if (length >= 2000) {
				list.addFirst(Pair.of("More results! Refine your filter!", ""));
				break;
			}
		}

		if (list.isEmpty()) {
			list.add(Pair.of("404", ""));
		}

		event.respond(list.stream().sorted((o1, o2) -> o1.b().compareToIgnoreCase(o2.b())).map(Pair::a).collect(Collectors.toList()));
	}

	public static void messages(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		List<String> list = new ArrayList<>();
		var length = 0;

		var activity = event.get("activity").asBoolean(false);

		if (!event.get("recently-deleted").asBoolean(false)) {
			var messages = event.context.gc.messages.query().descending("timestamp").filter(Filters.gte("timestamp", new Date(System.currentTimeMillis() - 15778476000L))).limit(100);

			var contentRegex = event.get("content-regex").asString();

			if (!contentRegex.isEmpty()) {
				messages.regex("content", Pattern.compile(contentRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
			}

			if (event.has("member")) {
				messages.eq("user", event.get("member").asLong());
			}

			if (event.has("flags")) {
				messages.filter(Filters.bitsAnySet("flags", event.get("flags").asInt()));
			}

			var count = messages.count();

			for (var message : messages) {
				var s = message.getURLAsArrow(event.context.gc) + " <@" + SnowFlake.str(message.getUserID()) + "> " + message.getContent();
				list.add(s);
				length += s.length() + 1;

				if (length >= 2000) {
					list.addFirst("More results! Refine your filter!");
					break;
				}
			}

			list.addFirst("Found " + count + " messages");
		}

		if (activity) {
			event.respond(MessageBuilder.create("Activity:").addFile("activity.csv", String.join("\n", list).getBytes(StandardCharsets.UTF_8)));
		} else {
			if (list.isEmpty()) {
				list.add("404");
			}

			event.respond(list);
		}
	}

	public static void quietMemberCount(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		var total = event.context.gc.getGuild().getMembers().count().block().intValue();
		event.context.gc.getGuild()
				.getMembers()
				.filter(m -> {
					var dm = event.context.gc.members.findFirst(m);
					return dm != null && dm.getTotalMessages() <= 0;
				})
				.count()
				.subscribe(count -> event.respond(count + " / " + total + " quiet people [" + (int) (count * 100D / (double) total) + "%]"));
	}

	public static void messageHistoryExport(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		var c = DM.open(event.context.gc.db.app.discordHandler, event.context.sender.getId().asLong());

		var memberId = event.get("member").asMember().orElse(event.context.sender).getId();

		if (!memberId.equals(event.context.sender.getId())) {
			event.context.checkSenderAdmin();
		}

		Message m;

		try {
			m = Objects.requireNonNull(c.createMessage("Gathering messages...").block());
		} catch (Exception ex) {
			throw new GnomeException("This command requires DMs to be enabled for this guild!");
		}

		var list = new LinkedList<ExportedMessage>();

		var channelPattern = Pattern.compile("<#(\\d+)>");
		Map<Long, String> channelNameMap = new HashMap<>();

		Function<Long, String> computeFunc = event.context.gc::getChannelDisplayName;
		Function<MatchResult, String> channelNameReplacer = matchResult -> channelNameMap.computeIfAbsent(Long.parseUnsignedLong(matchResult.group(1)), computeFunc);

		for (var msg : event.context.gc.messages.query().eq("user", memberId.asLong())) {
			var emessage = new ExportedMessage();
			emessage.timestamp = msg.getDate().getTime();
			emessage.channel = msg.getChannelID();
			emessage.channelName = channelNameMap.computeIfAbsent(emessage.channel, computeFunc);
			emessage.flags = msg.flags;
			emessage.content = channelPattern.matcher(msg.getContent()).replaceAll(channelNameReplacer);
			list.add(emessage);
		}

		m.edit(MessageBuilder.create("Done gathering messages! Saving to file...").toMessageEditSpec()).block();
		list.sort(ExportedMessage.COMPARATOR);

		var out = new ByteArrayOutputStream();
		var row = new String[6];
		var sb = new StringBuilder();

		var encoder = StandardCharsets.UTF_8.newEncoder();
		try (var writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
			for (var i = 0; i < list.size(); i++) {
				list.get(i).toString(i, row);

				for (var j = 0; j < row.length; j++) {
					if (j != 0) {
						sb.append(',');
					}

					sb.append(row[j]);
				}

				writer.append(sb);
				sb.setLength(0);
				writer.newLine();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		m.edit(MessageBuilder.create("Done!").toMessageEditSpec()).subscribe();

		c.createMessage(MessageBuilder.create().addFile(event.context.gc.guildId + "-" + memberId.asString() + "-" + Instant.now() + ".csv", out.toByteArray()).toMessageCreateSpec()).flatMap(dm -> {
			var attachment = dm.getAttachments().getFirst();
			return dm.edit(MessageBuilder.create().addComponent(ActionRow.of(Button.link(PasteHandlers.getUrl(event.app, dm.getChannelId().asLong(), dm.getId().asLong(), attachment.getId().asLong()), "View " + attachment.getFilename()))).toMessageEditSpec());
		}).block();

		event.respond("Done! Check your DMs!");
	}

	public static void messageCountPerMonth(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		var channelInfo = event.get("channel").asChannelInfo().orElse(null);

		if (channelInfo == null) {
			var messages = event.context.gc.messages.count();
			var months = (Instant.now().getEpochSecond() - SnowFlake.timestamp(event.context.gc.guildId) / 1000L) / 2592000D;
			event.respond(messages + " messages / " + months + " months = " + (messages / months) + " messages per month");
		} else {
			var messages = event.context.gc.messages.count(Filters.eq("channel", channelInfo.id));
			var months = (Instant.now().getEpochSecond() - SnowFlake.timestamp(channelInfo.id) / 1000L) / 2592000D;
			event.respond(messages + " messages / " + months + " months = " + (messages / months) + " messages per month");
		}
	}

	public static void adminRoles(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.respond("Admin roles:\n\n" + event.context.gc.getRoleList().stream().filter(r -> r.adminRole).map(r -> "<@&" + r.id + ">").collect(Collectors.joining("\n")));
	}

	public static void hourlyActivity(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		var member = event.get("member").asMember().orElse(event.context.sender);

		if (!member.equals(event.context.sender)) {
			event.context.checkSenderAdmin();
		}

		var ms = event.get("days").asDays().orElse(30L) * 1000L * 60L * 60L * 24L;
		var zoneId = event.get("timezone").asZone();

		var activity = new long[24];
		var total = 0L;
		Log.info("Starting... " + member.getTag());

		for (var m : event.context.gc.messages.query().eq("user", member.getId().asLong()).filter(ms == 0L ? null : Filters.gt("timestamp", new Date(System.currentTimeMillis() - ms)))) {
			activity[m.getDate().toInstant().atZone(zoneId).getHour()]++;
			total++;
		}

		Log.info("Stopped!");

		if (total == 0L) {
			event.respond("No messages found!");
			return;
		}

		var sb = new StringBuilder("Activity [" + total + " messages]: (" + zoneId + ")");

		for (var i = 0; i < 24; i++) {
			sb.append('\n');

			if (i < 10) {
				sb.append('0');
			}

			sb.append(i);
			sb.append(":00 - ");
			sb.append(activity[i]);
			sb.append(" [");
			sb.append(activity[i] * 100L / total);
			sb.append("%]");
		}

		event.respond(sb.toString());
	}

	public static void memberCount(ChatInputInteractionEventWrapper event) {
		event.acknowledge();
		var role = event.get("role").asRole();

		if (role.isPresent()) {
			var wr = role.get();
			long count = event.context.gc.getGuild()
					.getMembers()
					.filter(member -> member.getRoleIds().contains(SnowFlake.convert(wr.id)))
					.count()
					.block();

			event.respond(FormattingUtils.format(count) + " members with role " + wr);
		} else {
			var max = event.context.gc.getGuild().getMaxMembers().orElse(0);

			long count = event.context.gc.getGuild()
					.getMembers()
					.count()
					.block();

			event.respond(FormattingUtils.format(count) + " / " + (max == 0 ? "?" : FormattingUtils.format(max)) + " members");
		}
	}

	public static void userMentionLeaderboard(ChatInputInteractionEventWrapper event) throws Exception {
		mentionLeaderboard(event, true);
	}

	public static void roleMentionLeaderboard(ChatInputInteractionEventWrapper event) throws Exception {
		mentionLeaderboard(event, false);
	}

	private static void mentionLeaderboard(ChatInputInteractionEventWrapper event, boolean isUser) throws Exception {
		event.acknowledge();
		event.context.checkSenderAdmin();

		var mentionId = (isUser ? event.get("mention").asUser().map(User::getId) : event.get("mention").asRole().map(m -> m.id)).orElse(null);

		if (mentionId == null) {
			throw new GnomeException("Mention not found!");
		}

		var limit = Math.max(1L, Math.min(event.get("limit").asLong(20L), 10000L));

		var days = event.get("timespan").asDays().orElse(90L);
		var channelInfo = event.get("channel").asChannelInfo().orElse(null);
		var role = event.get("role").asRole().orElse(null);

		var url = "api/guilds/" + event.context.gc.guildId + "/activity/" + (isUser ? "user" : "role") + "-mention-leaderboard-image/" + mentionId + "/" + days + "?limit=" + limit;

		if (channelInfo != null) {
			url += "&channel=" + channelInfo.id;
		}

		if (role != null) {
			url += "&role=" + role.id;
		}

		var req = Utils.internalRequest(event.app, url).timeout(30000).toImage();

		try {
			var imageData = new ByteArrayOutputStream();
			ImageIO.write(req.block(), "PNG", imageData);
			event.respond(MessageBuilder.create().addFile("leaderboard.png", imageData.toByteArray()));
		} catch (URLRequest.UnsuccesfulRequestException ex) {
			if (ex.status == HTTPStatus.BAD_REQUEST) {
				event.respond("This leaderboard has no data!");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			event.respond(req.getFullUrl());
		}
	}

	public static void debugComplexMessage(Message message, ComponentEventWrapper event) {
		event.context.checkSenderAdmin();
		var str0 = String.join("\n", ComplexMessage.of(event.context.gc, message).getLines());
		var str = "```\n" + str0 + "\n```";

		if (str.length() > 2000) {
			event.respond(MessageBuilder.create("String was too long, so here's file instead").addFile("debug.txt", str0.getBytes(StandardCharsets.UTF_8)));
		} else {
			event.respond(str);
		}
	}
}