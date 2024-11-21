package dev.gnomebot.app.discord;

import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.json.JSONObject;
import discord4j.core.object.entity.User;

import java.util.HashMap;
import java.util.Optional;

public class UserCache {
	private final DiscordHandler handler;
	private final HashMap<Long, Optional<User>> map = new HashMap<>();

	public UserCache(DiscordHandler h) {
		handler = h;
	}

	public int getCacheSize() {
		return map.size();
	}

	public Optional<User> get(long id) {
		return map.computeIfAbsent(id, snowflake -> Optional.ofNullable(handler.getUser(snowflake)));
	}

	public String getUsername(long id) {
		return get(id).map(User::getUsername).orElse("Deleted User");
	}

	public String getTag(long id) {
		return get(id).map(User::getTag).orElse("Deleted User#0000");
	}

	public JSONObject getJson(long id) {
		var json = JSONObject.of();
		json.put("id", SnowFlake.str(id));
		json.put("name", getUsername(id));
		return json;
	}
}