package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.WebToken;
import dev.gnomebot.app.util.Utils;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

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
		WebToken token = event.context.gc.db.webTokens.query().eq("user", event.context.sender.getId().asLong()).first();
		String tokenString;

		if (token == null) {
			tokenString = Utils.createToken();
			Document document = new Document();
			document.put("_id", tokenString);
			document.put("user", event.context.sender.getId().asLong());
			document.put("name", event.context.sender.getUsername());
			document.put("created", new Date());
			event.context.gc.db.webTokens.insert(document);
		} else {
			tokenString = token.getUIDString();
		}

		event.respond("[Click here to open the panel!](<" + App.url("panel/login?token=" + Base64.getUrlEncoder().encodeToString(tokenString.getBytes(StandardCharsets.UTF_8))) + ">)");
	}

	private static void logout(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		App.instance.db.webTokens.query().eq("user", event.context.sender.getId().asLong()).many().delete();
		event.respond("Your Gnome Panel login tokens have been invalidated!");
	}

	private static void logoutEveryone(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderTrusted();
		App.instance.db.webTokens.drop();
		event.respond("Everyone's Gnome Panel login tokens have been invalidated!");
	}
}