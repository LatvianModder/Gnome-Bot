package dev.gnomebot.app.cli;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.command.RegisterCommand;
import dev.gnomebot.app.util.DiscordAnsi;

public class CLITestAnsi {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("test_ansi")
			.description("Test ANSI")
			.noAdmin()
			.run(CLITestAnsi::run);

	private static void run(CLIEvent event) throws Exception {
		StringBuilder sb = new StringBuilder("```ansi");

		sb.append('\n');
		sb.append(DiscordAnsi.progressBar(0.0F, "0%"));
		sb.append('\n');
		sb.append(DiscordAnsi.progressBar(0.25F, "25%"));
		sb.append('\n');
		sb.append(DiscordAnsi.progressBar(0.5F, "50%"));
		sb.append('\n');
		sb.append(DiscordAnsi.progressBar(0.75F, "75%"));
		sb.append('\n');
		sb.append(DiscordAnsi.progressBar(1.0F, "100%"));
		sb.append('\n');
		sb.append(DiscordAnsi.progressBar(-1.0F, "Error!"));

		sb.append("```");
		App.info(sb.toString());
		event.respond(sb.toString());
	}
}
