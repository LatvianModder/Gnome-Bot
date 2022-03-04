package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RegisterCommand;

public class CLIPrintDMs {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("print_dms")
			.description("Prints bot DMs with user")
			.trustedOnly()
			.run(CLIPrintDMs::run);

	private static void run(CLIEvent event) {
		event.respond("WIP!");

		/*
		PrivateChannel channel;
		try {
			channel = DM.open(discordHandler.getUser(Snowflake.of(matcher.group(1))));
			Snowflake lastId = channel.getLastMessageId().orElse(null);

			if (lastId == null) {
				error("No DMs!");
				return;
			}

			Table table = new Table("From", "Timestamp", "Title", "Content");

			Message message = channel.getMessageById(lastId).block();

			if (!message.getContent().isEmpty()) {
				table.addRow(message.getUserData().id().asString(), message.getTimestamp().toString(), "", message.getContent());
			}

			for (Embed embed : message.getEmbeds()) {
				table.addRow(message.getUserData().id().asString(), message.getTimestamp().toString(), embed.getTitle(), embed.getDescription());
			}

			for (Message message1 : channel.getMessagesBefore(lastId).toIterable()) {
				if (!message1.getContent().isEmpty()) {
					table.addRow(message1.getUserData().id().asString(), message1.getTimestamp().toString(), "", message1.getContent());
				}

				for (Embed embed : message1.getEmbeds()) {
					table.addRow(message1.getUserData().id().asString(), message1.getTimestamp().toString(), embed.getTitle(), embed.getDescription());
				}
			}

			table.print();
			Files.write(AppPaths.DATA_GUILDS.resolve("dms-" + matcher.group(1) + ".txt"), table.getCSVBytes(false));
		} catch (Exception e) {
			e.printStackTrace();
		}
		 */
	}
}
