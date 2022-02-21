package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.util.Utils;

import java.time.Instant;

/**
 * @author LatvianModder
 */
public class RemindMeCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("remindme")
			.description("Set a reminder")
			.add(string("text").required())
			.add(time("timer", false).description("Format can be '10 hours' or '5 days' etc"))
			.add(bool("private"))
			.run(RemindMeCommand::run);

	private static void run(ApplicationCommandEventWrapper event) {
		String text = event.get("text").asString();
		long timer = event.get("timer").asSeconds().orElse(60L * 60L);
		boolean dm = event.get("private").asBoolean(true);
		Instant instant = Instant.ofEpochSecond(Instant.now().getEpochSecond() + timer);

		if (dm) {
			event.acknowledgeEphemeral();
			dm = DM.send(event.context.handler, event.context.sender, "⏰ " + Utils.formatRelativeDate(instant) + ": " + text, false).isPresent();

			if (!dm) {
				event.respond("Failed to DM you! You must share at least one guild with Gnome where you have DMs open. You can specify `private:False` in command and you will receive reminder in this channel instead");
				return;
			}
		} else {
			event.acknowledge();
		}

		/*
		event.acknowledgeEphemeral();
		event.context.checkInChannel();
		event.context.checkSenderPerms(Permission.MANAGE_MESSAGES);
		String message = event.getString("message").orElse("");

		if (message.isEmpty()) {
			throw error("Empty content!");
		}

		event.context.reply(message);

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.ECHO)
				.channel(event.context.channelInfo.id)
				.user(event.context.sender)
				.content(message)
		);
		 */

		event.respond("(not actually true, its WIP) Reminder set! ⏰ " + Utils.formatRelativeDate(instant) + ": " + text);
	}
}
