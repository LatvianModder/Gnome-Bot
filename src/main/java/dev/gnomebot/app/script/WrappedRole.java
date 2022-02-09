package dev.gnomebot.app.script;

import dev.gnomebot.app.discord.CachedRole;
import discord4j.discordjson.json.RoleModifyRequest;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;

public class WrappedRole implements WithId, Deletable {
	public final WrappedGuild guild;
	public final WrappedId id;
	public final transient CachedRole role;
	private String name;

	WrappedRole(WrappedGuild g, CachedRole w) {
		guild = g;
		id = new WrappedId(w.id);
		role = w;
		name = role.name;
	}

	@Override
	public WrappedId id() {
		return id;
	}

	@Override
	public String toString() {
		return "@" + getName();
	}

	public String getName() {
		return name;
	}

	public int getRawPosition() {
		return role.rawPosition;
	}

	public int getColor() {
		return role.color.getRGB();
	}

	public PermissionSet getPermissions() {
		return role.permissions;
	}

	public boolean isOwnerRole() {
		return role.ownerRole;
	}

	public boolean isAdminRole() {
		return role.adminRole;
	}

	public void setName(String s) {
		guild.getGuildService().modifyGuildRole(guild.id.asLong(), id.asLong(), RoleModifyRequest.builder().name(s).build(), null).block();
		name = s;
	}

	@Override
	public void delete(@Nullable String reason) {
		guild.getGuildService().deleteGuildRole(guild.id.asLong(), id.asLong(), reason).block();
	}
}
