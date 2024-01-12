package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import org.bson.Document;

public final class ChannelSettings extends WrappedDocument<ChannelSettings> {
	public final long channelId;
	public int xp;
	public int threadXp;
	public boolean autoThread;
	public boolean autoUpvote;

	public ChannelSettings(WrappedCollection<ChannelSettings> c, MapWrapper d) {
		super(c, d);
		channelId = getUID();
		name = SnowFlake.str(channelId);
		xp = document.getInt("xp", -1);
		threadXp = document.getInt("thread_xp", -1);
		autoThread = document.getBoolean("auto_thread");
		autoUpvote = document.getBoolean("auto_upvote");
	}

	public void updateFrom(TopLevelGuildMessageChannel ch) {
		if (!name.equals(ch.getName())) {
			name = ch.getName();
		}
	}

	public void save() {
		if (collection.findFirst(getUID()) == null) {
			collection.insert(new Document("_id", getUID()));
		}
	}

	public void setXp(int xp) {
		this.xp = xp;
		save();
		update("xp", xp);
	}

	public void setThreadXp(int threadXp) {
		this.threadXp = threadXp;
		save();
		update("thread_xp", threadXp);
	}

	public void setAutoThread(boolean autoThread) {
		this.autoThread = autoThread;
		save();
		update("auto_thread", autoThread);
	}

	public void setAutoUpvote(boolean autoUpvote) {
		this.autoUpvote = autoUpvote;
		save();
		update("auto_upvote", autoUpvote);
	}
}