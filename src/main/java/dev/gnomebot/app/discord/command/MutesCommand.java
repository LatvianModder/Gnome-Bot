package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public class MutesCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("mutes")
			.description("Prints all mutes")
			.run(MutesCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		List<String> list = new ArrayList<>();

		for (DiscordMember member : event.context.gc.members.query().exists("muted").projectionFields("_id", "muted")) {
			Date muted = member.getMuted();

			if (muted != null) {
				list.add(member.getUIDString() + " - " + member.getTag() + " - " + Utils.formatRelativeDate(muted.toInstant()));
			}
		}

		event.respond(String.join("\n", list));
	}
}
