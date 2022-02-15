package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.ThreadMessageRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.util.AllowedMentions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReportHandler {
	public static class Report {
		public Snowflake messageId;
		public long ttl;
		public int reports;
	}

	public static final Map<Snowflake, Report> REPORT_STACKS = new HashMap<>();

	public static void report(ComponentEventWrapper event, Snowflake channelId, Snowflake messageId, String reason) {
		if (reason.equals("-")) {
			event.edit(2).respond("It's ok, everyone makes mistakes.");
			return;
		}

		Optional<ChannelInfo> c = event.context.gc.reportChannel.messageChannel();

		if (c.isEmpty()) {
			event.edit(2).respond("Report channel not set up!");
			return;
		}

		ChannelInfo channel = event.context.gc.getOrMakeChannelInfo(channelId);
		Message m = channel.getMessage(messageId);

		if (m == null) {
			event.edit(2).respond("Message already deleted!");
			return;
		}

		User author = m.getAuthor().orElse(null);

		if (author == null) {
			event.edit(2).respond("User already gone!");
			return;
		}

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.MESSAGE_REPORT)
				.channel(m.getChannelId())
				.message(m.getId())
				.user(author)
				.content(m.getContent())
				.source(event.context.sender)
		);

		String quoteURL = QuoteHandler.getMessageURL(event.context.gc.guildId, event.context.channelInfo.id, m.getId());
		EmbedBuilder reportEmbed = EmbedBuilder.create();
		reportEmbed.color(EmbedColors.RED);
		reportEmbed.author(author.getTag(), quoteURL, author.getAvatarUrl());
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
				reportMessage.allowedMentions(AllowedMentions.builder().allowRole(role.id).build());
			} else {
				reportMessage.content(author.getTag() + " (" + author.getMention() + ") has been reported!\nReason: **" + reason + "**");
				reportMessage.allowedMentions(DiscordMessage.noMentions());
			}

			reportMessage.addEmbed(reportEmbed);

			report = new Report();
			report.messageId = c.get().createMessage(reportMessage).block().getId();
			report.ttl = now + 3600000L;

			Utils.THREAD_ROUTE.newRequest(c.get().id.asLong(), report.messageId.asLong())
					.body(new ThreadMessageRequest(author.getUsername()))
					.exchange(event.context.gc.getClient().getCoreResources().getRouter())
					.skipBody()
					.block();

			REPORT_STACKS.put(author.getId(), report);
		}

		report.reports++;

		ChannelInfo thread = event.context.gc.getOrMakeChannelInfo(report.messageId);

		if (report.reports >= 2) {
			thread.createMessage(reportEmbed).block();
		} else {
			thread.createMessage("User ID:").block();
			thread.createMessage(author.getId().asString()).block();
		}

		if (role != null) {
			event.edit(2).respond("Message reported & " + role + " have been notified!");
		} else {
			event.edit(2).respond("Message reported!");
		}

		m.addReaction(Emojis.WARNING).subscribe();
	}
}
