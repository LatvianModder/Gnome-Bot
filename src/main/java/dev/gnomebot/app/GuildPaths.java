package dev.gnomebot.app;

import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.webutils.data.Lazy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GuildPaths {
	public static final Lazy<Map<Long, String>> CUSTOM_NAMES = Lazy.of(() -> {
		var map = new HashMap<Long, String>();

		if (Files.exists(AppPaths.CUSTOM_GUILD_IDS)) {
			try {
				for (var line : Files.readAllLines(AppPaths.CUSTOM_GUILD_IDS)) {
					if (line.isBlank()) {
						continue;
					}

					var split = line.split(":", 2);

					if (split.length >= 2) {
						map.put(SnowFlake.num(split[0]), split[1]);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		map.put(0L, "unknown");
		return map;
	});

	public static final Lazy<Map<String, Long>> INVERTED_CUSTOM_NAMES = Lazy.of(() -> {
		var map = new HashMap<String, Long>();

		for (var e : CUSTOM_NAMES.get().entrySet()) {
			map.put(e.getValue(), e.getKey());
		}

		return map;
	});

	public final long id;
	public final String key;
	public final Path path;
	public final Path info;
	public final Path config;
	public final Path macrosFile;
	public final Path macroUseFile;
	public final Path macros;
	public final Path scripts;
	public final Path feedback;
	public final Path polls;

	public GuildPaths(long i) {
		id = i;
		var key0 = CUSTOM_NAMES.get().get(id);
		key = key0 == null ? SnowFlake.str(id) : key0;
		path = AppPaths.makeDir(AppPaths.GUILD_DATA.resolve(key));

		info = path.resolve("info.json");
		config = path.resolve("config.json");
		macrosFile = path.resolve("macros.json");
		macroUseFile = path.resolve("macro_use.txt");
		macros = path.resolve("macros");
		scripts = path.resolve("scripts");
		feedback = path.resolve("feedback");
		polls = path.resolve("polls");
	}
}
