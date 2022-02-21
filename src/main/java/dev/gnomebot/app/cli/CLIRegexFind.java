package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLIRegexFind {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("regex_find")
			.description("Find messages with regex")
			.run(CLIRegexFind::run);

	private static void run(CLIEvent event) {
		event.respond("WIP!");

		/*
		Pattern pattern = Pattern.compile(matcher.group(1), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		UserCache cache = discordHandler.createUserCache();

		for (DiscordMessage m : gc.messages.query().regex("content", pattern).limit(100).descending("timestamp")) {
			info(Ansi.CYAN + m.getUIDSnowflake().asString() + " / " + m.getDate().toInstant() + Ansi.GREEN + " #" + "unknown" + " " + Ansi.YELLOW + cache.get(Snowflake.of(m.getUserID())).get().getUsername() + ": " + Ansi.RESET + m.getContent());
		}
		 */
	}
}
