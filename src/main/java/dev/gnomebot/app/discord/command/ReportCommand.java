package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Member;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class ReportCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("report")
			.description("Report a user to the admins")
			.add(user("user").required())
			.run(ReportCommand::run);

	private static void run(ApplicationCommandEventWrapper event) {
		if (!event.context.gc.reportChannel.isSet()) {
			throw new GnomeException("Report channel is not set up!");
		}

		Member member = event.get("user").asMember().orElse(null);

		if (member == null) {
			throw new GnomeException("Can't report non-members!");
		} else if (member.getId().equals(event.context.sender.getId())) {
			throw new GnomeException("You can't report your own messages!");
		} else if (member.isBot()) {
			throw new GnomeException("You can't report bot messages!");
		} else if (event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw new GnomeException("You can't report admin messages!");
		}

		presentModal(event, event.context.channelInfo.id, member);
	}

	public static void presentModal(DeferrableInteractionEventWrapper<?> event, Snowflake channel, Member member) {
		List<SelectMenu.Option> options = new ArrayList<>();

		for (String s : event.context.gc.reportOptions.get().split(" \\| ")) {
			options.add(SelectMenu.Option.of(s, s));
		}

		options.add(SelectMenu.Option.of("Other", "Other"));

		event.respondModal("report/" + channel.asString() + "/" + member.getId().asString(), "Report " + member.getDisplayName(),
				// SelectMenu.of("reason", options).withMinValues(1).withPlaceholder("Select Reason..."),
				TextInput.paragraph("additional_info", "Additional Info", "You can write additional info here").required(false)
		);
	}
}
