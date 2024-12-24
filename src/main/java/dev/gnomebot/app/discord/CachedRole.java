package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.channel.Permissions;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.log.Log;
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
	public final long id;
	public final String name;
	public final int index;
	public final int rawPosition;
	public final Color color;
	public final Permissions permissions;
	public final boolean ownerRole;
	public boolean adminRole;

	public CachedRole(GuildCollections g, Role role, int index) {
		this.gc = g;
		this.id = role.getId().asLong();
		this.name = role.getName();
		this.index = index;
		this.rawPosition = role.getRawPosition();
		this.color = role.getColor();
		this.permissions = Permissions.from(role.getPermissions().contains(Permission.ADMINISTRATOR) ? PermissionSet.all() : role.getPermissions(), id);
		this.ownerRole = permissions.has(Permission.ADMINISTRATOR);
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
		return "<@&" + SnowFlake.str(id) + '>';
	}

	@Nullable
	public Role getRole() {
		try {
			return gc.getGuild().getRoleById(SnowFlake.convert(id)).block();
		} catch (Exception ex) {
			return null;
		}
	}

	public boolean is(Member member) {
		return member.getRoleIds().contains(SnowFlake.convert(id));
	}

	public boolean isMentioned(Message message) {
		return message.getRoleMentionIds().contains(SnowFlake.convert(id));
	}

	public boolean add(Member member, @Nullable String reason) {
		return is(member) || add(member.getId().asLong(), reason);
	}

	public boolean add(long member, @Nullable String reason) {
		if (member != 0L) {
			try {
				gc.getClient().getRestClient().getGuildService().addGuildMemberRole(gc.guildId, member, id, reason).block();
				return true;
			} catch (Exception ex) {
				Log.warn("Can't assign role " + SnowFlake.str(id) + " to " + SnowFlake.str(member) + " in " + gc);
				Log.warn(ex);
			}
		}

		return false;
	}

	public boolean remove(Member member, @Nullable String reason) {
		return !is(member) || remove(member.getId().asLong(), reason);
	}

	public boolean remove(long member, @Nullable String reason) {
		if (member != 0L) {
			try {
				gc.getClient().getRestClient().getGuildService().removeGuildMemberRole(gc.guildId, member, id, reason).block();
				return true;
			} catch (Exception ex) {
				Log.warn("Can't remove role " + SnowFlake.str(id) + " from " + SnowFlake.str(member) + " in " + gc);
				Log.warn(ex);
			}
		}

		return false;
	}
}
