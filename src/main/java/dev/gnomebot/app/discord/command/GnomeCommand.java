package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GnomeCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("gnome")
			.supportsDM()
			.add(sub("about")
					.description("Info about Gnome Bot")
					.run(GnomeCommand::about)
			)
			.add(subGroup("panel")
					.description("Panel commands")
					.add(sub("login")
							.description("Log in to the panel")
							.run(GnomeCommand::login)
					)
					.add(sub("logout")
							.description("Log out of the panel (Invalidates all your tokens)")
							.run(GnomeCommand::logout)
					)
			)
			// END
			;

	private static void about(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		var s = System.currentTimeMillis() - Date.from(event.getTimestamp()).getTime();

		List<String> content = new ArrayList<>();
		content.add("[Gnome Panel](<" + event.context.gc.db.app.url("") + ">)");
		content.add("Last restart: " + Utils.formatRelativeDate(App.START_INSTANT));
		content.add("Gnome Response time: **" + s + " ms**");

		if (event.context.gc != null) {
			content.add("Legacy command prefix: **" + event.context.gc.legacyPrefix + "**");
			content.add("Macro prefix: **" + event.context.gc.macroPrefix + "**");
		}

		event.respond(content);
	}

	private static void login(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		var tokenString = event.context.gc.db.getEncodedToken(event.context.sender.getId().asLong(), event.context.sender.getUsername());
		event.respond("[Click here to open the panel!](<" + event.context.gc.db.app.url("login?logintoken=" + tokenString) + ">) (Do not share this link with others!)");
	}

	private static void logout(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		App.instance.db.invalidateTokens(event.context.sender.getId().asLong());
		event.respond("All of your Gnome Panel login tokens have been invalidated!");
	}
}
