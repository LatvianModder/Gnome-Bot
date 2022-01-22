package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.server.AuthLevel;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class AddCustomCommand {
	private static final Pattern INVALID_NAME = Pattern.compile("\\W", Pattern.CASE_INSENSITIVE);

	@LegacyDiscordCommand(name = "add_custom_command", help = "Adds a custom command", arguments = "<command name> <commands in each new line...>", permissionLevel = AuthLevel.ADMIN)
	public static final CommandCallback COMMAND = (context, reader) -> {
		String commandName = reader.readString().orElse("").toLowerCase();

		if (commandName.isEmpty() || INVALID_NAME.matcher(commandName).find()) {
			throw new DiscordCommandException("Invalid command name!");
		} else if (DiscordCommandImpl.COMMAND_MAP.containsKey(commandName)) {
			throw new DiscordCommandException("Gnome command already exists!");
		} else if (context.gc.customCommands.query().eq("command_name", commandName).firstDocument() != null) {
			throw new DiscordCommandException("Custom command already exists!");
		}

		Document document = new Document();
		document.put("command_name", commandName);
		document.put("author", context.sender.getId().asLong());
		document.put("created", context.message.getId().getTimestamp());
		document.put("permission_level", 0);

		List<String> commandList = new ArrayList<>();

		for (String s : reader.readRemainingStringLines()) {
			if (!s.isEmpty()) {
				commandList.add(s.trim());
			}
		}

		document.put("command_list", commandList);
		context.gc.customCommands.insert(document);
		context.upvote();
	};
}
