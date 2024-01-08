package dev.gnomebot.app;

import dev.latvian.apps.webutils.data.Lazy;
import dev.latvian.apps.webutils.json.JSON;
import discord4j.common.util.Snowflake;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GuildPaths {
	public static final Lazy<Map<String, String>> CUSTOM_NAMES = Lazy.of(() -> {
		var map = new HashMap<String, String>();

		if (Files.exists(AppPaths.CUSTOM_GUILD_IDS)) {
			try {
				for (var entry : JSON.DEFAULT.read(AppPaths.CUSTOM_GUILD_IDS).readObject().entrySet()) {
					map.put(entry.getKey(), String.valueOf(entry.getValue()));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		map.put("0", "unknown");
		return map;
	});

	public final Snowflake id;
	public final String key;
	public final Path path;
	public final Path info;
	public final Path config;
	public final Path macrosFile;
	public final Path macros;
	public final Path scripts;

	public GuildPaths(Snowflake i) {
		id = i;
		key = CUSTOM_NAMES.get().getOrDefault(id.asString(), id.asString());
		path = AppPaths.makeDir(AppPaths.GUILD_DATA.resolve(key));

		info = path.resolve("info.json");
		config = path.resolve("config.json");
		macrosFile = path.resolve("macros.json");
		macros = AppPaths.makeDir(path.resolve("macros"));
		scripts = AppPaths.makeDir(path.resolve("scripts"));
	}
}
