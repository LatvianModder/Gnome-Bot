package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.Config;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.DiscordHandler;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.InteractionEventWrapper;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.MessageId;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.json.response.ErrorResponse;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class CommandContext {
	public DiscordHandler handler;
	public GuildCollections gc;
	public ChannelInfo channelInfo;
	public Message message;
	public Member sender;
	public InteractionEventWrapper<?> interaction = null;
	public boolean referenceMessage = true;
	public AllowedMentions allowedMentions = DiscordMessage.noMentions();

	@Override
	public String toString() {
		return "Context{" +
				"gc=" + gc +
				", channel=#" + (channelInfo == null ? "<null>" : channelInfo.getName()) +
				", message=" + (message == null ? "<null>" : message.getContent()) +
				", sender=" + sender.getTag() +
				'}';
	}

	public void checkBotPerms(@Nullable ChannelInfo channelInfo, Permission... perms) {
		if (channelInfo == null) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "I don't have permission to do that :cry:\nChannel info can't be loaded").reaction(Emojis.RAGE).ephemeral();
		}

		if (!channelInfo.getSelfPermissions().containsAll(PermissionSet.of(perms))) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "I don't have permission to do that :cry:\nPermissions required: " + Utils.permsToString(perms)).reaction(Emojis.RAGE).ephemeral();
		}
	}

	public void checkBotPerms(Permission... perms) {
		checkBotPerms(channelInfo, perms);
	}

	public void checkGlobalPerms(Permission... perms) {
		if (!gc.getEffectiveGlobalPermissions(gc.db.app.discordHandler.selfId).containsAll(PermissionSet.of(perms))) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "I don't have permission to do that :cry:\nPermissions required: " + Utils.permsToString(perms)).reaction(Emojis.RAGE).ephemeral();
		}

		if (!gc.getEffectiveGlobalPermissions(sender.getId()).containsAll(PermissionSet.of(perms))) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "You don't have permission to do that :cry:\nPermissions required: " + Utils.permsToString(perms)).reaction(Emojis.RAGE).ephemeral();
		}
	}

	public void checkSenderPerms(@Nullable ChannelInfo channelInfo, Permission... perms) {
		if (channelInfo == null) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "You don't have permission to do that :cry:\nChannel info can't be loaded").reaction(Emojis.RAGE).ephemeral();
		}

		if (!channelInfo.checkPermissions(sender.getId(), perms)) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "You don't have permission to do that :cry:\nPermissions required: " + Utils.permsToString(perms)).reaction(Emojis.RAGE).ephemeral();
		}
	}

	public void checkSenderPerms(Permission... perms) {
		checkSenderPerms(channelInfo, perms);
	}

	public void checkSenderTrusted() {
		if (!isTrusted()) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "Only bot owners can do this!");
		}
	}

	public void checkSenderOwner() {
		if (!isOwner()) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "Wait a minute, you're not an owner! Only owners can do this");
		}
	}

	public void checkSenderAdmin() {
		if (!isAdmin()) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "Wait a minute, you're not an admin! Only admins can do this");
		}
	}

	public Message reply(MessageBuilder msg) {
		try {
			if (referenceMessage && message != null) {
				msg.messageReference(message.getId());
			}

			msg.allowedMentions(allowedMentions);

			Message m = Objects.requireNonNull(channelInfo.createMessage(msg).block());

			if (referenceMessage && message != null) {
				MessageHandler.addAutoDelete(message.getId(), new MessageId(m));
			}

			return m;
		} catch (ClientException ex) {
			ex.printStackTrace();
			throw new GnomeException("Failed to reply! " + ex.getStatus() + " " + ex.getErrorResponse().map(ErrorResponse::getFields).map(Object::toString).orElse("Unknown error")).clientException(ex);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new GnomeException("Failed to reply! " + ex);
		}
	}

	public Message reply(EmbedBuilder embed) {
		return reply(MessageBuilder.create(embed));
	}

	public Message reply(String title, String message) {
		return reply(EmbedBuilder.create(title, message));
	}

	public Message reply(String content) {
		return reply(MessageBuilder.create(content));
	}

	public Optional<Message> findMessage(Snowflake id) {
		return gc.findMessage(id, channelInfo);
	}

	public boolean isTrusted() {
		return Config.get().isTrusted(sender.getId());
	}

	public boolean isAdmin() {
		return gc.getAuthLevel(sender).is(AuthLevel.ADMIN);
	}

	public boolean isOwner() {
		return gc.getAuthLevel(sender).is(AuthLevel.OWNER);
	}

	public void upvote() {
		if (message != null) {
			message.addReaction(Emojis.VOTEUP).subscribe();
		}
	}
}
