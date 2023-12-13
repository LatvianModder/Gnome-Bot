package dev.gnomebot.app.cli;

public class CLIIsolateConversation {
	public static final CLICommand COMMAND = CLICommand.make("isolate_conversation")
			.description("Isolate conversation")
			.run(CLIIsolateConversation::run);

	private static void run(CLIEvent event) {
		event.respond("WIP!");

		/*
		List<Bson> userList = new ArrayList<>();

		for (String s : matcher.group(1).split(" ")) {
			userList.add(Filters.eq("user", gc.getUserID(s)));
		}

		printMessageTable(Collections.singletonList(Filters.or(userList)), 200);
		 */
	}
}
