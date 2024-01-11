package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CachedRole {
	public final GuildCollections gc;
	public final Snowflake id;
	public final String name;
	public final int index;
	public final int rawPosition;
	public final Color color;
	public final PermissionSet permissions;
	public final boolean ownerRole;
	public boolean adminRole;

	public CachedRole(GuildCollections g, Role role, int index) {
		this.gc = g;
		this.id = role.getId();
		this.name = role.getName();
		this.index = index;
		this.rawPosition = role.getRawPosition();
		this.color = role.getColor();
		this.permissions = role.getPermissions().contains(Permission.ADMINISTRATOR) ? PermissionSet.all() : role.getPermissions();
		this.ownerRole = permissions.contains(Permission.ADMINISTRATOR);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (CachedRole) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "<@&" + id.asString() + '>';
	}

	@Nullable
	public Role getRole() {
		try {
			return gc.getGuild().getRoleById(id).block();
		} catch (Exception ex) {
			return null;
		}
	}

	public boolean is(Member member) {
		return member.getRoleIds().contains(id);
	}

	public boolean isMentioned(Message message) {
		return message.getRoleMentionIds().contains(id);
	}

	public boolean add(Member member, @Nullable String reason) {
		return member.getRoleIds().contains(id) || add(member.getId(), reason);
	}

	public boolean add(@Nullable Snowflake member, @Nullable String reason) {
		if (member != null) {
			try {
				gc.getClient().getRestClient().getGuildService().addGuildMemberRole(gc.guildId.asLong(), member.asLong(), id.asLong(), reason).block();
				return true;
			} catch (Exception ex) {
				App.warn("Can't assign role " + id.asString() + " to " + member.asString() + " in " + gc);
				App.warn(ex);
			}
		}

		return false;
	}

	public boolean remove(Member member, @Nullable String reason) {
		return !member.getRoleIds().contains(id) || remove(member.getId(), reason);
	}

	public boolean remove(@Nullable Snowflake member, @Nullable String reason) {
		if (member != null) {
			try {
				gc.getClient().getRestClient().getGuildService().removeGuildMemberRole(gc.guildId.asLong(), member.asLong(), id.asLong(), reason).block();
				return true;
			} catch (Exception ex) {
				App.warn("Can't remove role " + id.asString() + " from " + member.asString() + " in " + gc);
				App.warn(ex);
			}
		}

		return false;
	}
}
