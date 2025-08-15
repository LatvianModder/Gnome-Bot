package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public class DiscordMember extends WrappedDocument<DiscordMember> {
	public static final long FLAG_BOT = 1L << 0L;
	public static final long FLAG_NICKNAME = 1L << 1L;
	public static final long FLAG_ANIMATED_AVATAR = 1L << 2L;
	public static final long FLAG_GONE = 1L << 3L;

	public static final long DELETED_USER_ID = 456226577798135808L;

	public final long flags;

	public DiscordMember(WrappedCollection<DiscordMember> c, MapWrapper d) {
		super(c, d);
		flags = document.getLong("flags");
	}

	public boolean is(long flag) {
		return (flags & flag) != 0L;
	}

	public boolean set(long flag, boolean value) {
		if (is(flag) != value) {
			if (value) {
				update("flags", flags | flag);
			} else {
				update("flags", flags & ~flag);
			}

			return true;
		}

		return false;
	}

	public long totalMessages() {
		return document.getLong("total_messages");
	}

	public long totalXp() {
		return document.getLong("total_xp");
	}

	public String nickname() {
		return document.getString("nickname");
	}

	public String avatar() {
		return document.getString("avatar");
	}

	public String discriminator() {
		return document.getString("discriminator");
	}

	public List<Long> roles() {
		return document.getList("roles");
	}

	@Nullable
	public Date muted() {
		return document.getDate("muted");
	}

	public List<String> pings() {
		return document.getList("pings");
	}

	public String getTag() {
		return getName() + "#" + discriminator();
	}

	public String displayName() {
		var n = nickname();
		return n.isEmpty() ? getName() : n;
	}
}