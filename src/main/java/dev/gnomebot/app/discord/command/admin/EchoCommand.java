package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.latvian.apps.ansi.log.Log;
import discord4j.rest.util.Permission;
import org.bson.Document;

public class EchoCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkSenderPerms(Permission.MANAGE_MESSAGES);
		var message = event.get("message").asString();

		if (message.isEmpty()) {
			throw error("Empty content!");
		}

		var msg = event.context.reply(message);

		event.context.gc.echoLog.insert(new Document("_id", msg.getId().asLong())
				.append("channel", event.context.channelInfo.id)
				.append("author", event.context.sender.getId().asLong())
				.append("content", message)
		);

		Log.info(event.context.sender.getUsername() + " echoed: " + message);
		event.respond("Echo!");
	}
}
