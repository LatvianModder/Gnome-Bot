package dev.gnomebot.app.discord.command;

/**
 * @author LatvianModder
 */
public class MentionsCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("mentions")
			.description("Manage mentions")
			.run(MentionsCommand::run);

	private static void run(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		event.respond("WIP!");
	}
}