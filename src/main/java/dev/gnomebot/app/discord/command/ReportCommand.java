package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Member;

import java.util.Collections;

/**
 * @author LatvianModder
 */
public class ReportCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("report")
			.description("Report a user to the admins")
			.add(user("user").required())
			.run(ReportCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws DiscordCommandException {
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

		presentModal(event, event.context.channelInfo.id, member);
	}

	public static void presentModal(DeferrableInteractionEventWrapper<?> event, Snowflake channel, Member member) {
		event.presentModal("report/" + channel.asString() + "/" + member.getId().asString(), "Report " + member.getDisplayName(), Collections.singletonList(ActionRow.of(TextInput.paragraph("additional_info", "Additional Info", "You can write additional info here").required(false))));
	}
}
