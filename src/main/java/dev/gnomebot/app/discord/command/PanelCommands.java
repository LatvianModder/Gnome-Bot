package dev.gnomebot.app.discord.command;

/**
 * @author LatvianModder
 */
public class PanelCommands extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("panel")
			.description("Panel commands")
			.add(sub("login")
					.description("Log in to the panel")
					.run(PanelCommands::login)
			)
			.add(sub("logout")
					.description("Log out of the panel (Invalidates all your tokens)")
					.run(PanelCommands::logout)
			)
			.add(sub("logout-everyone")
					.description("Log everyone out of the panel (Invalidates everyone's tokens)")
					.run(PanelCommands::logoutEveryone)
			);

	private static void login(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.respond("WIP!");
	}

	private static void logout(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		// App.instance.db.webTokens.query(request.token.document.getString("_id")).delete();
		event.respond("WIP!");
	}

	private static void logoutEveryone(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderTrusted();
		// App.instance.db.webTokens.query().many().delete();
		event.respond("WIP!");
	}
}