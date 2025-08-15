package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GnomeAuditLogEntry extends WrappedDocument<GnomeAuditLogEntry> {
	public interface Flags {
		int EXPIRES = 1 << 2;
		int CONTENT = 1 << 3;
		// int OLD_CONTENT = 1 << 4;
		int EXTRA = 1 << 5;
		int REVOCABLE = 1 << 6;
		// int BOT_USER_IGNORED = 1 << 7;
		int DM = 1 << 8;
		int LOCKDOWN = 1 << 9;
		int DELETED_MESSAGES = 1 << 10;

		int USER_AUDIT_LOG = 1 << 11;
	}

	public enum Type {
		REACTION_ADDED("reaction_added", "Added Reaction", 0, Flags.EXPIRES | Flags.CONTENT, 0, gc -> gc.reactionLog),
		REACTION_REMOVED("reaction_removed", "Removed Reaction", 0, Flags.EXPIRES | Flags.CONTENT, 1, gc -> gc.reactionLog),
		JOIN_VOICE("join_voice", "Joined Voice", 0, Flags.EXPIRES, 0, gc -> gc.voiceLog),
		LEAVE_VOICE("leave_voice", "Left Voice", 0, Flags.EXPIRES, 1, gc -> gc.voiceLog),

		MACRO_EDIT("macro_edit", "Macro Edited", 1, Flags.USER_AUDIT_LOG | Flags.REVOCABLE),

		ADMIN_PING("admin_ping", "Admin Ping", 2, 0),
		DISCORD_INVITE("discord_invite", "Discord Invite", 2, Flags.USER_AUDIT_LOG | Flags.CONTENT | Flags.REVOCABLE),
		IP_ADDRESS("ip_address", "IP Address", 2, Flags.USER_AUDIT_LOG | Flags.REVOCABLE),
		URL_SHORTENER("url_shortener", "URL Shortener", 2, Flags.USER_AUDIT_LOG | Flags.REVOCABLE),
		SCAM("scam", "Potential Scam", 2, Flags.USER_AUDIT_LOG | Flags.REVOCABLE),
		// BAD_WORD("bad_word", "Bad Word", 2, Flags.USER_AUDIT_LOG),
		MESSAGE_REPORT("message_report", "Message Report", 2, Flags.USER_AUDIT_LOG),

		CUSTOM("custom", "Custom", 3, Flags.CONTENT),
		BAN("ban", "Banned", 3, Flags.USER_AUDIT_LOG | Flags.REVOCABLE | Flags.CONTENT | Flags.EXTRA),
		UNBAN("unban", "Unbanned", 3, Flags.USER_AUDIT_LOG),
		KICK("kick", "Kicked", 3, Flags.USER_AUDIT_LOG | Flags.CONTENT | Flags.EXTRA),
		WARN("warn", "Warned", 3, Flags.USER_AUDIT_LOG | Flags.REVOCABLE | Flags.CONTENT | Flags.EXTRA),
		MUTE("mute", "Muted", 3, Flags.USER_AUDIT_LOG | Flags.REVOCABLE | Flags.CONTENT | Flags.EXTRA),
		NOTE("note", "Note", 2, Flags.USER_AUDIT_LOG | Flags.REVOCABLE | Flags.CONTENT),
		LOCKDOWN_ENABLED("lockdown_enabled", "Enabled Lockdown", 3, Flags.CONTENT),
		LOCKDOWN_DISABLED("lockdown_disabled", "Disabled Lockdown", 3, 0),

		;

		public static final Map<String, Type> NAME_MAP = new HashMap<>();
		public static final List<String> USER_AUDIT_LOG_TYPES = new ArrayList<>();

		static {
			for (var type : values()) {
				NAME_MAP.put(type.name, type);

				if (type.has(Flags.USER_AUDIT_LOG)) {
					USER_AUDIT_LOG_TYPES.add(type.name);
				}
			}
		}

		public final String name;
		public final String displayName;
		public final int flags;
		public final int customId;
		public final Function<GuildCollections, WrappedCollection<GnomeAuditLogEntry>> collection;

		Type(String n, String dn, int l, int f) {
			this(n, dn, l, f, -1, gc -> gc.auditLog);
		}

		Type(String n, String dn, int l, int f, int customId, Function<GuildCollections, WrappedCollection<GnomeAuditLogEntry>> collection) {
			this.name = n;
			this.displayName = dn;
			this.flags = l | f | (l == 0 ? Flags.EXPIRES : 0);
			this.customId = customId;
			this.collection = collection;
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

			if (type.customId != -1) {
				doc.put("type", type.customId);
			} else {
				doc.put("type", type.name);
			}

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

			if (!extra.isEmpty()) {
				doc.put("extra", extra);
			}

			return doc;
		}
	}

	public GnomeAuditLogEntry(WrappedCollection<GnomeAuditLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	public Type type() {
		return Type.NAME_MAP.getOrDefault(document.getString("type"), Type.CUSTOM);
	}

	public int flags() {
		return document.getInt("flags");
	}

	@Nullable
	public Date expires() {
		return document.getDate("expires");
	}

	public long channel() {
		return document.getLong("channel");
	}

	public long message() {
		return document.getLong("message");
	}

	public long user() {
		return document.getLong("user");
	}

	public long source() {
		return document.getLong("source");
	}

	public String oldContent() {
		return document.getString("old_content");
	}

	public String content() {
		return document.getString("content");
	}

	public MapWrapper extra() {
		return document.getMap("extra");
	}
}