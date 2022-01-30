package dev.gnomebot.app;

import discord4j.common.util.Snowflake;

import java.nio.file.Path;

public class GuildPaths {
	public final Snowflake id;
	public final Path path;
	public final Path config;
	public final Path scripts;

	public GuildPaths(Snowflake i) {
		id = i;
		path = AppPaths.makeDir(AppPaths.DATA_GUILDS.resolve(id.asString()));
		config = path.resolve("config.json");
		scripts = AppPaths.makeDir(path.resolve("scripts"));
	}
}
