package dev.gnomebot.app.script;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.util.ThreadMessageRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.MessageData;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public class WrappedMessage implements WithId, Deletable {
	public final WrappedChannel channel;
	public final transient Message message;
	public final transient MessageData messageData;
	public final WrappedId id;
	private String content;
	private String contentNoEmojis;
	private boolean cancelEvent;

	WrappedMessage(WrappedChannel c, Message w) {
		channel = c;
		message = w;
		messageData = message.getData();
		id = new WrappedId(message.getId());
		content = message.getContent();
	}

	@Override
	public WrappedId id() {
		return id;
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
		User u = message.getAuthor().orElse(null);
		return u == null ? null : getGuild().getUser(u.getId().asString());
	}

	public boolean isEdited() {
		return message.getEditedTimestamp().isPresent();
	}

	@Nullable
	public Date getEditedTimestamp() {
		Instant i = message.getEditedTimestamp().orElse(null);
		return i == null ? null : Date.from(i);
	}

	public boolean isUserMentioned() {
		return !messageData.mentions().isEmpty();
	}

	public boolean isUserMentioned(Snowflake id) {
		return message.getUserMentionIds().contains(id);
	}

	public WrappedUser[] getMentionedUsers() {
		List<Snowflake> list = message.getUserMentionIds();
		WrappedUser[] array = new WrappedUser[list.size()];

		for (int i = 0; i < array.length; i++) {
			array[i] = channel.guild.getUser(list.get(i).asString());
		}

		return array;
	}

	public boolean isRoleMentioned() {
		return !messageData.mentionRoles().isEmpty();
	}

	public boolean isRoleMentioned(Snowflake id) {
		return message.getRoleMentionIds().contains(id);
	}

	public WrappedRole[] getMentionedRoles() {
		List<Snowflake> list = message.getRoleMentionIds();
		WrappedRole[] array = new WrappedRole[list.size()];

		for (int i = 0; i < array.length; i++) {
			array[i] = channel.guild.roles.get(list.get(i).asString());
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

	// getReactions()

	public boolean isPinned() {
		return message.isPinned();
	}

	public boolean hasStickers() {
		return false;
	}

	@Nullable
	public WrappedMessage getReferencedMessage() {
		Message m = message.getReferencedMessage().orElse(null);
		return m == null ? null : channel.getMessage(m);
	}

	public boolean hasComponents() {
		return messageData.components().toOptional().isPresent();
	}

	@Override
	public void delete(@Nullable String reason) {
		message.delete(reason).block();
		cancel();
	}

	public void cancel() {
		cancelEvent = true;
	}

	public boolean cancelEvent() {
		return cancelEvent;
	}

	public void addReaction(ReactionEmoji emoji) {
		message.addReaction(emoji).block();
	}

	public void removeReaction(ReactionEmoji emoji) {
		message.removeSelfReaction(emoji).block();
	}

	public void removeReaction(ReactionEmoji emoji, Snowflake userId) {
		message.removeReaction(emoji, userId).block();
	}

	public void removeAllReactions(ReactionEmoji emoji) {
		message.removeReactions(emoji).block();
	}

	public void removeAllReactions() {
		message.removeAllReactions().block();
	}

	public void pin() {
		message.pin().block();
	}

	public void unpin() {
		message.unpin().block();
	}

	public void setPinned(boolean b) {
		if (b) {
			pin();
		} else {
			unpin();
		}
	}

	public WrappedMessage publish() {
		return channel.getMessage(message.publish().block());
	}

	public void createThread(String title) {
		try {
			Utils.THREAD_ROUTE.newRequest(channel.id.asLong, id.asLong)
					.body(new ThreadMessageRequest(title))
					.exchange(App.instance.discordHandler.client.getCoreResources().getRouter())
					.skipBody()
					.block();
		} catch (Exception ex) {
			App.error("Failed to create a thread!");
			App.warn(ex);
		}
	}

	public void setContent(String c) {
		message.edit(MessageEditSpec.builder().contentOrNull(c).allowedMentionsOrNull(DiscordMessage.noMentions()).build()).block();
		content = c;
		contentNoEmojis = null;
	}
}
