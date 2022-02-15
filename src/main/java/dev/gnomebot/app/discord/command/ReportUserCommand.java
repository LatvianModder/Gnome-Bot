package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.entity.Member;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class ReportUserCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("Report User")
			.userInteraction()
			.run(ReportUserCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		Optional<ChannelInfo> c = event.context.gc.reportChannel.messageChannel();

		if (c.isEmpty()) {
			throw new DiscordCommandException("Report channel not set up!");
		}

		Member member = event.get("user").asMember().orElse(null);

		if (member == null) {
			throw new DiscordCommandException("Can't report non-members!");
		} else if (member.getId().equals(event.context.sender.getId())) {
			throw new DiscordCommandException("You can't report your own messages!");
		} else if (member.isBot()) {
			throw new DiscordCommandException("You can't report bot messages!");
		} else if (event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw new DiscordCommandException("You can't report admin messages!");
		}

		ReportCommand.presentModal(event, event.context.channelInfo.id, member);
	}
}
