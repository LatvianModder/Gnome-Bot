package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;

public class SettingsCommands extends ApplicationCommands {
	public static void set(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderOwner();

		var key = event.context.gc.config.map.get(event.get("key").asString(""));

		if (key != null && !key.internal) {
			var value = event.context.gc.config.map.get(event.get("value").asString(""));

			event.respond("WIP");
		}

		throw new GnomeException("Config key not found!");
	}

	public static void get(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		var key = event.context.gc.config.map.get(event.get("key").asString(""));

		if (key != null && !key.internal) {
			event.respond(key.serialize());
		}

		throw new GnomeException("Config key not found!");
	}

	public static void suggestKey(ChatCommandSuggestionEvent event) {
		for (var key : event.context.gc.config.map.values()) {
			if (!key.internal) {
				event.suggest(key.id, key.serialize());
			}
		}
	}

	public static void suggestValue(ChatCommandSuggestionEvent event) {
		var key = event.context.gc.config.map.get(event.get("key").asString(""));

		App.info(key);

		if (key != null && !key.internal && key.enumValues != null) {
			App.info(key.enumValues.get());

			for (var value : key.enumValues.get()) {
				event.suggest(value.name(), value.value());
			}
		}
	}
}