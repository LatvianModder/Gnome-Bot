package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public class AboutCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("about")
			.description("Info about Gnome Bot")
			.add(sub("gnome")
					.description("Info about Gnome Bot")
					.run(AboutCommand::gnome)
			)
			.add(sub("macro")
					.description("Info about macros")
					.run(AboutCommand::macro)
			)
			.add(sub("pings")
					.description("Info about pings")
					.run(PingsCommands::help)
			)
			.add(sub("regex")
					.description("Info about Regular Expressions")
					.run(PingsCommands::regexHelp)
			);

	private static void gnome(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		long s = System.currentTimeMillis() - Date.from(event.getTimestamp()).getTime();

		List<String> content = new ArrayList<>();
		content.add("[Gnome Panel](<" + App.url("") + ">)");
		content.add("Last restart: " + Utils.formatRelativeDate(App.START_INSTANT));
		content.add("Gnome Response time: **" + s + " ms**");

		if (event.context.gc != null) {
			content.add("Legacy command prefix: **" + event.context.gc.legacyPrefix + "**");
			content.add("Macro prefix: **" + event.context.gc.macroPrefix + "**");
		}

		event.respond(content);
	}

	private static void macro(ChatInputInteractionEventWrapper event) {
		event.respond(MacroCommand.HELP);
	}
}