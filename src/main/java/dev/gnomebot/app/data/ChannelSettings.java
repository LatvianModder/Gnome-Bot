package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;

public final class ChannelSettings extends WrappedDocument<ChannelSettings> {
	public final long guildId;
	public final long channelId;
	public int xp;
	public int threadXp;
	public long totalMessages;
	public long totalXp;
	public boolean autoThread;
	public boolean autoUpvote;

	public ChannelSettings(WrappedCollection<ChannelSettings> c, MapWrapper d) {
		super(c, d);
		channelId = document.getLong("_id");
		guildId = document.getLong("guild");
		name = document.getString("name");
		xp = document.getInt("xp", -1);
		threadXp = document.getInt("thread_xp", -1);
		totalMessages = document.getLong("total_messages");
		totalXp = document.getLong("total_xp");
		autoThread = document.getBoolean("auto_thread");
		autoUpvote = document.getBoolean("auto_upvote");
	}

	public void updateFrom(TopLevelGuildMessageChannel ch) {
		if (!name.equals(ch.getName())) {
			name = ch.getName();
			update("name", name);
		}
	}
}