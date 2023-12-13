package dev.gnomebot.app.cli;

public class CLIFindSlurs {
	public static final CLICommand COMMAND = CLICommand.make("find_slurs")
			.description("Find slurs")
			.run(CLIFindSlurs::run);

	private static void run(CLIEvent event) {
		event.respond("WIP!");

		/*
		printMessageTable(Arrays.asList(Filters.eq("user", gc.getUserID(matcher.group(1))), Filters.regex("content", gc.badWordRegex)), 1000);
		 */
	}
}
