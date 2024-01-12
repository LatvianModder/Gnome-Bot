package dev.gnomebot.app.script;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.Vote;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.script.event.EventJS;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.StartThreadSpec;
import discord4j.discordjson.json.MessageData;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@SuppressWarnings("unused")
public class WrappedMessage extends DiscordObject {
	public final WrappedChannel channel;
	public final transient Message message;
	public final transient MessageData messageData;
	private String content;
	private String contentNoEmojis;
	public transient EventJS messageEvent;

	WrappedMessage(WrappedChannel c, Message w) {
		super(new WrappedId(w.getData().id()));
		channel = c;
		message = w;
		messageData = message.getData();
		content = message.getContent();
	}

	@Override
	public String toString() {
		return getContent();
	}

	public WrappedGuild getGuild() {
		return channel.guild;
	}

	public String getContent() {
		return content;
	}

	public String getContentNoEmojis() {
		if (contentNoEmojis == null) {
			contentNoEmojis = Emojis.stripEmojis(content);
		}

		return contentNoEmojis;
	}

	@Nullable
	public WrappedUser getAuthor() {
		var u = message.getAuthor().orElse(null);
		return u == null ? null : getGuild().getUser(u.getId().asString());
	}

	public boolean isEdited() {
		return messageData.editedTimestamp().isPresent();
	}

	@Nullable
	public Date getEditedTimestamp() {
		var i = messageData.editedTimestamp().orElse("");
		return i.isEmpty() ? null : Date.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(i, Instant::from));
	}

	public boolean isUserMentioned() {
		return !messageData.mentions().isEmpty();
	}

	public boolean isUserMentioned(WrappedId id) {
		for (var u : messageData.mentions()) {
			if (u.id().asLong() == id.asLong()) {
				return true;
			}
		}

		return false;
	}

	public WrappedUser[] getMentionedUsers() {
		var list = messageData.mentions();
		var array = new WrappedUser[list.size()];

		for (var i = 0; i < array.length; i++) {
			array[i] = channel.guild.getUser(list.get(i).id().asString());
		}

		return array;
	}

	public boolean isRoleMentioned() {
		return !messageData.mentionRoles().isEmpty();
	}

	public boolean isRoleMentioned(WrappedId id) {
		return messageData.mentionRoles().contains(id.asString());
	}

	public WrappedRole[] getMentionedRoles() {
		var list = messageData.mentionRoles();
		var array = new WrappedRole[list.size()];

		for (var i = 0; i < array.length; i++) {
			array[i] = channel.guild.roles.get(list.get(i));
		}

		return array;
	}

	public boolean hasAttachments() {
		return !messageData.attachments().isEmpty();
	}

	public boolean hasEmbeds() {
		return !messageData.embeds().isEmpty();
	}

	public boolean hasReactions() {
		return messageData.reactions().toOptional().isPresent();
	}

	public boolean hasStickers() {
		return messageData.stickerItems().toOptional().isPresent();
	}

	@Nullable
	public WrappedMessage getReferencedMessage() {
		var m = message.getReferencedMessage().orElse(null);
		return m == null ? null : channel.getMessage(m);
	}

	public boolean hasComponents() {
		return messageData.components().toOptional().isPresent();
	}

	@Override
	public void delete(@Nullable String reason) {
		channel.guild.discordJS.checkReadOnly();
		message.delete(reason).block();

		if (messageEvent != null) {
			messageEvent.cancel();
		}
	}

	public void addReaction(ReactionEmoji emoji) {
		channel.guild.discordJS.checkReadOnly();
		message.addReaction(emoji).block();
	}

	public void removeReaction(ReactionEmoji emoji) {
		channel.guild.discordJS.checkReadOnly();
		message.removeSelfReaction(emoji).block();
	}

	public void removeReaction(ReactionEmoji emoji, long userId) {
		channel.guild.discordJS.checkReadOnly();
		message.removeReaction(emoji, SnowFlake.convert(userId)).block();
	}

	public void removeAllReactions(ReactionEmoji emoji) {
		channel.guild.discordJS.checkReadOnly();
		message.removeReactions(emoji).block();
	}

	public void removeAllReactions() {
		channel.guild.discordJS.checkReadOnly();
		message.removeAllReactions().block();
	}

	public void upvote() {
		addReaction(Vote.UP.reaction);
	}

	public void downvote() {
		addReaction(Vote.DOWN.reaction);
	}

	public boolean isPinned() {
		return messageData.pinned();
	}

	public void setPinned(boolean b) {
		channel.guild.discordJS.checkReadOnly();

		if (b != message.isPinned()) {
			channel.getChannelService().addPinnedMessage(channel.id.asLong(), id.asLong()).block();
		} else {
			channel.getChannelService().deletePinnedMessage(channel.id.asLong(), id.asLong()).block();
		}
	}

	public WrappedMessage publish() {
		channel.guild.discordJS.checkReadOnly();
		return channel.getMessage(message.publish().block());
	}

	public WrappedThread createThread(String title) {
		channel.guild.discordJS.checkReadOnly();

		try {
			return new WrappedThread(channel.guild, message.startThread(StartThreadSpec.builder().name(title).build()).block());
		} catch (Exception ex) {
			App.error("Failed to create a thread!");
			App.warn(ex);
			return null;
		}
	}

	public void setContent(String c) {
		channel.guild.discordJS.checkReadOnly();
		message.edit(MessageEditSpec.builder().contentOrNull(c).allowedMentionsOrNull(DiscordMessage.noMentions()).build()).block();
		content = c;
		contentNoEmojis = null;
	}

	public String getUrl() {
		return "https://discord.com/channels/" + channel.guild.id.asString() + "/" + channel.id.asString() + "/" + id.asString();
	}

	public WrappedMessage reply(MessageBuilder message) {
		channel.guild.discordJS.checkReadOnly();
		message.messageReference(id.asLong());
		return new WrappedMessage(channel, new Message(channel.guild.gc.db.app.discordHandler.client, channel.getChannelService().createMessage(channel.id.asLong(), message.toMultipartMessageCreateRequest()).block()));
	}
}
