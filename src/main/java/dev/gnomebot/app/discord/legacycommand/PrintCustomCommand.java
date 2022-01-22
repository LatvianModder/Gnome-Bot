package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.data.DiscordCustomCommand;
import dev.gnomebot.app.server.AuthLevel;

import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class PrintCustomCommand {
	@LegacyDiscordCommand(name = "print_custom_command", help = "Prints  a custom command", arguments = "<command name>", permissionLevel = AuthLevel.ADMIN)
	public static final CommandCallback COMMAND = (context, reader) -> {
		String commandName = reader.readString().orElse("").toLowerCase();
		DiscordCustomCommand command = context.gc.customCommands.query().eq("command_name", commandName).first();

		if (command != null) {
			context.reply(commandName, command.getCommandList().stream().collect(Collectors.joining("\n")));
		} else {
			throw new DiscordCommandException("Unknown custom command!");
		}
	};
}
