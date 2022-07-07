package dev.gnomebot.app.script;

import dev.gnomebot.app.discord.CachedRole;
import discord4j.discordjson.json.RoleModifyRequest;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;

public class WrappedRole extends DiscordObject {
	public final WrappedGuild guild;
	public final transient CachedRole role;
	private String name;

	WrappedRole(WrappedGuild g, CachedRole w) {
		super(new WrappedId(w.id));
		guild = g;
		role = w;
		name = role.name;
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
		guild.discordJS.checkReadOnly();
		guild.getGuildService().modifyGuildRole(guild.id.asLong(), id.asLong(), RoleModifyRequest.builder().name(s).build(), null).block();
		name = s;
	}

	@Override
	public void delete(@Nullable String reason) {
		guild.discordJS.checkReadOnly();
		guild.getGuildService().deleteGuildRole(guild.id.asLong(), id.asLong(), reason).block();
	}
}
