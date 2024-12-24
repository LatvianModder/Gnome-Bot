package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.json.JSONObject;
import discord4j.core.object.entity.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CachedRoles implements Iterable<CachedRole> {
	public final GuildCollections gc;
	public final List<CachedRole> list;
	private final Map<Long, CachedRole> map;
	public final Map<String, CachedRole> uniqueNameMap;
	public final CachedRole everyone;

	public CachedRoles(GuildCollections gc) {
		this.gc = gc;
		this.list = new ArrayList<>();
		this.map = new LinkedHashMap<>();
		this.uniqueNameMap = new LinkedHashMap<>();

		long adminRoleId = gc.adminRole.get();

		CachedRole adminRoleW = null;
		CachedRole everyone = null;

		for (var r : gc.getGuild().getRoles().sort(Comparator.comparing(Role::getRawPosition).thenComparing(Role::getId).reversed()).toStream().toList()) {
			var role = new CachedRole(gc, r, list.size());

			if (r.isEveryone()) {
				everyone = role;
			} else {
				if (role.id == adminRoleId) {
					adminRoleW = role;
				}

				list.add(role);

				var name = role.name.toLowerCase().replace(' ', '-');
				var key = name;

				for (var i = 2; uniqueNameMap.containsKey(key); i++) {
					key = name + '-' + i;
				}

				uniqueNameMap.put(key, role);
			}

			map.put(role.id, role);
		}

		if (adminRoleW != null) {
			for (var role : list) {
				if (role.index <= adminRoleW.index) {
					role.adminRole = true;
				}
			}
		}

		this.everyone = everyone;
		Log.debug("Cached roles for " + gc + " updated");
	}

	public String name(long id) {
		var ri = map.get(id);
		return ri == null ? SnowFlake.str(id) : ri.name;
	}

	public String displayName(long id) {
		return "@" + name(id);
	}

	@Nullable
	public CachedRole get(long id) {
		return map.get(id);
	}

	@Override
	@NotNull
	public Iterator<CachedRole> iterator() {
		return list.iterator();
	}

	public JSONObject json(long channel) {
		var json = JSONObject.of();
		json.put("id", SnowFlake.str(channel));
		json.put("name", displayName(channel));
		return json;
	}
}
