package dev.gnomebot.app.data.channel;

import discord4j.rest.util.Permission;

public record CachedPermissions(long permissions) {
	public boolean has(Permission permission) {
		return (permissions & permission.getValue()) != 0;
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
