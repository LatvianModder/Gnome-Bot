package dev.gnomebot.app.discord.command;

/**
 * @author LatvianModder
 */
public class InviteCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("invite")
			.description("Find invite links related to this server")
			.add(string("search").required().suggest(InviteCommand::suggestInvite))
			.run(InviteCommand::run);

	private static void suggestInvite(ChatCommandSuggestionEvent event) {
		event.suggest("WIP");
	}

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();
		event.respond("WIP!");
	}
}
