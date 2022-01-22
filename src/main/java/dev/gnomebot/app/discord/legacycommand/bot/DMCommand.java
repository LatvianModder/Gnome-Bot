package dev.gnomebot.app.discord.legacycommand.bot;

import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.legacycommand.CommandCallback;
import dev.gnomebot.app.discord.legacycommand.LegacyDiscordCommand;
import dev.gnomebot.app.server.AuthLevel;

/**
 * @author LatvianModder
 */
public class DMCommand {
	@LegacyDiscordCommand(name = "dm", arguments = "<text...>", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback COMMAND = (context, reader) -> {
		String text = reader.readRemainingString().orElse("");
		DM.send(context.handler, context.sender, text, true);
	};
}
