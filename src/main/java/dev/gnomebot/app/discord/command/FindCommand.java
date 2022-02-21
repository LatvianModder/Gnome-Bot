package dev.gnomebot.app.discord.command;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.CollectionQuery;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.ExportedMessage;
import dev.gnomebot.app.data.Paste;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.Pair;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.CharsetEncoder;
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

/**
 * @author LatvianModder
 */
public class FindCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("find")
			.description("Finds messages, users, etc.")
			.add(sub("members")
					.add(string("name_regex"))
					.add(role("role"))
					.run(FindCommand::members)
			)
			.add(sub("messages")
					.add(string("content_regex"))
					.add(user("member"))
					.add(integer("flags"))
					.add(bool("recently_deleted"))
					.run(FindCommand::messages)
			)
			.add(sub("quiet_member_count")
					.run(FindCommand::quietMemberCount)
			)
			.add(sub("message_history_export")
					.add(user("member"))
					.run(FindCommand::messageHistoryExport)
			)
			.add(sub("message_count_per_month")
					.add(channel("channel"))
					.run(FindCommand::messageCountPerMonth)
			)
			.add(sub("admin_roles")
					.run(FindCommand::adminRoles)
			);

	private static void members(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		List<Pair<String, String>> list = new ArrayList<>();
		Predicate<Member> predicate = member -> true;
		int length = 0;

		if (event.has("name_regex")) {
			Pattern pattern = Pattern.compile(event.get("name_regex").asString(".*"), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			predicate = predicate.and(m -> m.getNickname().isPresent() ? (pattern.matcher(m.getUsername()).find() || pattern.matcher(m.getNickname().get()).find()) : pattern.matcher(m.getUsername()).find());
		}

		if (event.has("role")) {
			Snowflake role = event.get("role").asRole().get().id;
			predicate = predicate.and(m -> m.getRoleIds().contains(role));
		}

		for (Member member : event.context.gc.getGuild().getMembers().filter(predicate).toIterable(5)) {
			String s = member.getMention() + "(" + member.getTag() + ")";
			list.add(Pair.of(s, member.getDisplayName()));
			length += s.length() + 1;

			if (length >= 2000) {
				list.add(0, Pair.of("More results! Refine your filter!", ""));
				break;
			}
		}

		if (list.isEmpty()) {
			list.add(Pair.of("404", ""));
		}

		event.respond(list.stream().sorted((o1, o2) -> o1.b.compareToIgnoreCase(o2.b)).map(Pair::getA).collect(Collectors.toList()));
	}

	private static void messages(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		List<String> list = new ArrayList<>();
		int length = 0;

		if (!event.get("recently_deleted").asBoolean(false)) {
			CollectionQuery<DiscordMessage> messages = event.context.gc.messages.query().descending("timestamp").filter(Filters.gte("timestamp", new Date(System.currentTimeMillis() - 15778476000L))).limit(100);

			String contentRegex = event.get("content_regex").asString();

			if (!contentRegex.isEmpty()) {
				messages.regex("content", Pattern.compile(contentRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
			}

			if (event.has("member")) {
				messages.eq("user", event.get("member").asLong());
			}

			if (event.has("flags")) {
				messages.filter(Filters.bitsAnySet("flags", event.get("flags").asInt()));
			}

			for (DiscordMessage message : messages) {
				String s = message.getURLAsArrow(event.context.gc) + " <@" + Snowflake.of(message.getUserID()).asString() + "> " + message.getContent();
				list.add(s);
				length += s.length() + 1;

				if (length >= 2000) {
					list.add(0, "More results! Refine your filter!");
					break;
				}
			}
		}

		if (list.isEmpty()) {
			list.add("404");
		}

		event.respond(list);
	}

	private static void quietMemberCount(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		int total = event.context.gc.getGuild().getMembers().count().block().intValue();
		event.context.gc.getGuild()
				.getMembers()
				.filter(m -> {
					DiscordMember dm = event.context.gc.members.findFirst(m);
					return dm != null && dm.getTotalMessages() <= 0;
				})
				.count()
				.subscribe(count -> event.respond(count + " / " + total + " quiet people [" + (int) (count * 100D / (double) total) + "%]"));
	}

	private static void messageHistoryExport(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		PrivateChannel c = DM.open(event.context.sender);

		Snowflake memberId = event.get("member").asMember().orElse(event.context.sender).getId();

		if (!memberId.equals(event.context.sender.getId())) {
			event.context.checkSenderAdmin();
		}

		Message m;

		try {
			m = Objects.requireNonNull(c.createMessage("Gathering messages...").block());
		} catch (Exception ex) {
			throw new GnomeException("This command requires DMs to be enabled for this guild!");
		}

		LinkedList<ExportedMessage> list = new LinkedList<>();

		Pattern channelPattern = Pattern.compile("<#(\\d+)>");
		Map<Long, String> channelNameMap = new HashMap<>() {
			@Override
			public String get(Object key) {
				String s = super.get(key);

				if (s == null) {
					s = "#" + key.toString();

					try {
						s = event.context.gc.getChannelName(Snowflake.of((Long) key));
					} catch (Exception ex) {
					}

					super.put((Long) key, s);
				}

				return s;
			}
		};

		Function<MatchResult, String> channelNameReplacer = matchResult -> channelNameMap.get(Long.parseUnsignedLong(matchResult.group(1)));

		for (DiscordMessage msg : event.context.gc.messages.query().eq("user", memberId.asLong())) {
			ExportedMessage emessage = new ExportedMessage();
			emessage.timestamp = msg.getDate().getTime();
			emessage.channel = msg.getChannelID();
			emessage.channelName = channelNameMap.get(emessage.channel);
			emessage.flags = msg.flags;
			emessage.content = channelPattern.matcher(msg.getContent()).replaceAll(channelNameReplacer);
			list.add(emessage);
		}

		m.edit(MessageEditSpec.builder().contentOrNull("Done gathering messages! Saving to file...").build()).block();
		list.sort(ExportedMessage.COMPARATOR);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String[] row = new String[6];
		StringBuilder sb = new StringBuilder();

		CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
			for (int i = 0; i < list.size(); i++) {
				list.get(i).toString(i, row);

				for (int j = 0; j < row.length; j++) {
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

		m.edit(MessageEditSpec.builder().contentOrNull("Done!").build()).subscribe();

		c.createMessage(MessageCreateSpec.builder().addFile(event.context.gc.guildId.asString() + "-" + memberId.asString() + "-" + Instant.now() + ".csv", new ByteArrayInputStream(out.toByteArray())).build()).flatMap(dm -> {
			Attachment attachment = dm.getAttachments().get(0);
			Paste.createPaste(event.context.gc.db, dm.getChannelId().asLong(), attachment.getId().asLong(), attachment.getFilename(), event.context.sender.getUsername());
			return dm.edit(MessageEditSpec.builder().addComponent(ActionRow.of(Button.link(Paste.getUrl(attachment.getId().asString()), "View " + attachment.getFilename()))).build());
		}).block();

		event.respond("Done! Check your DMs!");
	}

	private static void messageCountPerMonth(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		ChannelInfo channelInfo = event.get("channel").asChannelInfo().orElse(null);

		if (channelInfo == null) {
			long messages = event.context.gc.messages.count();
			double months = (Instant.now().getEpochSecond() - event.context.gc.guildId.getTimestamp().getEpochSecond()) / 2592000D;
			event.respond(messages + " messages / " + months + " months = " + ((long) (messages / months)) + " messages per month");
		} else {
			long messages = event.context.gc.messages.count(Filters.eq("channel", channelInfo.id.asLong()));
			double months = (Instant.now().getEpochSecond() - channelInfo.id.getTimestamp().getEpochSecond()) / 2592000D;
			event.respond(messages + " messages / " + months + " months = " + ((long) (messages / months)) + " messages per month");
		}
	}

	private static void adminRoles(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		event.respond("Admin roles:\n\n" + event.context.gc.getRoleList().stream().filter(r -> r.adminRole).map(r -> "<@&" + r.id.asString() + ">").collect(Collectors.joining("\n")));
	}
}