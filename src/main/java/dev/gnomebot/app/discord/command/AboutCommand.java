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
	@RootCommand
	public static final CommandBuilder COMMAND = root("about")
			.description("Info about Gnome Bot")
			.add(sub("gnome")
					.description("Info about Gnome Bot")
					.run(AboutCommand::gnome)
			)
			.add(sub("macro")
					.description("Info about macros")
					.run(AboutCommand::macro)
			);

	private static void gnome(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		long s = System.currentTimeMillis() - Date.from(event.getTimestamp()).getTime();

		List<String> content = new ArrayList<>();
		content.add("[Gnome Panel](<" + App.url("") + ">)");
		content.add("Last restart: " + Utils.formatRelativeDate(App.START_INSTANT));
		content.add("Gnome Response time: **" + s + " ms**");

		if (event.context.gc != null) {
			content.add("Chat command prefix: **" + event.context.gc.prefix + "**");
			content.add("Macro prefix: **" + event.context.gc.macroPrefix + "**");
		}

		event.respond(content);
	}

	private static void macro(ApplicationCommandEventWrapper event) {
		event.respond("""
				Extras allow you to add buttons to your macro, change it into script or embed, etc. List of available properties:
								
				clear - Remove all extras when editing
				hidden - /macro list will not show this macro
				embed [title] - Changes macro into embed with optional title
				script <js> - Instead of printing text, it runs Text as script instead (WIP!)
				edit <macro> - Edits original message to new macro
				macro <name> <macro> - Adds a macro button
				url <name> <url> - Adds a URL button
				newrow - Adds new component row
				""");
	}
}
