package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.cli.CLI;
import dev.gnomebot.app.cli.CLICommand;
import dev.gnomebot.app.cli.CLIEvent;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Pair;
import discord4j.core.object.entity.Member;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class CLIApplicationCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("cli")
			.description("Run a CLI command")
			.add(string("command")
					.suggest(CLIApplicationCommand::suggestCommands)
					.required()
			)
			.run(CLIApplicationCommand::run);

	private static class CLIEventFromCommand extends CLIEvent {
		private final List<String> response = new ArrayList<>();
		private Pair<String, byte[]> file = null;

		public CLIEventFromCommand(GuildCollections g, Member m, CommandReader r) {
			super(g, m, r);
		}

		@Override
		public void respond(String text) {
			response.add(text);
		}

		@Override
		public void respondFile(String text, String filename, byte[] data) {
			response.add(text);
			file = Pair.of(filename, data);
		}
	}

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();

		CommandReader reader = new CommandReader(event.context.gc, event.get("command").asString());

		CLICommand command = CLI.COMMANDS.get(reader.readString().orElse(""));

		if (command == null) {
			throw error("Command not found!");
		} else if (command.trusted) {
			if (!event.context.isTrusted()) {
				throw new DiscordCommandException("Only trusted users can use this command!");
			}
		} else if (command.admin) {
			event.context.checkSenderAdmin();
		}

		CLIEventFromCommand event1 = new CLIEventFromCommand(event.context.gc, event.context.sender, reader);

		try {
			command.callback.run(event1);
		} catch (DiscordCommandException ex) {
			event1.respond(ex.getMessage());
		} catch (Exception ex) {
			event1.respond(ex.toString());
		}

		if (event1.response.isEmpty()) {
			event.respond("No response");
		} else if (event1.file != null) {
			event.respondFile(builder -> builder.content(String.join("\n", event1.response)), event1.file.a, event1.file.b);
		} else {
			event.respond(String.join("\n", event1.response));
		}
	}

	private static void suggestCommands(ChatCommandSuggestionEvent event) {
		for (CLICommand command : CLI.COMMANDS.values()) {
			if (command.trusted && !event.context.isTrusted()) {
				continue;
			}

			if (command.admin && !event.context.isAdmin()) {
				continue;
			}

			event.suggest(command.name);
		}
	}
}
