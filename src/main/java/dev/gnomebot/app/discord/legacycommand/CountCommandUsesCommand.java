package dev.gnomebot.app.discord.legacycommand;

import com.mongodb.client.model.Filters;

/**
 * @author LatvianModder
 */
public class CountCommandUsesCommand {
	@LegacyDiscordCommand(name = "count_command_uses", arguments = "<command>")
	public static final CommandCallback COMMAND = (context, reader) -> {
		String s = reader.readString().orElse("");

		if (s.isEmpty()) {
			throw new DiscordCommandException("Requires command name!");
		}

		context.reply(s + ": " + context.gc.auditLog.count(Filters.and(Filters.eq("type", "command"), Filters.eq("old_content", s))));
	};
}
