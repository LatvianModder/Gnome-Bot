package dev.gnomebot.app.data.config;

import dev.gnomebot.app.App;
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

	public boolean add(Snowflake member, @Nullable String reason) {
		if (member != null && isSet()) {
			try {
				gc.getClient().getRestClient().getGuildService().addGuildMemberRole(gc.guildId.asLong(), member.asLong(), get().asLong(), reason).block();
				return true;
			} catch (Exception ex) {
				App.warn("Can't assign role " + get().asString() + " to " + member.asString() + " in " + gc);
				App.warn(ex);
			}
		}

		return false;
	}

	public boolean remove(Snowflake member, @Nullable String reason) {
		if (member != null && isSet()) {
			try {
				gc.getClient().getRestClient().getGuildService().removeGuildMemberRole(gc.guildId.asLong(), member.asLong(), get().asLong(), reason).block();
				return true;
			} catch (Exception ex) {
				App.warn("Can't remove role " + get().asString() + " from " + member.asString() + " in " + gc);
				App.warn(ex);
			}
		}

		return false;
	}
}
