package dev.gnomebot.app.discord.legacycommand.bot;

import dev.gnomebot.app.discord.legacycommand.CommandCallback;
import dev.gnomebot.app.discord.legacycommand.LegacyDiscordCommand;
import dev.gnomebot.app.server.AuthLevel;

/**
 * @author LatvianModder
 */
public class KickCommand {
	@LegacyDiscordCommand(name = "kick", arguments = "[reason...]", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback COMMAND = (context, reader) -> context.sender.kick(reader.readRemainingString().orElse(null)).block();
}
