package dev.gnomebot.app.data.channel;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.CachedRole;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.EnumMap;
import java.util.EnumSet;

public record Permissions(EnumMap<Permission, Boolean> map, long id) {
	public static final Permission[] VALUES = Permission.values();
	public static final Permissions NONE = new Permissions(new EnumMap<>(Permission.class), 0L);
	public static final Permissions ALL = new Permissions(new EnumMap<>(Permission.class), 0L);

	static {
		for (var permission : Permission.values()) {
			ALL.map.put(permission, Boolean.TRUE);
		}
	}

	public static Permissions from(PermissionSet set, long owner) {
		var map = new EnumMap<>(NONE.map);

		for (var p : set) {
			map.put(p, Boolean.TRUE);
		}

		return new Permissions(map, owner);
	}

	public static Permissions ofRole(CachedRole role) {
		return role == null ? NONE : role.permissions;
	}

	public static Permissions compute(GuildCollections gc, ChannelInfo ci, long memberId) {
		var member = gc.getMemberData(memberId);

		if (member == null) {
			return NONE;
		}

		var permissions = ofRole(gc.roles().everyone);
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

	public PermissionSet asSet() {
		if (this == NONE) {
			return PermissionSet.none();
		} else if (this == ALL) {
			return PermissionSet.all();
		}

		var set = EnumSet.noneOf(Permission.class);

		for (var permission : VALUES) {
			if (has(permission)) {
				set.add(permission);
			}
		}

		return PermissionSet.of(set.toArray(new Permission[0]));
	}
}
