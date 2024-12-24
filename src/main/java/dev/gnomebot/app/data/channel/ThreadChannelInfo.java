package dev.gnomebot.app.data.channel;

import dev.gnomebot.app.discord.WebHookDestination;
import discord4j.core.object.entity.channel.CategorizableChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public class ThreadChannelInfo extends ChannelInfo {
	public final TopLevelChannelInfo parent;

	public ThreadChannelInfo(TopLevelChannelInfo parent, long id, String name) {
		super(parent.gc, id, name, parent.settings);
		this.parent = parent;
	}

	@Override
	public boolean isThread() {
		return true;
	}

	@Override
	public long getTopId() {
		return parent.getTopId();
	}

	@Override
	public int getXp() {
		return settings.threadXp >= 0 ? settings.threadXp : parent.getXp();
	}

	@Override
	public Map<Long, Permissions> getPermissionOverrides() {
		return parent.getPermissionOverrides();
	}

	@Override
	@Nullable
	public CategorizableChannel getTopLevelChannel() {
		return parent.getTopLevelChannel();
	}

	@Override
	public Permissions getPermissions(long member) {
		return parent.getPermissions(member);
	}

	@Override
	public Optional<WebHookDestination> getWebHook() {
		return parent.getWebHook().map(w -> w.withThread(this, id));
	}
}
