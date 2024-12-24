package dev.gnomebot.app.data.channel;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.CachedRole;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.EnumMap;

public record Permissions(EnumMap<Permission, Boolean> map, long id) {
	public static final Permissions DEFAULT = new Permissions(new EnumMap<>(Permission.class), 0L);

	public static Permissions from(PermissionSet set, long owner) {
		var map = new EnumMap<>(DEFAULT.map);

		for (var p : set) {
			map.put(p, Boolean.TRUE);
		}

		return new Permissions(map, owner);
	}

	public static Permissions ofRole(CachedRole role) {
		return role == null ? DEFAULT : role.permissions;
	}

	public static Permissions compute(GuildCollections gc, ChannelInfo ci, long memberId) {
		var member = gc.getMemberData(memberId);

		if (member == null) {
			return DEFAULT;
		}

		var permissions = ofRole(gc.roles().get(gc.guildId));
		var roleIds = member.roles();

		for (var id : roleIds) {
			permissions = permissions.merge(ofRole(gc.roles().get(id.asLong())));
		}

		var overrides = ci.getPermissionOverrides();

		for (var id : roleIds) {
			permissions = permissions.merge(overrides.getOrDefault(id.asLong(), permissions));
		}

		return permissions.merge(overrides.getOrDefault(memberId, permissions));
	}

	public Permissions merge(Permissions override) {
		if (override == null || override == this || override.map.isEmpty()) {
			return this;
		}

		var newMap = new EnumMap<>(map);
		newMap.putAll(override.map);
		return new Permissions(newMap, id);
	}

	public boolean has(Permission permission) {
		return Boolean.TRUE.equals(map.get(permission));
	}

	public boolean has(Permission... permissions) {
		for (var permission : permissions) {
			if (!has(permission)) {
				return false;
			}
		}

		return true;
	}
}
