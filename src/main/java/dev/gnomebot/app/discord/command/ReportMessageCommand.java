package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class ReportMessageCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("Report Message")
			.messageInteraction()
			.run(ReportMessageCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		Optional<ChannelInfo> c = event.context.gc.reportChannel.messageChannel();

		if (c.isEmpty()) {
			throw new DiscordCommandException("Report channel not set up!");
		}

		Message m = event.context.findMessage(event.get("message").asSnowflake()).orElse(null);

		if (m == null) {
			throw new DiscordCommandException("Message not found... __What.__");
		}

		User user = m.getAuthor().orElse(null);

		if (user == null) {
			throw new DiscordCommandException("Can't report non-members!");
		}

		if (user.getId().equals(event.context.sender.getId())) {
			throw new DiscordCommandException("You can't report your own messages!");
		} else if (user.isBot()) {
			throw new DiscordCommandException("You can't report bot messages!");
		}

		Member member = null;

		try {
			member = user.asMember(event.context.gc.guildId).block();
		} catch (Exception ex) {
		}

		if (member == null) {
			throw new DiscordCommandException("Can't report non-members!");
		} else if (event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw new DiscordCommandException("You can't report admin messages!");
		}

		ReportCommand.presentModal(event, m.getChannelId(), member);
	}
}
