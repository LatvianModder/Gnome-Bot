package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatInputInteractionEventWrapper extends ApplicationCommandInteractionEventWrapper<ChatInputInteractionEvent> {
	public final Map<String, CommandOption> options;

	public ChatInputInteractionEventWrapper(App app, GuildCollections gc, ChatInputInteractionEvent e, List<ApplicationCommandInteractionOption> o) {
		super(app, gc, e);
		options = new HashMap<>();

		for (var option : o) {
			var o1 = new CommandOption(context, option);
			options.put(o1.name, o1);
		}
	}

	@Override
	public String toString() {
		return event.getCommandName() + " " + options;
	}

	public boolean has(String id) {
		return options.containsKey(id);
	}

	public CommandOption get(String id) {
		var o = options.get(id);

		if (o == null) {
			return new CommandOption(context, id, "", false);
		}

		return o;
	}
}
