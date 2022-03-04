package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RegisterCommand;

import java.awt.GraphicsEnvironment;

public class CLIFonts {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("fonts")
			.description("Print all fonts that server has available")
			.noAdmin()
			.run(CLIFonts::run);

	private static void run(CLIEvent event) {
		event.respond("Fonts:\n" + String.join("\n", GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
	}
}
