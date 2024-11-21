package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.InteractionEventWrapper;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ChatCommandSuggestionEvent extends InteractionEventWrapper<ChatInputAutoCompleteEvent> {
	public final Map<String, CommandOption> options;
	public final CommandOption focused;
	public final List<ChatCommandSuggestion> suggestions;
	public Function<String, String> transformSearch;

	public ChatCommandSuggestionEvent(App app, GuildCollections gc, ChatInputAutoCompleteEvent e, List<ApplicationCommandInteractionOption> o) {
		super(app, gc, e);

		options = new HashMap<>();
		CommandOption f = null;

		for (var option : o) {
			var o1 = new CommandOption(context, option);
			options.put(o1.name, o1);

			if (o1.focused) {
				f = o1;
			}
		}

		focused = f;

		suggestions = new ArrayList<>();
		transformSearch = s -> s.toLowerCase().trim();
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

	public void suggest(String name, Object value, int priority) {
		suggestions.add(new ChatCommandSuggestion(name, value, name.toLowerCase(), priority));
	}

	public void suggest(String name, Object value) {
		suggest(name, value, 0);
	}

	public void suggest(String value) {
		suggest(value, value, 0);
	}
}
