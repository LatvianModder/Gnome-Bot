package dev.gnomebot.app.discord;

import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.util.EntityUtil;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ChannelData;
import discord4j.discordjson.json.DMCreateRequest;
import discord4j.discordjson.json.ImmutableStartThreadRequest;
import discord4j.discordjson.json.UserData;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DM {
	public record DMChannel(long userId, long messageId, long channelId) {
		@Override
		public String toString() {
			return userId + ":" + messageId + ":" + channelId;
		}
	}

	private static final Map<Long, DMChannel> DM_CHANNELS_USER = new HashMap<>();
	private static final Map<Long, DMChannel> DM_CHANNELS_MESSAGE = new HashMap<>();

	public static void loadDmChannels() {
		DM_CHANNELS_USER.clear();
		DM_CHANNELS_MESSAGE.clear();

		if (Files.exists(AppPaths.DM_CHANNELS)) {
			try {
				for (var line : Files.readAllLines(AppPaths.DM_CHANNELS)) {
					var split = line.split(":", 3);
					var userId = SnowFlake.num(split[0].trim());
					var messageId = SnowFlake.num(split[1].trim());
					var channelId = split.length == 3 ? SnowFlake.num(split[2].trim()) : 0L;
					var dm = new DMChannel(userId, messageId, channelId);
					DM_CHANNELS_USER.put(userId, dm);
					DM_CHANNELS_MESSAGE.put(messageId, dm);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void saveDmChannels() {
		try {
			Files.write(AppPaths.DM_CHANNELS, DM_CHANNELS_USER.values().stream().map(DMChannel::toString).toList());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Nullable
	public static DMChannel getChannelFromUser(long userId) {
		return DM_CHANNELS_USER.get(userId);
	}

	@Nullable
	public static DMChannel getChannelFromMessage(long id) {
		return DM_CHANNELS_MESSAGE.get(id);
	}

	public static long openId(DiscordHandler handler, long userId) {
		var c = DM_CHANNELS_USER.get(userId);

		if (c != null && c.channelId != 0L) {
			try {
				return c.channelId;
			} catch (Exception ex) {
			}
		}

		try {
			return Objects.requireNonNull(handler.client.getRestClient().getUserService()
					.createDM(DMCreateRequest.builder().recipientId(SnowFlake.str(userId)).build())
					.map(ChannelData::id)
					.map(Id::asLong)
					.block());
		} catch (Exception ex) {
			throw new GnomeException("This command requires user's DMs to be enabled for this guild!");
		}
	}

	public static PrivateChannel open(DiscordHandler handler, long userId) {
		try {
			var c = DM_CHANNELS_USER.get(userId);

			if (c != null && c.channelId != 0L) {
				try {
					return new PrivateChannel(handler.client, handler.client.getRestClient().getChannelService().getChannel(c.channelId).block());
				} catch (Exception ex) {
				}
			}

			return Objects.requireNonNull(handler.client.getRestClient().getUserService()
					.createDM(DMCreateRequest.builder().recipientId(SnowFlake.str(userId)).build())
					.map(data -> EntityUtil.getChannel(handler.client, data))
					.cast(PrivateChannel.class)
					.block()
			);
		} catch (Exception ex) {
			throw new GnomeException("This command requires user's DMs to be enabled for this guild!");
		}
	}

	// async this
	private static void sendInDmChannel(DiscordHandler handler, @Nullable PrivateChannel privateChannel, UserData user, MessageBuilder message) {
		var dmChannelId = Config.get().gnome_dm_channel_id;

		if (dmChannelId == 0L) {
			Config.get().gnome_dm_webhook.execute(message);
		} else {
			var dmChannel = DM_CHANNELS_USER.get(user.id().asLong());
			var save = false;

			if (dmChannel == null) {
				var channelService = handler.app.discordHandler.client.getRestClient().getChannelService();
				var messageId = channelService.createMessage(dmChannelId, MessageBuilder.create(user.username() + " [" + user.id().asString() + "]").toMultipartMessageCreateRequest()).block().id().asLong();
				channelService.startThreadWithMessage(dmChannelId, messageId, ImmutableStartThreadRequest.builder().name("DMs of " + user.username()).autoArchiveDuration(1440).build()).block();
				dmChannel = new DMChannel(user.id().asLong(), messageId, 0L);
				save = true;
			}

			if (dmChannel.channelId == 0L && privateChannel != null) {
				dmChannel = new DMChannel(dmChannel.userId, dmChannel.messageId, privateChannel.getId().asLong());
				save = true;
			}

			if (save) {
				DM_CHANNELS_USER.put(dmChannel.userId, dmChannel);
				DM_CHANNELS_MESSAGE.put(dmChannel.messageId, dmChannel);
				saveDmChannels();
			}

			Config.get().gnome_dm_webhook.withThread(null, dmChannel.messageId).execute(message);
		}
	}

	public static Optional<Message> send(DiscordHandler handler, UserData user, MessageBuilder message, boolean log) {
		try {
			var m = Optional.of(open(handler, user.id().asLong()).createMessage(message.toMessageCreateSpec()).block());

			if (log && message.getContent() != null) {
				sendInDmChannel(handler, null, user, MessageBuilder.create()
						.content(message.getContent())
						.webhookName("Gnome")
						.webhookAvatarUrl(Assets.AVATAR.getPath())
				);
			}

			return m;
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	public static Optional<Message> send(DiscordHandler handler, UserData user, String content, boolean log) {
		return send(handler, user, MessageBuilder.create().content(content), log);
	}

	public static void log(DiscordHandler handler, PrivateChannel privateChannel, UserData author, Message message) {
		var builder = new StringBuilder(message.getContent());

		for (var attachment : message.getAttachments()) {
			builder.append('\n');
			builder.append(attachment.getUrl());
		}

		for (var sticker : message.getStickersItems()) {
			builder.append('\n');
			builder.append("(Sticker) ").append(sticker.getId()).append(":").append(sticker.getName());
		}

		if (builder.isEmpty()) {
			builder.append("<Empty>");
		}

		sendInDmChannel(handler, privateChannel, author, MessageBuilder.create()
				.content(builder.toString().trim())
				.webhookName(author.globalName().orElse(author.username()))
				.webhookAvatarUrl(Utils.getAvatarUrl(author))
		);
	}

	public static void reply(DiscordHandler handler, PrivateChannel privateChannel, UserData author, MessageChannel channel, String content) {
		channel.createMessage(content).block();

		sendInDmChannel(handler, privateChannel, author, MessageBuilder.create()
				.content(content)
				.webhookName("Gnome")
				.webhookAvatarUrl(Assets.AVATAR.getPath())
		);
	}
}
