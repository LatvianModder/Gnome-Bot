package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class GnomeAuditLogEntry extends WrappedDocument<GnomeAuditLogEntry> {
	public interface Flags {
		int EXPIRES = 1;
		int CHANNEL = 2;
		int MESSAGE = 4;
		int USER = 8;
		int SOURCE = 16;
		int CONTENT = 32;
		int OLD_CONTENT = 64;
		int EXTRA = 128;
		int REVOCABLE = 256;
		int BOT_USER_IGNORED = 512;

		int CHANNEL_MESSAGE_USER = CHANNEL | MESSAGE | USER;
		int CHANNEL_MESSAGE_USER_CONTENT = CHANNEL_MESSAGE_USER | CONTENT;
	}

	public enum Type {
		MESSAGE_DELETED("message_deleted", 0, Flags.CHANNEL_MESSAGE_USER | Flags.OLD_CONTENT | Flags.EXTRA | Flags.BOT_USER_IGNORED),
		MESSAGE_EDITED("message_edited", 0, Flags.CHANNEL_MESSAGE_USER_CONTENT | Flags.OLD_CONTENT | Flags.EXTRA | Flags.BOT_USER_IGNORED),
		REACTION_ADDED("reaction_added", 0, Flags.CHANNEL_MESSAGE_USER_CONTENT | Flags.BOT_USER_IGNORED),
		REACTION_REMOVED("reaction_removed", 0, Flags.CHANNEL_MESSAGE_USER_CONTENT | Flags.BOT_USER_IGNORED),
		JOIN_VOICE("join_voice", 0, Flags.CHANNEL | Flags.USER),
		LEAVE_VOICE("leave_voice", 0, Flags.CHANNEL | Flags.USER),

		JOIN("join", 1, Flags.USER | Flags.EXTRA),
		LEAVE("leave", 1, Flags.USER | Flags.CONTENT | Flags.EXTRA),
		COMMAND("command", 1, Flags.CHANNEL_MESSAGE_USER_CONTENT | Flags.OLD_CONTENT),
		ECHO("echo", 1, Flags.CHANNEL | Flags.USER | Flags.CONTENT),

		DISCORD_INVITE("discord_invite", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT),
		FORCED_NAME_UPDATE("forced_name_update", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT | Flags.OLD_CONTENT),
		IP_ADDRESS("ip_address", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT),
		EVERYONE_PING("everyone_ping", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT),
		URL_SHORTENER("url_shortener", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT),
		SCAM("scam", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT),
		BAD_WORD("bad_word", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT),
		ADMIN_PING("admin_ping", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT | Flags.OLD_CONTENT),
		MESSAGE_REPORT("message_report", 2, Flags.CHANNEL_MESSAGE_USER_CONTENT | Flags.SOURCE),

		CUSTOM("custom", 3, Flags.SOURCE | Flags.CONTENT),
		BAN("ban", 3, Flags.REVOCABLE | Flags.USER | Flags.SOURCE | Flags.CONTENT | Flags.EXTRA),
		UNBAN("unban", 3, Flags.USER),
		KICK("kick", 3, Flags.USER | Flags.SOURCE | Flags.CONTENT | Flags.EXTRA),
		WARN("warn", 3, Flags.REVOCABLE | Flags.USER | Flags.SOURCE | Flags.CONTENT | Flags.EXTRA),
		MUTE("mute", 3, Flags.REVOCABLE | Flags.USER | Flags.SOURCE | Flags.CONTENT | Flags.EXTRA),
		LOCKDOWN_ENABLED("lockdown_enabled", 3, Flags.SOURCE | Flags.CONTENT),
		LOCKDOWN_DISABLED("lockdown_disabled", 3, Flags.SOURCE),

		;

		public static final Map<String, Type> NAME_MAP = new HashMap<>();

		static {
			for (Type type : values()) {
				NAME_MAP.put(type.name, type);
			}
		}

		public final String name;
		public final int level;
		public final int flags;

		Type(String n, int l, int f) {
			name = n;
			level = l;
			flags = f | (level == 0 ? Flags.EXPIRES : 0);
		}

		public boolean has(int f) {
			return (flags & f) != 0;
		}
	}

	public static Builder builder(Type type) {
		return new Builder(type);
	}

	public static class Builder {
		private Instant timestamp;
		public final Type type;
		private long channel = 0L;
		private long message = 0L;
		private long user = 0L;
		private long source = 0L;
		private String content = null;
		private String oldContent = null;
		private final Document extra = new Document();

		public Builder(Type t) {
			type = t;
		}

		public Builder timestamp(Instant in) {
			timestamp = in;
			return this;
		}

		public Builder channel(Snowflake l) {
			channel = l.asLong();
			return this;
		}

		public Builder channel(Channel channel) {
			return channel(channel.getId());
		}

		public Builder message(Snowflake l) {
			message = l.asLong();

			if (timestamp == null) {
				timestamp = l.getTimestamp();
			}

			return this;
		}

		public Builder message(Message message) {
			return message(message.getId());
		}

		public Builder user(Snowflake l) {
			user = l.asLong();
			return this;
		}

		public Builder user(User user) {
			return user(user.getId());
		}

		public Builder source(Snowflake l) {
			source = l.asLong();
			return this;
		}

		public Builder source(@Nullable User user) {
			source = user == null ? 0L : user.getId().asLong();
			return this;
		}

		public Builder content(@Nullable String s) {
			content = s;
			return this;
		}

		public Builder oldContent(@Nullable String s) {
			oldContent = s;
			return this;
		}

		public Builder extra(String key, @Nullable Object value) {
			if (value != null) {
				extra.put(key, value);
			}

			return this;
		}

		public Document build() {
			if (timestamp == null) {
				timestamp = Instant.now();
			}

			Document doc = new Document();
			doc.put("type", type.name);
			doc.put("timestamp", Date.from(timestamp));
			doc.put("level", type.level);

			if (type.has(Flags.EXPIRES)) {
				doc.put("expires", Date.from(timestamp));
			}

			if (channel != 0L) {
				doc.put("channel", channel);
			}

			if (message != 0L) {
				doc.put("message", message);
			}

			if (user != 0L) {
				doc.put("user", user);
			}

			if (source != 0L) {
				doc.put("source", source);
			}

			if (content != null) {
				doc.put("content", content);
			}

			if (oldContent != null) {
				doc.put("old_content", oldContent);
			}

			if (!extra.isEmpty()) {
				doc.put("extra", extra);
			}

			return doc;
		}
	}

	public GnomeAuditLogEntry(WrappedCollection<GnomeAuditLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	@Override
	public Date getDate() {
		return document.getDate("timestamp");
	}

	public Type getType() {
		return Type.NAME_MAP.getOrDefault(document.getString("type"), Type.CUSTOM);
	}

	@Nullable
	public Date getExpires() {
		return document.getDate("expires");
	}

	public long getChannel() {
		return document.getLong("channel");
	}

	public long getMessage() {
		return document.getLong("message");
	}

	public long getUser() {
		return document.getLong("user");
	}

	public long getSource() {
		return document.getLong("source");
	}

	public String getOldContent() {
		return document.getString("old_content");
	}

	public String getContent() {
		return document.getString("content");
	}

	public MapWrapper getExtra() {
		return document.getMap("extra");
	}
}