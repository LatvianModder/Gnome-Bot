package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.server.AuthLevel;

/**
 * @author LatvianModder
 */
public class RemoveCustomCommand {
	@LegacyDiscordCommand(name = "remove_custom_command", help = "Removes a custom command", arguments = "<command name>", permissionLevel = AuthLevel.ADMIN)
	public static final CommandCallback COMMAND = (context, reader) -> {
		String commandName = reader.readString().orElse("").toLowerCase();

		if (context.gc.customCommands.query().eq("command_name", commandName).delete().getDeletedCount() > 0L) {
			context.upvote();
		} else {
			throw new DiscordCommandException("Unknown custom command!");
		}
	};
}
