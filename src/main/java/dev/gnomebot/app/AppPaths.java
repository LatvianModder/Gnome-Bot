package dev.gnomebot.app;

import discord4j.common.util.Snowflake;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public interface AppPaths {
	static Path makeDir(Path p) {
		if (Files.notExists(p)) {
			try {
				Files.createDirectories(p);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return p;
	}

	Path RESOURCES = makeDir(Path.of("resources"));
	Path ASSETS = makeDir(RESOURCES.resolve("assets"));
	Path EXPORT = makeDir(Path.of("export"));
	Path DATA = makeDir(Path.of("data"));

	Path CACHE = makeDir(Path.of("cache"));
	Path AVATAR_CACHE = makeDir(CACHE.resolve("avatars"));
	Path EMOJI_CACHE = makeDir(CACHE.resolve("emoji"));

	Path LOG = DATA.resolve("log.txt");
	Path CONFIG_FILE = DATA.resolve("config.json");
	Path COMMANDS_FILE = DATA.resolve("commands");
	Path GUILD_DATA = makeDir(DATA.resolve("guilds"));
	Path PUBLIC_DATA = makeDir(DATA.resolve("public"));
	Path BAD_DOMAINS = DATA.resolve("bad_domains.txt");
	Path BAD_DOMAIN_OVERRIDES = DATA.resolve("bad_domain_overrides.txt");
	Path DM_CHANNELS = DATA.resolve("dm_channels.txt");
	Path CUSTOM_GUILD_IDS = DATA.resolve("custom_guild_ids.txt");
	Path PINGS = makeDir(DATA.resolve("pings"));

	Map<Snowflake, GuildPaths> GUILD_PATHS = new HashMap<>();

	static GuildPaths getGuildPaths(Snowflake id) {
		return GUILD_PATHS.computeIfAbsent(id, GuildPaths::new);
	}
}
