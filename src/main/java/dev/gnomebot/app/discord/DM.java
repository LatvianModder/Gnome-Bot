package dev.gnomebot.app.discord;

import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Sticker;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ImmutableStartThreadRequest;
import discord4j.rest.service.ChannelService;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DM {
	// TODO: Save this map
	private static final Map<Snowflake, Snowflake> DM_CHANNELS = new HashMap<>();
	private static final Map<Snowflake, Snowflake> DM_CHANNELS_REVERSE = new HashMap<>();

	public static void loadDmChannels() {
		DM_CHANNELS.clear();
		DM_CHANNELS_REVERSE.clear();

		if (Files.exists(AppPaths.DATA_DM_CHANNELS)) {
			try {
				for (String line : Files.readAllLines(AppPaths.DATA_DM_CHANNELS)) {
					String[] split = line.split(": ", 2);
					Snowflake userId = Snowflake.of(split[0]);
					Snowflake dmChannelId = Snowflake.of(split[1]);
					DM_CHANNELS.put(userId, dmChannelId);
					DM_CHANNELS_REVERSE.put(dmChannelId, userId);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void saveDmChannels() {
		try {
			Files.write(AppPaths.DATA_DM_CHANNELS, DM_CHANNELS.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey().asString() + ": " + e.getValue().asString()).toList());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Nullable
	public static Snowflake getUserFromDmChannel(Snowflake id) {
		return DM_CHANNELS_REVERSE.get(id);
	}

	public static PrivateChannel open(User user) {
		try {
			return Objects.requireNonNull(user.getPrivateChannel().block());
		} catch (Exception ex) {
			throw new GnomeException("This command requires DMs to be enabled for this guild!");
		}
	}

	// async this
	private static void sendInDmChannel(DiscordHandler handler, User user, MessageBuilder message) {
		long dmChannelId = Config.get().gnome_dm_channel_id.asLong();

		if (dmChannelId == 0L) {
			Config.get().gnome_dm_webhook.execute(message);
		} else {
			Snowflake dmChannel = DM_CHANNELS.get(user.getId());

			if (dmChannel == null) {
				ChannelService channelService = handler.app.discordHandler.client.getRestClient().getChannelService();
				Snowflake messageId = Snowflake.of(channelService.createMessage(dmChannelId, MessageCreateSpec.builder().content(user.getUsername() + " [" + user.getId().asString() + "]").build().asRequest()).block().id());
				channelService.startThreadWithMessage(dmChannelId, messageId.asLong(), ImmutableStartThreadRequest.builder().name("DMs of " + user.getUsername()).autoArchiveDuration(1440).build()).block();
				DM_CHANNELS.put(user.getId(), messageId);
				DM_CHANNELS_REVERSE.put(messageId, user.getId());
				dmChannel = messageId;
				saveDmChannels();
			}

			Config.get().gnome_dm_webhook.withThread(dmChannel.asString()).execute(message);
		}
	}

	public static Optional<Message> send(DiscordHandler handler, User user, MessageBuilder message, boolean log) {
		try {
			Optional<Message> m = Optional.of(open(user).createMessage(message.toMessageCreateSpec()).block());

			if (log && message.getContent() != null) {
				sendInDmChannel(handler, user, MessageBuilder.create()
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

	public static Optional<Message> send(DiscordHandler handler, User user, String content, boolean log) {
		return send(handler, user, MessageBuilder.create().content(content), log);
	}

	public static void log(DiscordHandler handler, User author, Message message) {
		StringBuilder builder = new StringBuilder(message.getContent());

		for (Attachment attachment : message.getAttachments()) {
			builder.append('\n');
			builder.append(attachment.getUrl());
		}

		for (Sticker sticker : message.getStickers()) {
			builder.append('\n');
			builder.append("(Sticker) ").append(sticker.getId()).append(":").append(sticker.getName());
		}

		if (builder.isEmpty()) {
			builder.append("<Empty>");
		}

		sendInDmChannel(handler, author, MessageBuilder.create()
				.content(builder.toString().trim())
				.webhookName(author.getUsername())
				.webhookAvatarUrl(author.getAvatarUrl())
		);
	}

	public static void reply(DiscordHandler handler, User author, MessageChannel channel, String content) {
		channel.createMessage(content).block();

		sendInDmChannel(handler, author, MessageBuilder.create()
				.content(content)
				.webhookName("Gnome")
				.webhookAvatarUrl(Assets.AVATAR.getPath())
		);
	}
}
