package dev.gnomebot.app;

import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.util.ConfigFile;
import dev.gnomebot.app.util.URLRequest;
import discord4j.common.util.Snowflake;

import java.nio.file.Path;

/**
 * @author LatvianModder
 */
public class Config {
	private static Config inst;

	public static Config get() {
		if (inst == null) {
			try {
				inst = new Config(AppPaths.CONFIG_FILE);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		return inst;
	}

	public final int port;
	public final String panel_url;
	public final String db_uri;
	public final String discord_bot_token;
	public final String self_token;
	public final WebHook death_webhook;
	public final WebHook gnome_mention_webhook;
	public final WebHook gnome_dm_webhook;
	public final Snowflake gnome_dm_channel_id;

	private Config(Path file) {
		ConfigFile c = new ConfigFile(file);

		port = c.getInt("port", 26609);

		String defUrl = "";

		if (!c.has("panel_url")) {
			try {
				defUrl = "http://" + URLRequest.of("https://api.ipify.org").toJoinedString().block().trim() + ":" + port;
			} catch (Exception ex) {
				ex.printStackTrace();
				defUrl = "http://localhost:" + port;
			}
		}

		panel_url = c.getString("panel_url", defUrl) + "/";
		db_uri = c.getString("db_uri", "mongodb://localhost:27017");
		discord_bot_token = c.getString("discord_bot_token", "");
		self_token = c.getString("self_token", "");
		death_webhook = new WebHook(c.getString("death_webhook", ""));
		gnome_mention_webhook = new WebHook(c.getString("gnome_mention_webhook", ""));
		gnome_dm_webhook = new WebHook(c.getString("gnome_dm_webhook", ""));
		gnome_dm_channel_id = c.getSnowflake("gnome_dm_channel_id");

		if (port < 1024 || port > 65535) {
			throw new IllegalArgumentException("Port has to be between [1024, 65535]!");
		}

		c.save();
		App.success("Loaded Gnome config with panel URL: " + panel_url);
	}
}