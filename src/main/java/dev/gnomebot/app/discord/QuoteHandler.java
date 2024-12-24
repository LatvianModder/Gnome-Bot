package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.channel.ChannelInfo;
import dev.gnomebot.app.util.AttachmentType;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.service.ChannelService;

public class QuoteHandler {
	public static void getMessageURL(StringBuilder builder, long guild, long channel, long message) {
		builder.append("https://discord.com/channels/").append(SnowFlake.str(guild)).append('/').append(SnowFlake.str(channel)).append('/').append(SnowFlake.str(message));
	}

	public static void getChannelURL(StringBuilder builder, long guild, long channel) {
		builder.append("https://discord.com/channels/").append(SnowFlake.str(guild)).append('/').append(SnowFlake.str(channel));
	}

	public static String getMessageURL(long guild, long channel, long message) {
		var builder = new StringBuilder();
		getMessageURL(builder, guild, channel, message);
		return builder.toString();
	}

	public static String getChannelURL(long guild, long channel) {
		var builder = new StringBuilder();
		getChannelURL(builder, guild, channel);
		return builder.toString();
	}

	public static int handle(App app, GuildCollections gc, Message message, ChannelInfo channel, Member member) {
		if (member.isBot() && member.getId().asLong() != gc.db.app.discordHandler.selfId) {
			return 0;
		}

		var quoteMatcher = MessageHandler.MESSAGE_URL_PATTERN.matcher(message.getContent());
		var quotes = 0;

		var init = true;

		var mentionsAdmin = false;
		ChannelService service = null;
		ChannelInfo channel1 = null;
		WebHookDestination webHook = null;

		var messageBuilder = MessageBuilder.create();
		var remaining = new StringBuilder();

		while (quoteMatcher.find()) {
			var url = quoteMatcher.group();
			var qguildId = SnowFlake.num(quoteMatcher.group(1));
			var qchannelId = SnowFlake.num(quoteMatcher.group(2));
			var qmessageId = SnowFlake.num(quoteMatcher.group(3));

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
				m = service.getMessage(qchannelId, qmessageId).block();
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
					thumbnailUrl = "https://gnomebot.dev/api/info/video-thumbnail/" + SnowFlake.str(qchannelId) + "/" + SnowFlake.str(qmessageId) + "/" + a.id().asString();
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
						thumbnailUrl = Assets.VIDEO.getPath(app);
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
			var authorMember = member.getId().asLong() == author.id().asLong() ? member : gc.getMember(author.id().asLong());

			if (authorMember != null) {
				quoteEmbed.author("➤ " + authorMember.getDisplayName(), authorMember.getAvatarUrl(), quoteURL);
			} else {
				quoteEmbed.author("➤ " + author.globalName().orElse(author.username()), Utils.getAvatarURL(author) + "?size=32", quoteURL);
			}

			quoteEmbed.color(EmbedColor.GRAY);
			quoteEmbed.description("[➤](" + quoteURL + ')' + (content.indexOf('\n') != -1 ? '\n' : ' ') + content);
			// quoteEmbed.description(content);
			// quoteEmbed.timestamp(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(m.timestamp(), Instant::from));

			if (webHook == null && member.getId().asLong() != gc.db.app.discordHandler.selfId) {
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
