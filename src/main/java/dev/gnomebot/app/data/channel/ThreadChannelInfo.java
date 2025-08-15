package dev.gnomebot.app.data.channel;

import dev.gnomebot.app.discord.WebHookDestination;
import discord4j.core.object.entity.channel.CategorizableChannel;
import discord4j.rest.util.Permission;
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
	public CachedPermissions getPermissions(long member) {
		return parent.getPermissions(member);
	}

	@Override
	public CachedPermissions getSelfPermissions() {
		return parent.getSelfPermissions();
	}

	@Override
	public boolean checkPermissions(long memberId, Permission... permissions) {
		return parent.checkPermissions(memberId, permissions);
	}

	@Override
	public boolean checkPermissions(long memberId, Permission permission) {
		return parent.checkPermissions(memberId, permission);
	}

	@Override
	public boolean canViewChannel(long memberId) {
		return parent.canViewChannel(memberId);
	}

	@Override
	public Optional<WebHookDestination> getWebHook() {
		return parent.getWebHook().map(w -> w.withThread(this, id));
	}
}
