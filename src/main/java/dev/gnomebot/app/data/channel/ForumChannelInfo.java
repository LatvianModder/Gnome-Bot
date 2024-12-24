package dev.gnomebot.app.data.channel;

import dev.gnomebot.app.data.GuildCollections;

public class ForumChannelInfo extends TopLevelChannelInfo {
	public ForumChannelInfo(GuildCollections g, long id, String name, ChannelSettings s) {
		super(g, id, name, s);
	}
}
