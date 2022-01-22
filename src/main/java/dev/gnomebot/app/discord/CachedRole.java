package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GuildCollections;
import discord4j.common.util.Snowflake;
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
	public final int rawPosition;
	public final Color color;
	public final PermissionSet permissions;
	public final boolean ownerRole;
	public boolean adminRole;

	public CachedRole(GuildCollections g, Role role) {
		gc = g;
		id = role.getId();
		name = role.getName();
		rawPosition = role.getRawPosition();
		color = role.getColor();
		permissions = role.getPermissions().contains(Permission.ADMINISTRATOR) ? PermissionSet.all() : role.getPermissions();
		ownerRole = permissions.contains(Permission.ADMINISTRATOR);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CachedRole that = (CachedRole) o;
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
}
