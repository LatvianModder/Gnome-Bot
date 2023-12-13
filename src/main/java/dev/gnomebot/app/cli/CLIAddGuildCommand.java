package dev.gnomebot.app.cli;

public class CLIAddGuildCommand {
	public static final CLICommand COMMAND = CLICommand.make("add_guild_command")
			.description("Adds a new guild command")
			.arg('n', CLIArgument.Type.STRING)
			.arg('d', CLIArgument.Type.STRING)
			.arg('a', CLIArgument.Type.STRING)
			.arg('i', CLIArgument.Type.STRING)
			.run(CLIAddGuildCommand::run);

	// add counter_leaderboard

	private static void run(CLIEvent event) {
	}
}
