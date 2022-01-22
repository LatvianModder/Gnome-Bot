package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;

/**
 * @author LatvianModder
 */
public class MiscCommands extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("misc")
			.description("Misc commands that don't really deserve to take up their own command slot")
			.add(sub("emojiful")
					.description("Generate emojiful datapack")
					.add(string("emojis"))
					.run(MiscCommands::emojiful)
			);

	private static void emojiful(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		throw wip();
	}
}