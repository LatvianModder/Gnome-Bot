package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.latvian.apps.ansi.log.Log;
import discord4j.rest.util.Permission;

public class EchoCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkSenderPerms(Permission.MANAGE_MESSAGES);
		var message = event.get("message").asString();

		if (message.isEmpty()) {
			throw error("Empty content!");
		}

		event.context.reply(message);

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.ECHO)
				.channel(event.context.channelInfo.id)
				.user(event.context.sender)
				.content(message)
		);

		Log.info(event.context.sender.getUsername() + " echoed: " + message);
		event.respond("Echo!");
	}
}
