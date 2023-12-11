package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.entity.Member;

import java.util.Optional;

public class ReportUserCommand extends ApplicationCommands {
	@RegisterCommand
	public static final UserInteractionBuilder COMMAND = userInteraction("Report User")
			.run(ReportUserCommand::run);

	private static void run(UserInteractionEventWrapper event) {
		Optional<ChannelInfo> c = event.context.gc.reportChannel.messageChannel();

		if (c.isEmpty()) {
			throw new GnomeException("Report channel not set up!");
		}

		Member member = event.getMember();

		if (member == null) {
			throw new GnomeException("Can't report non-members!");
		} else if (member.getId().equals(event.context.sender.getId())) {
			throw new GnomeException("You can't report your own messages!");
		} else if (member.isBot()) {
			throw new GnomeException("You can't report bot messages!");
		} else if (event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw new GnomeException("You can't report admin messages!");
		}

		ReportCommand.presentModal(event, event.context.channelInfo.id, member);
	}
}
