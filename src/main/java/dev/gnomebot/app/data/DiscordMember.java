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

	public long getTotalMessages() {
		return document.getLong("total_messages");
	}

	public long getTotalXp() {
		return document.getLong("total_xp");
	}

	public String getNickname() {
		return document.getString("nickname");
	}

	public String getDisplayName() {
		return document.getString("nickname", getName());
	}

	public String getAvatar() {
		return document.getString("avatar");
	}

	public String getDiscriminator() {
		return document.getString("discriminator");
	}

	public List<Long> getRoles() {
		return document.getList("roles");
	}

	@Nullable
	public Date getMuted() {
		return document.getDate("muted");
	}

	public String getTag() {
		return getName() + "#" + getDiscriminator();
	}

	public List<String> getPings() {
		return document.getList("pings");
	}
}