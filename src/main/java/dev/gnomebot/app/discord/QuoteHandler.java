package dev.gnomebot.app.discord;

import dev.gnomebot.app.Assets;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.AttachmentType;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.service.ChannelService;

public class QuoteHandler {
	public static void getMessageURL(StringBuilder builder, Snowflake guild, Snowflake channel, Snowflake message) {
		builder.append("https://discord.com/channels/").append(guild.asString()).append('/').append(channel.asString()).append('/').append(message.asString());
	}

	public static void getChannelURL(StringBuilder builder, Snowflake guild, Snowflake channel) {
		builder.append("https://discord.com/channels/").append(guild.asString()).append('/').append(channel.asString());
	}

	public static String getMessageURL(Snowflake guild, Snowflake channel, Snowflake message) {
		var builder = new StringBuilder();
		getMessageURL(builder, guild, channel, message);
		return builder.toString();
	}

	public static String getChannelURL(Snowflake guild, Snowflake channel) {
		var builder = new StringBuilder();
		getChannelURL(builder, guild, channel);
		return builder.toString();
	}

	public static int handle(GuildCollections gc, Message message, ChannelInfo channel, Member member) {
		var quoteMatcher = MessageHandler.MESSAGE_URL_PATTERN.matcher(message.getContent());
		var quotes = 0;

		var init = true;

		var mentionsAdmin = false;
		ChannelService service = null;
		ChannelInfo channel1 = null;
		WebHook webHook = null;

		var messageBuilder = MessageBuilder.create();
		var remaining = new StringBuilder();

		while (quoteMatcher.find()) {
			var url = quoteMatcher.group();
			var qguildId = Utils.snowflake(quoteMatcher.group(1));
			var qchannelId = Utils.snowflake(quoteMatcher.group(2));
			var qmessageId = Utils.snowflake(quoteMatcher.group(3));

			if (init) {
				mentionsAdmin = gc.adminLogChannel.isSet() && gc.adminRole.isSet() && gc.adminRole.isMentioned(message);
				service = gc.db.app.discordHandler.client.getRestClient().getChannelService();
				channel1 = mentionsAdmin ? gc.adminLogChannel.messageChannel().orElse(null) : channel;

				if (channel1 == null) {
					return 0;
				}

				webHook = member.isBot() ? null : channel1.getWebHook().orElse(null);

				if (webHook != null) {
					messageBuilder.webhookAvatarUrl(member.getAvatarUrl());
					messageBuilder.webhookName(member.getDisplayName() + " quoted:");
				}

				init = false;
			}

			if (!mentionsAdmin && url.startsWith("<") && url.endsWith(">")) {
				quoteMatcher.appendReplacement(remaining, ".");
				continue;
			}

			MessageData m = null;

			try {
				m = service.getMessage(qchannelId.asLong(), qmessageId.asLong()).block();
			} catch (Exception ignore) {
			}

			if (m == null) {
				quoteMatcher.appendReplacement(remaining, ".");
				continue;
			}

			var content = AttachmentType.FULL_IMAGE_PATTERN.matcher(AttachmentType.FULL_VIDEO_PATTERN.matcher(m.content()).replaceAll("")).replaceAll("").trim();
			var thumbnailUrl = "";

			for (var a : m.attachments()) {
				if (!a.filename().startsWith(Attachment.SPOILER_PREFIX) && AttachmentType.get(a.filename(), a.contentType().toOptional().orElse("")) == AttachmentType.VIDEO) {
					thumbnailUrl = "https://gnomebot.dev/api/info/video-thumbnail/" + qchannelId.asString() + "/" + qmessageId.asString() + "/" + a.id().asString();
					break;
				}
			}

			if (!thumbnailUrl.isEmpty()) {
				for (var a : m.attachments()) {
					if (!a.filename().startsWith(Attachment.SPOILER_PREFIX) && AttachmentType.get(a.filename(), a.contentType().toOptional().orElse("")) == AttachmentType.IMAGE) {
						thumbnailUrl = a.proxyUrl();
						break;
					}
				}
			}

			if (thumbnailUrl.isEmpty()) {
				for (var embed : m.embeds()) {
					if (!embed.image().isAbsent()) {
						thumbnailUrl = embed.image().get().proxyUrl().toOptional().orElse("");
						break;
					}
				}
			}

			if (thumbnailUrl.isEmpty()) {
				for (var embed : m.embeds()) {
					if (!embed.video().isAbsent()) {
						thumbnailUrl = Assets.VIDEO.getPath();
						break;
					}
				}
			}

			if (content.isEmpty() && thumbnailUrl.isEmpty()) {
				quoteMatcher.appendReplacement(remaining, ".");
				continue;
			}

			quoteMatcher.appendReplacement(remaining, "");

			var quoteEmbed = EmbedBuilder.create();
			var quoteURL = getMessageURL(qguildId, qchannelId, qmessageId);

			if (!thumbnailUrl.isEmpty()) {
				quoteEmbed.thumbnail(thumbnailUrl);
			}

			var author = m.author();
			var authorMember = member.getId().asLong() == author.id().asLong() ? member : gc.getMember(Snowflake.of(author.id().asLong()));

			if (authorMember != null) {
				quoteEmbed.author("➤ " + authorMember.getDisplayName(), authorMember.getAvatarUrl(), quoteURL);
			} else {
				quoteEmbed.author("➤ " + author.globalName().orElse(author.username()), Utils.getAvatarUrl(author), quoteURL);
			}

			quoteEmbed.color(EmbedColor.GRAY);
			//quoteEmbed.description("[Quote ➤](" + quoteURL + ')' + (content.indexOf('\n') != -1 ? '\n' : ' ') + content);
			quoteEmbed.description(content);
			// quoteEmbed.timestamp(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(m.timestamp(), Instant::from));

			if (webHook == null && !member.getId().equals(gc.db.app.discordHandler.selfId)) {
				if (mentionsAdmin) {
					quoteEmbed.footer(member.getTag() + " pinged admins", member.getAvatarUrl());
				} else if (authorMember != member) {
					quoteEmbed.footer(member.getDisplayName() + " quoted", member.getAvatarUrl());
				}
			}

			messageBuilder.addEmbed(quoteEmbed);

			quotes++;

			if (quotes >= 5) {
				break;
			}
		}

		if (quotes > 0) {
			if (webHook == null) {
				channel1.createMessage(messageBuilder).subscribe();
			} else {
				webHook.execute(messageBuilder);
			}

			quoteMatcher.appendTail(remaining);
			var remainingStr = remaining.toString().trim();

			if (remainingStr.isEmpty()) {
				return -1;
			}
		}

		return quotes;
	}
}
