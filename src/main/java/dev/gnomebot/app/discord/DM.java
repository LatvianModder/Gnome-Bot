package dev.gnomebot.app.discord;

import dev.gnomebot.app.Assets;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Sticker;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.WebhookExecuteRequest;

import java.util.Objects;
import java.util.Optional;

public class DM {
	public static PrivateChannel open(User user) throws DiscordCommandException {
		try {
			return Objects.requireNonNull(user.getPrivateChannel().block());
		} catch (Exception ex) {
			throw new DiscordCommandException("This command requires DMs to be enabled for this guild!");
		}
	}

	public static Optional<Message> send(DiscordHandler handler, User user, MessageCreateSpec spec, boolean log) {
		try {
			Optional<Message> m = Optional.of(open(user).createMessage(spec).block());

			if (log && !spec.content().isAbsent()) {
				handler.app.config.gnome_dm_webhook.execute(WebhookExecuteRequest.builder()
						.content(spec.content())
						.avatarUrl(Assets.AVATAR.getPath())
						.username("Gnome [" + user.getId().asString() + "]")
						.allowedMentions(DiscordMessage.noMentions().toData())
						.build()
				);
			}

			return m;
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	public static Optional<Message> send(DiscordHandler handler, User user, String content, boolean log) {
		return send(handler, user, MessageCreateSpec.builder()
				.content(Utils.trimContent(content))
				.allowedMentions(DiscordMessage.noMentions())
				.build(), log);
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

		handler.app.config.gnome_dm_webhook.execute(WebhookExecuteRequest.builder()
				.content(builder.toString().trim())
				.avatarUrl(author.getAvatarUrl())
				.username(author.getUsername() + " [" + author.getId().asString() + "]")
				.allowedMentions(DiscordMessage.noMentions().toData())
				.build()
		);
	}

	public static void reply(DiscordHandler handler, User author, MessageChannel channel, String content) {
		channel.createMessage(content).block();

		handler.app.config.gnome_dm_webhook.execute(WebhookExecuteRequest.builder()
				.content(content)
				.avatarUrl(Assets.AVATAR.getPath())
				.username("Gnome [" + author.getId().asString() + "]")
				.allowedMentions(DiscordMessage.noMentions().toData())
				.build()
		);
	}
}
