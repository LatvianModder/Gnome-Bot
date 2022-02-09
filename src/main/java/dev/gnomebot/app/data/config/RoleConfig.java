package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.CachedRole;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import org.jetbrains.annotations.Nullable;

public class RoleConfig extends SnowflakeConfig {
	public RoleConfig(GuildCollections gc, String name) {
		super(gc, name);
	}

	@Override
	public String getType() {
		return "role";
	}

	@Override
	public String toString() {
		return "<@&" + get().asString() + '>';
	}

	public boolean is(Member member) {
		return !isSet() || member.getRoleIds().contains(get());
	}

	public boolean isMentioned(Message message) {
		return isSet() && message.getRoleMentionIds().contains(get());
	}

	@Nullable
	public CachedRole getRole() {
		return isSet() ? gc.getRoleMap().get(get()) : null;
	}

	public boolean add(@Nullable Snowflake member, @Nullable String reason) {
		CachedRole role = getRole();
		return role != null && role.add(member, reason);
	}

	public boolean remove(@Nullable Snowflake member, @Nullable String reason) {
		CachedRole role = getRole();
		return role != null && role.remove(member, reason);
	}
}
