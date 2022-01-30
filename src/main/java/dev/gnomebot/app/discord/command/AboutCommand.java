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
			.run(AboutCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
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
}
