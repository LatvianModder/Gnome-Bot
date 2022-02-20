package dev.gnomebot.app.discord.command;

import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import org.jetbrains.annotations.NotNull;

public record ChatCommandSuggestion(String name, Object value, String match, int priority) implements Comparable<ChatCommandSuggestion> {
	public ApplicationCommandOptionChoiceData build() {
		return ApplicationCommandOptionChoiceData.builder().name(name).value(value).build();
	}

	@Override
	public int compareTo(@NotNull ChatCommandSuggestion o) {
		if (priority == o.priority) {
			return name.compareToIgnoreCase(o.name);
		}

		return Integer.compare(o.priority, priority);
	}
}
