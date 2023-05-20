package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import discord4j.rest.util.Permission;

/**
 * @author LatvianModder
 */
public class EchoCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("echo")
			.description("Sends message back as bot")
			.add(string("message").required())
			.run(EchoCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkSenderPerms(Permission.MANAGE_MESSAGES);
		String message = event.get("message").asString();

		if (message.isEmpty()) {
			throw error("Empty content!");
		}

		event.context.reply(message);

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.ECHO)
				.channel(event.context.channelInfo.id.asLong())
				.user(event.context.sender)
				.content(message)
		);

		event.respond("Echo!");
	}
}
