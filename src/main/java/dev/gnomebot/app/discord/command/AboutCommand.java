package dev.gnomebot.app.discord.command;

public class AboutCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("about")
			.supportsDM()
			.description("Info about Gnome Bot")
			.add(sub("macro")
					.description("Info about macros")
					.run(AboutCommand::macro)
			)
			.add(sub("pings")
					.description("Info about pings")
					.run(PingsCommands::help)
			)
			.add(sub("regex")
					.description("Info about Regular Expressions")
					.run(PingsCommands::regexHelp)
			)
			.add(sub("complex")
					.description("Info about complex message format")
					.run(AboutCommand::complex)
			);

	private static void macro(ChatInputInteractionEventWrapper event) {
		event.respond(MacroCommands.HELP);
	}

	private static void complex(ChatInputInteractionEventWrapper event) {
		event.respond("WIP");
	}
}