package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.discord.QuoteHandler;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.AllowedMentions;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DiscordMessage extends WrappedDocument<DiscordMessage> {
	public static final long FLAG_BOT = 1L << 0L;
	public static final long FLAG_EMBEDS = 1L << 1L;
	public static final long FLAG_IMAGES = 1L << 2L;
	public static final long FLAG_VIDEOS = 1L << 3L;
	public static final long FLAG_MENTIONS_ANYONE = 1L << 4L;
	public static final long FLAG_MENTIONS_USERS = 1L << 5L;
	public static final long FLAG_MENTIONS_ROLES = 1L << 6L;
	public static final long FLAG_MENTIONS_EVERYONE = 1L << 7L;
	public static final long FLAG_MENTIONS_BOT = 1L << 8L;
	public static final long FLAG_EDITED = 1L << 9L;
	//public static final long FLAG_DELETED = 1L << 10L;
	// public static final long FLAG_BAD_WORD = 1L << 11L;
	public static final long FLAG_TTS = 1L << 19L;
	public static final long FLAG_MULTILINE = 1L << 20L;
	public static final long FLAG_IP = 1L << 21L;
	public static final long FLAG_ATTACHMENTS = 1L << 22L;
	//public static final long FLAG_DM = 1L << 23L;
	public static final long FLAG_FILES = 1L << 24L;
	public static final long FLAG_REPLY = 1L << 25L;

	public static AllowedMentions noMentions() {
		return MessageBuilder.NO_MENTIONS;
	}

	public final long flags;

	public DiscordMessage(WrappedCollection<DiscordMessage> c, MapWrapper d) {
		super(c, d);
		flags = d.getLong("flags");
	}

	public boolean is(long flag) {
		return (flags & flag) != 0L;
	}

	public String getContent() {
		return document.getString("content");
	}

	public long getUserID() {
		return document.getLong("user");
	}

	public long getChannelID() {
		return document.getLong("channel");
	}

	@Override
	public Date getDate() {
		return document.getDate("timestamp");
	}

	public void delete(GuildCollections gc, boolean auditLog) {
		deleteOrEdit(gc, true, "", auditLog);
	}

	public void edit(GuildCollections gc, String newContent, boolean auditLog) {
		deleteOrEdit(gc, false, newContent, auditLog);
	}

	public long getReply() {
		return document.getLong("reply");
	}

	public List<String> getImages() {
		return document.getList("images");
	}

	public String getURL(GuildCollections gc) {
		return "https://discord.com/channels/" + gc.guildId.asString() + "/" + Snowflake.of(getChannelID()).asString() + "/" + getUIDSnowflake().asString();
	}

	public String getURLAsArrow(GuildCollections gc) {
		return "[Quote ➤](" + getURL(gc) + ")";
	}

	private void deleteOrEdit(GuildCollections gc, boolean deleted, String newContent, boolean auditLog) {
		var flags1 = deleted ? flags : (flags & ~DiscordMessage.FLAG_EDITED);

		List<Bson> updates = new ArrayList<>();
		updates.add(Updates.set("channel", getChannelID()));
		updates.add(Updates.set("user", getUserID()));
		updates.add(Updates.set("flags", flags1));
		updates.add(Updates.set("timestamp", getDate()));
		updates.add(Updates.set("old_content", getContent()));
		updates.add(Updates.set("new_content", newContent));
		updates.add(Updates.set("deleted", deleted));

		var reply = getReply();

		if (auditLog) {
			gc.auditLog(GnomeAuditLogEntry.builder(deleted ? GnomeAuditLogEntry.Type.MESSAGE_DELETED : GnomeAuditLogEntry.Type.MESSAGE_EDITED)
					.channel(getChannelID())
					.message(getUID())
					.user(getUserID())
					.oldContent(getContent())
					.content(newContent)
					.extra("flags", flags1 == 0L ? null : flags1)
					.extra("reply", reply == 0L ? null : reply)
			);
		}

		if (reply != 0L) {
			updates.add(Updates.set("reply", reply));
		}

		gc.editedMessages.query(getUID()).upsert(updates);

		if (deleted) {
			gc.messages.query(getUID()).delete();
		} else {
			gc.messages.query(getUID()).update(Updates.bitwiseOr("flags", DiscordMessage.FLAG_EDITED), Updates.set("content", newContent));
		}
	}

	public void appendMessageURL(StringBuilder sb) {
		QuoteHandler.getMessageURL(sb, collection.gc.guildId, Snowflake.of(getChannelID()), Snowflake.of(getUID()));
	}
}