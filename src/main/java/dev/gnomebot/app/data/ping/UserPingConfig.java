package dev.gnomebot.app.data.ping;

import java.util.Collections;
import java.util.Set;

public record UserPingConfig(Set<Long> ignoredGuilds, Set<Long> ignoredChannels, Set<Long> ignoredUsers, boolean bots, boolean self) {
	public static final UserPingConfig DEFAULT = new UserPingConfig(true, false);
	public static final UserPingConfig DEFAULT_NO_BOTS = new UserPingConfig(false, false);
	public static final UserPingConfig DEFAULT_SELF = new UserPingConfig(true, true);
	public static final UserPingConfig DEFAULT_NO_BOTS_SELF = new UserPingConfig(false, true);

	public static UserPingConfig get(Set<Long> ignoredGuilds, Set<Long> ignoredChannels, Set<Long> ignoredUsers, boolean bots, boolean self) {
		if (ignoredGuilds.isEmpty() && ignoredChannels.isEmpty() && ignoredUsers.isEmpty()) {
			if (self) {
				return bots ? DEFAULT_SELF : DEFAULT_NO_BOTS_SELF;
			} else {
				return bots ? DEFAULT : DEFAULT_NO_BOTS;
			}
		}

		return new UserPingConfig(Set.copyOf(ignoredGuilds), Set.copyOf(ignoredChannels), Set.copyOf(ignoredUsers), bots, self);
	}

	private UserPingConfig(boolean bots, boolean self) {
		this(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), bots, self);
	}

	public boolean match(long guildId, long channelId, long userId, boolean bot) {
		if (!bots && bot) {
			return false;
		} else if (ignoredGuilds.contains(guildId)) {
			return false;
		} else if (ignoredChannels.contains(channelId)) {
			return false;
		} else {
			return !ignoredUsers.contains(userId);
		}
	}

	public boolean match(PingData pingData) {
		return match(pingData.gc().guildId, pingData.channel().id, pingData.userId(), pingData.bot());
	}

	@Override
	public String toString() {
		if (this == DEFAULT) {
			return "default";
		} else if (this == DEFAULT_NO_BOTS) {
			return "default_no_bots";
		} else if (this == DEFAULT_SELF) {
			return "default_self";
		} else if (this == DEFAULT_NO_BOTS_SELF) {
			return "default_no_bots_self";
		}

		return "{" +
				"ignoredGuilds=" + ignoredGuilds +
				", ignoredChannels=" + ignoredChannels +
				", ignoredUsers=" + ignoredUsers +
				", bots=" + bots +
				", self=" + self +
				'}';
	}
}
