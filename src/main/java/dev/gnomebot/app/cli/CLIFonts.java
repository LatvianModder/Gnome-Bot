package dev.gnomebot.app.cli;

import java.awt.GraphicsEnvironment;

public class CLIFonts {
	public static final CLICommand COMMAND = CLICommand.make("fonts")
			.description("Print all fonts that server has available")
			.noAdmin()
			.run(CLIFonts::run);

	private static void run(CLIEvent event) {
		event.respond("Fonts:\n" + String.join("\n", GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
	}
}
