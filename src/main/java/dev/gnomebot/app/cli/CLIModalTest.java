package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RegisterCommand;
import discord4j.core.object.component.Button;

public class CLIModalTest {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("modal_test")
			.description("Test modals")
			.noAdmin()
			.visible()
			.run(CLIModalTest::run);

	private static void run(CLIEvent event) throws Exception {
		event.respond("Modal Test");
		event.response.addComponentRow(Button.danger("modal_test", "Click this to open test form!"));
	}
}
