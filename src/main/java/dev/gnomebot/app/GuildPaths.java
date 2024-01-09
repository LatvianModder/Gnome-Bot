package dev.gnomebot.app;

import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.data.Lazy;
import discord4j.common.util.Snowflake;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GuildPaths {
	public static final Lazy<Map<Snowflake, String>> CUSTOM_NAMES = Lazy.of(() -> {
		var map = new HashMap<Snowflake, String>();

		if (Files.exists(AppPaths.CUSTOM_GUILD_IDS)) {
			try {
				for (var line : Files.readAllLines(AppPaths.CUSTOM_GUILD_IDS)) {
					if (line.isBlank()) {
						continue;
					}

					var split = line.split(":", 2);

					if (split.length >= 2) {
						map.put(Utils.snowflake(split[0]), split[1]);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		map.put(Utils.NO_SNOWFLAKE, "unknown");
		return map;
	});

	public static final Lazy<Map<String, Snowflake>> INVERTED_CUSTOM_NAMES = Lazy.of(() -> {
		var map = new HashMap<String, Snowflake>();

		for (var e : CUSTOM_NAMES.get().entrySet()) {
			map.put(e.getValue(), e.getKey());
		}

		return map;
	});

	public final Snowflake id;
	public final String key;
	public final Path path;
	public final Path info;
	public final Path config;
	public final Path macrosFile;
	public final Path macroUseFile;
	public final Path macros;
	public final Path scripts;

	public GuildPaths(Snowflake i) {
		id = i;
		key = CUSTOM_NAMES.get().getOrDefault(id, id.asString());
		path = AppPaths.makeDir(AppPaths.GUILD_DATA.resolve(key));

		info = path.resolve("info.json");
		config = path.resolve("config.json");
		macrosFile = path.resolve("macros.json");
		macroUseFile = path.resolve("macro_use.txt");
		macros = path.resolve("macros");
		scripts = path.resolve("scripts");
	}
}
