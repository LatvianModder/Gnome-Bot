package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.Config;
import dev.gnomebot.app.cli.CLICommands;
import dev.gnomebot.app.cli.CLIEvent;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.latvian.apps.webutils.ansi.Log;

import java.util.ArrayList;
import java.util.List;

public class CLIApplicationCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("cli")
			.description("Run a CLI command")
			.add(string("command")
					.suggest(CLIApplicationCommand::suggestCommands)
					.required()
			)
			.add(string("arguments")
					.suggest(CLIApplicationCommand::suggestArguments)
			)
			.run(CLIApplicationCommand::run);

	public static class CLIEventFromCommand extends CLIEvent {
		public final ChatInputInteractionEventWrapper event;
		private final List<String> responseText = new ArrayList<>();

		public CLIEventFromCommand(ChatInputInteractionEventWrapper e, CommandReader r) {
			super(e.context.gc, e.context.sender, r);
			event = e;
		}

		@Override
		public void respond(String text) {
			responseText.add(text);
		}
	}

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		var command = CLICommands.COMMANDS.get(event.get("command").asString());

		if (command == null) {
			throw error("Command not found!");
		}

		if (command.ephemeral) {
			event.acknowledgeEphemeral();
		} else {
			event.acknowledge();
		}

		if (command.trusted == 1) {
			if (!event.context.isTrusted()) {
				throw new GnomeException("Only trusted users can use this command!");
			}
		} else if (command.trusted == 2) {
			if (Config.get().owner != event.context.sender.getId().asLong()) {
				throw new GnomeException("Only bot owner can use this command!");
			}
		}

		if (command.admin) {
			event.context.checkSenderAdmin();
		}

		var reader = new CommandReader(event.context.gc, event.get("arguments").asString());
		var event1 = new CLIEventFromCommand(event, reader);

		try {
			command.callback.run(event1);
		} catch (GnomeException ex) {
			event1.respond(ex.getMessage());
		} catch (Exception ex) {
			event1.respond(ex.toString());
		}

		if (event1.response != null) {
			if (!event1.responseText.isEmpty()) {
				event1.response.content(String.join("\n", event1.responseText));
			}

			event.respond(event1.response);
		}
	}

	private static void suggestCommands(ChatCommandSuggestionEvent event) {
		for (var command : CLICommands.COMMANDS.values()) {
			if (command.trusted == 1 && !event.context.isTrusted()) {
				continue;
			} else if (command.trusted == 2 && Config.get().owner != event.context.sender.getId().asLong()) {
				continue;
			} else if (command.admin && !event.context.isAdmin()) {
				continue;
			}

			event.suggest(command.name);
		}
	}

	private static void suggestArguments(ChatCommandSuggestionEvent event) {
		Log.info("Suggestions for " + event.get("command").asString("Unknown") + ": []");
	}
}
