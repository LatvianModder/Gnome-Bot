package dev.gnomebot.app.discord.command;

@FunctionalInterface
public interface SlashCommandCallback {
	SlashCommandCallback DEFAULT = event -> event.respond("Unknown command!");

	void run(ApplicationCommandEventWrapper event) throws Exception;
}
