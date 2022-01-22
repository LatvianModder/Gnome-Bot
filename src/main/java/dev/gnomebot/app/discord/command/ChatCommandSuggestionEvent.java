package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GuildCollections;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;

import java.util.ArrayList;
import java.util.List;

public class ChatCommandSuggestionEvent extends ApplicationCommandEventWrapper {
	public final List<ChatCommandSuggestion> suggestions;

	public ChatCommandSuggestionEvent(GuildCollections gc, InteractionCreateEvent e, List<ApplicationCommandInteractionOption> o) {
		super(gc, e, o);
		suggestions = new ArrayList<>();
	}

	public void suggest(String name, Object value, int priority) {
		suggestions.add(new ChatCommandSuggestion(name, value, priority));
	}

	public void suggest(String name, Object value) {
		suggest(name, value, 0);
	}

	public void suggest(String value) {
		suggest(value, value, 0);
	}
}
