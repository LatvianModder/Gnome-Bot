package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.data.config.GuildConfig;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;

public class SettingsCommands extends ApplicationCommands {
	public static void set(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderOwner();

		var holder = event.context.gc.getConfigHolder(event.get("key").asString(""));

		if (holder != null) {
			var value = event.get("value").asString("");
			var error = holder.validate(0, value);

			if (error.isEmpty()) {
				holder.deserialize(0, value);
				event.respond("Updated config value");
			} else {
				throw new GnomeException(error);
			}
		} else {
			throw new GnomeException("Config key not found!");
		}
	}

	public static void get(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		var holder = event.context.gc.getConfigHolder(event.get("key").asString(""));

		if (holder != null) {
			event.respond(holder.serialize(0));
		} else {
			throw new GnomeException("Config key not found!");
		}
	}

	public static void suggestKey(ChatCommandSuggestionEvent event) {
		for (var key : GuildConfig.MAP.values()) {
			event.suggest(key.title(), key.id());
		}
	}

	public static void suggestValue(ChatCommandSuggestionEvent event) {
		var key = GuildConfig.get(event.get("key").asString(""));

		if (key != null && key.type().hasEnumValues()) {
			for (var value : key.type().getEnumValues(event.context.gc)) {
				event.suggest(value.name(), value.value());
			}
		}
	}
}