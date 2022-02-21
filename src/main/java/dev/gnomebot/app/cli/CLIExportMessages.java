package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLIExportMessages {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("export_messages")
			.description("Export messages")
			.run(CLIExportMessages::run);

	private static void run(CLIEvent event) {
		event.respond("WIP!");

		/*
		long id = gc.getUserID(matcher.group(1));
		LinkedList<ExportedMessage> list = new LinkedList<>();

		for (DiscordMessage m : gc.messages.query().eq("user", id)) {
			if (list.size() % 10000 == 0) {
				info("Gathered " + list.size() + " so far...");
			}

			ExportedMessage message = new ExportedMessage();
			message.timestamp = m.getDate().getTime();
			message.channel = m.getChannelID();
			message.flags = m.flags;
			message.content = m.getContent();
			list.add(message);
		}

		info("Done gathering messages! Sorting...");

		list.sort(ExportedMessage.COMPARATOR);

		info("Done sorting! Saving to file...");

		CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
		try (OutputStream out = Files.newOutputStream(AppPaths.DATA_GUILDS.resolve(id + ".csv"));
			 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
			for (ExportedMessage line : list) {
				writer.append(line.toString());
				writer.newLine();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		info("Done!");
		 */
	}
}
