package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;

/**
 * @author LatvianModder
 */
public class NoteCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("note")
			.description("Adds note to member")
			.add(user("user").required())
			.add(string("note").required())
			.run(NoteCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkBotPerms(Permission.MODERATE_MEMBERS);
		event.context.checkSenderPerms(Permission.MODERATE_MEMBERS);

		User user = event.get("user").asUser().orElse(null);
		Member member = null;

		try {
			member = user == null ? null : user.asMember(event.context.gc.guildId).block();
		} catch (Exception ex) {
		}

		String note = event.get("note").asString();

		if (user == null) {
			throw error("User not found!");
		} else if (user.isBot() || member != null && event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw error("Nice try.");
		}

		event.context.gc.adminLogChannelEmbed(user.getUserData(), event.context.gc.adminLogChannel, spec -> {
			spec.description(note);
			spec.author("Note has been added to " + user.getUsername(), user.getAvatarUrl());
			spec.footer(event.context.sender.getUsername(), event.context.sender.getAvatarUrl());
		});

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.NOTE)
				.user(user)
				.source(event.context.sender)
				.content(note)
		);

		event.respond("Note added!");
	}
}
