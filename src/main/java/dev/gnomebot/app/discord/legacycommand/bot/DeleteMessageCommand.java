package dev.gnomebot.app.discord.legacycommand.bot;

import dev.gnomebot.app.discord.legacycommand.CommandCallback;
import dev.gnomebot.app.discord.legacycommand.LegacyDiscordCommand;
import dev.gnomebot.app.server.AuthLevel;

/**
 * @author LatvianModder
 */
public class DeleteMessageCommand {
	@LegacyDiscordCommand(name = "delete_message", arguments = "[reason...]", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback COMMAND = (context, reader) -> context.message.delete(reader.readRemainingString().orElse(null)).block();
}
