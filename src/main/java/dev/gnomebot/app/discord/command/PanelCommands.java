package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;

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
		String tokenString = event.context.gc.db.getEncodedToken(event.context.sender.getId().asLong(), event.context.sender.getUsername());
		event.respond("[Click here to open the panel!](<" + App.url("panel/login?logintoken=" + tokenString) + ">)");
	}

	private static void logout(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		App.instance.db.invalidateToken(event.context.sender.getId().asLong());
		event.respond("Your Gnome Panel login tokens have been invalidated!");
	}

	private static void logoutEveryone(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderTrusted();
		App.instance.db.invalidateAllTokens();
		event.respond("Everyone's Gnome Panel login tokens have been invalidated!");
	}
}