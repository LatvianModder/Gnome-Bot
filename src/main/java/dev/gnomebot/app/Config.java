package dev.gnomebot.app;

import dev.gnomebot.app.discord.WebHook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author LatvianModder
 */
public class Config {
	public static Config inst;

	public static Config get() {
		if (inst == null) {
			inst = new Config();

			try {
				inst.load(AppPaths.CONFIG_FILE);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		return inst;
	}

	public int port;
	public int max_errors;
	public String db_uri;
	public String discord_client_id;
	public String discord_client_secret;
	public String discord_bot_token;
	public String discord_public_key;
	public String self_token;
	public WebHook plex_webhook;
	public String plex_webhook_secret;
	public WebHook mm_showcase_webhook;
	public WebHook death_webhook;
	public WebHook gnome_mention_webhook;
	public WebHook gnome_dm_webhook;

	public void load(Path file) {
		Properties properties = new Properties();

		if (!Files.exists(file)) {
			try (BufferedWriter writer = Files.newBufferedWriter(file)) {
				properties.store(writer, "Config file for Gnome Bot");
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return;
		}

		try (BufferedReader reader = Files.newBufferedReader(file)) {
			properties.load(reader);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		port = Integer.parseInt(properties.getProperty("port", "0"));
		max_errors = Integer.parseInt(properties.getProperty("max_errors", "3"));
		db_uri = properties.getProperty("db_uri", "mongodb://localhost:27017");
		discord_client_id = properties.getProperty("discord_client_id", "");
		discord_client_secret = properties.getProperty("discord_client_secret", "");
		discord_bot_token = properties.getProperty("discord_bot_token", "");
		discord_public_key = properties.getProperty("discord_public_key", "");
		self_token = properties.getProperty("self_token", "");
		plex_webhook = new WebHook(properties.getProperty("plex_webhook", ""));
		plex_webhook_secret = properties.getProperty("plex_webhook_secret", "");
		mm_showcase_webhook = new WebHook(properties.getProperty("mm_showcase_webhook", ""));
		death_webhook = new WebHook(properties.getProperty("death_webhook", ""));
		gnome_mention_webhook = new WebHook(properties.getProperty("gnome_mention_webhook", ""));
		gnome_dm_webhook = new WebHook(properties.getProperty("gnome_dm_webhook", ""));

		if (port == 0) {
			throw new IllegalArgumentException("Port can't be 0!");
		}
	}
}