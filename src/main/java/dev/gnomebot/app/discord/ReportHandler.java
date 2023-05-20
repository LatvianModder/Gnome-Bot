package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.StartThreadSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReportHandler {
	public static class Report {
		public ThreadChannel message;
		public long ttl;
		public int reports;
	}

	public static final Map<Snowflake, Report> REPORT_STACKS = new HashMap<>();

	public static void report(ComponentEventWrapper event, Snowflake channelId, Snowflake messageId, String reason) {
		if (reason.equals("-")) {
			event.edit().respond(MessageBuilder.create("It's ok, everyone makes mistakes.").noComponents());
			return;
		}

		Optional<ChannelInfo> c = event.context.gc.reportChannel.messageChannel();

		if (c.isEmpty()) {
			event.edit().respond(MessageBuilder.create("Report channel not set up!").noComponents());
			return;
		}

		ChannelInfo channel = event.context.gc.getOrMakeChannelInfo(channelId);
		Message m = channel.getMessage(messageId);

		if (m == null) {
			event.edit().respond(MessageBuilder.create("Message already deleted!").noComponents());
			return;
		}

		User author = m.getAuthor().orElse(null);

		if (author == null) {
			event.edit().respond(MessageBuilder.create("User already gone!").noComponents());
			return;
		}

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.MESSAGE_REPORT)
				.channel(m.getChannelId().asLong())
				.message(m.getId().asLong())
				.user(author)
				.content(m.getContent())
				.source(event.context.sender)
		);

		String quoteURL = QuoteHandler.getMessageURL(event.context.gc.guildId, event.context.channelInfo.id, m.getId());
		EmbedBuilder reportEmbed = EmbedBuilder.create();
		reportEmbed.color(EmbedColor.RED);
		reportEmbed.author(author.getTag(), author.getAvatarUrl(), quoteURL);
		reportEmbed.description("[Quote ➤](" + quoteURL + ") " + m.getContent());
		reportEmbed.footer("Reported by " + event.context.sender.getTag() + " [" + reason + "]", event.context.sender.getAvatarUrl());

		CachedRole role = event.context.gc.reportMentionRole.getRole();

		Report report = REPORT_STACKS.get(author.getId());
		long now = System.currentTimeMillis();

		if (report != null && now >= report.ttl) {
			report = null;
		}

		if (report == null) {
			MessageBuilder reportMessage = MessageBuilder.create();

			if (role != null) {
				reportMessage.content(role + " " + author.getTag() + " (" + author.getMention() + ") has been reported!\nReason: **" + reason + "**");
				reportMessage.allowRoleMentions(role.id);
			} else {
				reportMessage.content(author.getTag() + " (" + author.getMention() + ") has been reported!\nReason: **" + reason + "**");
			}

			reportMessage.addEmbed(reportEmbed);

			report = new Report();

			var msg = c.get().createMessage(reportMessage).block();
			report.message = msg.startThread(StartThreadSpec.builder().name(author.getUsername()).build()).block();
			report.ttl = now + 3600000L;
			REPORT_STACKS.put(author.getId(), report);
		}

		report.reports++;

		if (report.reports >= 2) {
			report.message.createMessage(reportEmbed.toEmbedCreateSpec()).block();
		} else {
			report.message.createMessage("User ID:").block();
			report.message.createMessage(author.getId().asString()).block();
		}

		if (role != null) {
			event.edit().respond(MessageBuilder.create("Message reported & " + role + " have been notified!").noComponents());
		} else {
			event.edit().respond(MessageBuilder.create("Message reported!").noComponents());
		}

		m.addReaction(Emojis.WARNING).subscribe();
	}
}
