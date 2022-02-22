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
			)
			.add(sub("pings")
					.description("Info about pings")
					.run(AboutCommand::pings)
			);

	private static void gnome(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		long s = System.currentTimeMillis() - Date.from(event.getTimestamp()).getTime();

		List<String> content = new ArrayList<>();
		content.add("[Gnome Panel](<" + App.url("") + ">)");
		content.add("Last restart: " + Utils.formatRelativeDate(App.START_INSTANT));
		content.add("Gnome Response time: **" + s + " ms**");

		if (event.context.gc != null) {
			content.add("Legacy command prefix: **" + event.context.gc.prefix + "**");
			content.add("Macro prefix: **" + event.context.gc.macroPrefix + "**");
		}

		event.respond(content);
	}

	private static void macro(ApplicationCommandEventWrapper event) {
		event.respond("""
				Extras allow you to add buttons to your macro, change it into script or embed, etc. List of available properties:
								
				`clear`
				Remove all extras when editing
								
				`hidden`
				/macro list will not show this macro
								
				`embed ["title"] [#RRGGBB]`
				Changes macro into embed with optional title
								
				`embed_field <"name"> <"value">`
				Changes macro into embed with optional title
								
				`inline_embed_field <"title"> <"value">`
				Changes macro into embed with optional title
								
				`script <js>`
				Instead of printing text, it runs Text as script instead (WIP!)
								
				`url <"name"> <"url">`
				Adds a URL button
								
				`macro <"name"> <macro> [gray|blurple|green|red] [emoji]`
				Adds a macro button (creates new ephemeral message)
								
				`edit_macro <"name"> <macro> [gray|blurple|green|red] [emoji]`
				Adds a macro button (edits original message)
								
				`newrow`
				Adds new component row (not required for first row)
				""");
	}

	private static void pings(ApplicationCommandEventWrapper event) {
		event.respond(PingsCommand.HELP.replace("{USER}", event.context.sender.getUsername().toLowerCase()));
	}
}
