package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.util.ArrayList;
import java.util.List;
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
		event.acknowledgeEphemeral();

		Optional<ChannelInfo> c = event.context.gc.reportChannel.messageChannel();

		if (c.isEmpty()) {
			throw new DiscordCommandException("Report channel not set up!");
		}

		Message m = event.context.findMessage(event.get("message").asSnowflake()).orElse(null);

		if (m == null) {
			throw new DiscordCommandException("Message not found... __What.__");
		}

		User author = m.getAuthor().orElse(null);

		if (author == null) {
			throw new DiscordCommandException("Can't report non-members!");
		}

		if (!event.context.gc.isGnomeland() && author.getId().equals(event.context.sender.getId())) {
			throw new DiscordCommandException("You can't report your own messages!");
		} else if (author.isBot()) {
			throw new DiscordCommandException("You can't report bot messages!");
		}

		Member authorMember = null;

		try {
			authorMember = author.asMember(event.context.gc.guildId).block();
		} catch (Exception ex) {
		}

		if (authorMember == null) {
			throw new DiscordCommandException("Can't report non-members!");
		} else if (!event.context.gc.isGnomeland() && event.context.gc.getAuthLevel(authorMember).is(AuthLevel.ADMIN)) {
			throw new DiscordCommandException("You can't report admin messages!");
		}

		CachedRole role = event.context.gc.reportMentionRole.getRole();

		event.respond(msg -> {
			if (role == null) {
				msg.content("Select reason for reporting this message:");
			} else {
				msg.content("Select reason for reporting this message: (<@&" + role.id.asString() + "> will be pinged)");
			}

			List<SelectMenu.Option> options = new ArrayList<>();
			options.add(SelectMenu.Option.of("Cancel", "-"));

			for (String s : event.context.gc.reportOptions.get().split(" \\| ")) {
				options.add(SelectMenu.Option.of(s, s));
			}

			options.add(SelectMenu.Option.of("Other", "Other"));
			msg.addComponent(ActionRow.of(SelectMenu.of("report/" + m.getChannelId().asString() + "/" + m.getId().asString(), options).withPlaceholder("Select Reason...")).getData());
		});
	}
}
