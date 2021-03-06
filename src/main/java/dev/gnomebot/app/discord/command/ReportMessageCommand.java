package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class ReportMessageCommand extends ApplicationCommands {
	@RegisterCommand
	public static final MessageInteractionBuilder COMMAND = messageInteraction("Report Message")
			.run(ReportMessageCommand::run);

	private static void run(MessageInteractionEventWrapper event) throws Exception {
		Optional<ChannelInfo> c = event.context.gc.reportChannel.messageChannel();

		if (c.isEmpty()) {
			throw new GnomeException("Report channel not set up!");
		}

		Message m = event.message;

		if (m == null) {
			throw new GnomeException("Message not found... __What.__");
		}

		User user = m.getAuthor().orElse(null);

		if (user == null) {
			throw new GnomeException("Can't report non-members!");
		}

		if (user.getId().equals(event.context.sender.getId())) {
			throw new GnomeException("You can't report your own messages!");
		} else if (user.isBot()) {
			throw new GnomeException("You can't report bot messages!");
		}

		Member member = null;

		try {
			member = user.asMember(event.context.gc.guildId).block();
		} catch (Exception ex) {
		}

		if (member == null) {
			throw new GnomeException("Can't report non-members!");
		} else if (event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw new GnomeException("You can't report admin messages!");
		}

		ReportCommand.presentModal(event, m.getChannelId(), member);
	}
}
