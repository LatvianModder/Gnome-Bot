package dev.gnomebot.app.discord.legacycommand;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface CommandCallback {
	void run(CommandContext context, CommandReader reader) throws Exception;

	default boolean hasPermission(LegacyCommands command, CommandContext context) {
		return command.hasPermission(context);
	}
}