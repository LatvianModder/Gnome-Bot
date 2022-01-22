package dev.gnomebot.app.discord.command;

@FunctionalInterface
public interface AutoCompleteSuggestionCallback {
	void getSuggestions(ChatCommandSuggestionEvent event) throws Exception;
}
