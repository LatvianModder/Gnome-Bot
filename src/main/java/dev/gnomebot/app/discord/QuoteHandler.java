package dev.gnomebot.app.discord;

import dev.gnomebot.app.Assets;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.AttachmentType;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.UserData;
import discord4j.rest.service.ChannelService;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;

public class QuoteHandler {
	public static void getMessageURL(StringBuilder builder, Snowflake guild, Snowflake channel, Snowflake message) {
		builder.append("https://discord.com/channels/").append(guild.asString()).append('/').append(channel.asString()).append('/').append(message.asString());
	}

	public static void getChannelURL(StringBuilder builder, Snowflake guild, Snowflake channel) {
		builder.append("https://discord.com/channels/").append(guild.asString()).append('/').append(channel.asString());
	}

	public static String getMessageURL(Snowflake guild, Snowflake channel, Snowflake message) {
		StringBuilder builder = new StringBuilder();
		getMessageURL(builder, guild, channel, message);
		return builder.toString();
	}

	public static String getChannelURL(Snowflake guild, Snowflake channel) {
		StringBuilder builder = new StringBuilder();
		getChannelURL(builder, guild, channel);
		return builder.toString();
	}

	public static int handle(GuildCollections gc, Message message, ChannelInfo channel, User user) {
		Matcher quoteMatcher = MessageHandler.MESSAGE_URL_PATTERN.matcher(message.getContent());
		int quotes = 0;

		boolean mentionsAdmin = gc.adminLogChannel.isSet() && gc.adminRole.isSet() && gc.adminRole.isMentioned(message);
		ChannelService service = gc.db.app.discordHandler.client.getRestClient().getChannelService();

		while (quoteMatcher.find()) {
			String url = quoteMatcher.group();

			Snowflake qguildId = Snowflake.of(quoteMatcher.group(1));
			Snowflake qchannelId = Snowflake.of(quoteMatcher.group(2));
			Snowflake qmessageId = Snowflake.of(quoteMatcher.group(3));

			if (!mentionsAdmin && url.startsWith("<") && url.endsWith(">")) {
				continue;
			}

			MessageData m = null;

			try {
				m = service.getMessage(qchannelId.asLong(), qmessageId.asLong()).block();
			} catch (Exception ex) {
			}

			if (m == null) {
				continue;
			}

			ChannelInfo channel1 = mentionsAdmin ? gc.adminLogChannel.messageChannel().orElse(null) : channel;

			if (channel1 == null) {
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
				continue;
			}

			EmbedBuilder quoteEmbed = EmbedBuilder.create();
			String quoteURL = getMessageURL(qguildId, qchannelId, qmessageId);

			if (!thumbnailUrl.isEmpty()) {
				quoteEmbed.thumbnail(thumbnailUrl);
			}

			UserData author = m.author();
			quoteEmbed.author(author.username(), Utils.getAvatarUrl(author), quoteURL);

			quoteEmbed.color(EmbedColor.GRAY);
			quoteEmbed.description("[Quote ➤](" + quoteURL + ") " + content);
			quoteEmbed.timestamp(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(m.timestamp(), Instant::from));

			if (!user.getId().equals(gc.db.app.discordHandler.selfId)) {
				if (mentionsAdmin) {
					quoteEmbed.footer(user.getTag() + " pinged admins", user.getAvatarUrl());
				} else if (author == null || user.getId().asLong() != author.id().asLong()) {
					quoteEmbed.footer(user.getUsername() + " quoted", user.getAvatarUrl());
				}
			}

			channel1.createMessage(quoteEmbed).subscribe();

			quotes++;

			if (quotes >= 3) {
				break;
			}
		}

		if (quotes > 0) {
			Matcher m = MessageHandler.MESSAGE_URL_PATTERN.matcher(message.getContent());

			if (m.matches()) {
				String g = m.group(0);

				if (!g.startsWith("<") && !g.endsWith(">")) {
					return -1;
				}
			}
		}

		return quotes;
	}
}
