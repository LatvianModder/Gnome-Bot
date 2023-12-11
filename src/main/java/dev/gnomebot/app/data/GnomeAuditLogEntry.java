package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GnomeAuditLogEntry extends WrappedDocument<GnomeAuditLogEntry> {
	public interface Flags {
		int LEVEL_01 = 1 << 0;
		int LEVEL_23 = 1 << 1;
		int EXPIRES = 1 << 2;
		int CONTENT = 1 << 3;
		int OLD_CONTENT = 1 << 4;
		int EXTRA = 1 << 5;
		int REVOCABLE = 1 << 6;
		int BOT_USER_IGNORED = 1 << 7;
		int DM = 1 << 8;
		int LOCKDOWN = 1 << 9;
		int DELETED_MESSAGES = 1 << 10;
	}

	public enum Type {
		MESSAGE_DELETED("message_deleted", 0, Flags.OLD_CONTENT | Flags.EXTRA | Flags.BOT_USER_IGNORED),
		MESSAGE_EDITED("message_edited", 0, Flags.CONTENT | Flags.OLD_CONTENT | Flags.EXTRA | Flags.BOT_USER_IGNORED),
		REACTION_ADDED("reaction_added", 0, Flags.CONTENT | Flags.BOT_USER_IGNORED),
		REACTION_REMOVED("reaction_removed", 0, Flags.CONTENT | Flags.BOT_USER_IGNORED),
		JOIN_VOICE("join_voice", 0, 0),
		LEAVE_VOICE("leave_voice", 0, 0),

		JOIN("join", 1, Flags.EXTRA),
		LEAVE("leave", 1, Flags.CONTENT | Flags.EXTRA),
		COMMAND("command", 1, Flags.OLD_CONTENT),
		ECHO("echo", 1, Flags.CONTENT),

		ADMIN_PING("admin_ping", 2, 0),
		DISCORD_INVITE("discord_invite", 2, Flags.REVOCABLE),
		IP_ADDRESS("ip_address", 2, 0),
		URL_SHORTENER("url_shortener", 2, 0),
		SCAM("scam", 2, 0),
		// BAD_WORD("bad_word", 2, 0),
		MESSAGE_REPORT("message_report", 2, 0),

		CUSTOM("custom", 3, Flags.CONTENT),
		BAN("ban", 3, Flags.REVOCABLE | Flags.CONTENT | Flags.EXTRA),
		UNBAN("unban", 3, 0),
		KICK("kick", 3, Flags.CONTENT | Flags.EXTRA),
		WARN("warn", 3, Flags.REVOCABLE | Flags.CONTENT | Flags.EXTRA),
		MUTE("mute", 3, Flags.REVOCABLE | Flags.CONTENT | Flags.EXTRA),
		NOTE("note", 2, Flags.REVOCABLE | Flags.CONTENT),
		LOCKDOWN_ENABLED("lockdown_enabled", 3, Flags.CONTENT),
		LOCKDOWN_DISABLED("lockdown_disabled", 3, 0),

		;

		public static final Map<String, Type> NAME_MAP = new HashMap<>();

		static {
			for (var type : values()) {
				NAME_MAP.put(type.name, type);
			}
		}

		public final String name;
		public final int flags;

		Type(String n, int l, int f) {
			name = n;
			flags = l | f | (l == 0 ? Flags.EXPIRES : 0);
		}

		public boolean has(int f) {
			return (flags & f) != 0;
		}

		public int level() {
			return flags & 3;
		}
	}

	public static Builder builder(Type type) {
		return new Builder(type);
	}

	public static class Builder {
		public final Type type;
		private int flags;
		private long channel = 0L;
		private long message = 0L;
		private long user = 0L;
		private long source = 0L;
		private String content = null;
		private String oldContent = null;
		private final Document extra = new Document();

		public Builder(Type t) {
			type = t;
			flags = type.flags;
		}

		public Builder channel(long l) {
			channel = l;
			return this;
		}

		public Builder channel(Channel channel) {
			return channel(channel.getId().asLong());
		}

		public Builder message(long l) {
			message = l;
			return this;
		}

		public Builder message(Message message) {
			return message(message.getId().asLong());
		}

		public Builder user(long l) {
			user = l;
			return this;
		}

		public Builder user(User user) {
			return user(user.getId().asLong());
		}

		public Builder source(long l) {
			source = l;
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

		public Builder flags(int f) {
			flags |= f;
			return this;
		}

		public Builder flags(int flags, boolean predicate) {
			return predicate ? flags(flags) : this;
		}

		public Document build() {
			var id = new ObjectId();
			var doc = new Document("_id", id);
			doc.put("type", type.name);
			doc.put("flags", flags);

			if (type.has(Flags.EXPIRES)) {
				doc.put("expires", id.getDate());
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

	public Type getType() {
		return Type.NAME_MAP.getOrDefault(document.getString("type"), Type.CUSTOM);
	}

	public int getFlags() {
		return document.getInt("flags");
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