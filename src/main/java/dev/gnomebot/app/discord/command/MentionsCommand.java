package dev.gnomebot.app.discord.command;

/**
 * @author LatvianModder
 */
public class MentionsCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("mentions")
			.description("Manage mentions")
			.add(sub("add")
					.add(string("pattern").required())
					.add(string("webhook_name"))
					.add(string("name"))
			)
			.add(sub("remove")
					.add(string("name").required())
			)
			.add(sub("list")
			);
}