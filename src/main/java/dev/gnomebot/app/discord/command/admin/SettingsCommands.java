package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;

public class SettingsCommands extends ApplicationCommands {
	public static void set(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderOwner();
		event.respond("WIP!");
	}

	public static void get(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		event.respond("WIP!");
	}

	public static void suggestKey(ChatCommandSuggestionEvent event) {
	}

	public static void suggestValue(ChatCommandSuggestionEvent event) {
	}
}