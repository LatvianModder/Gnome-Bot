package dev.gnomebot.app.discord.legacycommand;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.data.DiscordCustomCommand;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public class MacroCommand {
	@LegacyDiscordCommand(name = "macro", help = "Create and manage macros", arguments = "add <name> <content...> | remove <name> | edit <name> <content...> | info <name> | print <name> | convert <name> | list [author] | rename <name> <new name> | slash_command <name> <enabled>")
	public static final CommandCallback COMMAND = (context, reader) -> {
		String sub = reader.readString().orElse("");
		String name = reader.readString().orElse("");
		Macro first = context.gc.macros.query().eq("command_name", name.toLowerCase()).first();

		switch (sub) {
			case "create", "add", "-a" -> {
				if (first != null) {
					throw new DiscordCommandException("Macro with that name already exists!");
				} else if (name.isEmpty()) {
					throw new DiscordCommandException("Empty macro name? Bad.");
				}

				String s = reader.readRemainingString().orElse("")
						.replaceAll("<@&(\\d+)>", "role:$1")
						.replaceAll("<@(\\d+)>", "user:$1")
						.replace("@here", "mention:here")
						.replace("@everyone", "mention:everyone");

				if (s.isEmpty()) {
					throw new DiscordCommandException("Can't have empty content!");
				}

				Document document = new Document();
				document.put("name", name);
				document.put("command_name", name.toLowerCase());
				document.put("content", s);
				document.put("author", context.sender.getId().asLong());
				document.put("created", new Date());
				document.put("uses", 0);
				document.put("type", "text");
				context.gc.macros.insert(document);

				context.reply("Macro '" + name + "' created");
			}
			case "delete", "remove", "-r" -> {
				if (first == null) {
					throw new DiscordCommandException("Macro not found!");
				} else if (first.getAuthor() != context.sender.getId().asLong() && !context.isAdmin()) {
					throw new DiscordCommandException("You can't delete this macro!");
				}

				first.setSlashCommand(false);
				first.delete();
				context.upvote();
			}
			case "update", "edit", "-u" -> {
				if (first == null) {
					throw new DiscordCommandException("Macro not found!");
				} else if (first.getAuthor() != context.sender.getId().asLong() && !context.isAdmin()) {
					throw new DiscordCommandException("You can't edit this macro!");
				}

				String s = reader.readRemainingString().orElse("")
						.replaceAll("<@&(\\d+)>", "role:$1")
						.replaceAll("<@(\\d+)>", "user:$1")
						.replace("@here", "mention:here")
						.replace("@everyone", "mention:everyone");

				if (s.isEmpty()) {
					throw new DiscordCommandException("Can't have empty content!");
				}

				context.upvote();
				first.update(Updates.set("content", s));
			}
			case "source", "info", "-s", "-i" -> {
				if (first == null) {
					throw new DiscordCommandException("Macro not found!");
				}

				List<String> list = new ArrayList<>();
				list.add("Author: <@" + Snowflake.of(first.getAuthor()).asString() + ">");
				list.add("Created: " + Utils.formatRelativeDate(first.getDate().toInstant()));
				list.add("Uses: " + first.getUses());
				context.reply("Macro: " + first.getName(), String.join("\n", list));
			}
			case "test", "print" -> {
				if (first == null) {
					throw new DiscordCommandException("Macro not found!");
				}

				first.update(Updates.inc("uses", 1));
				context.reply("Macro: " + first.getName() + "\n```\n" + first.getContent() + "```");
			}
			case "convert" -> {
				if (first != null) {
					throw new DiscordCommandException("Macro with that name already exists!");
				} else if (name.isEmpty()) {
					throw new DiscordCommandException("Empty macro name? Bad.");
				} else if (!context.isAdmin()) {
					throw new DiscordCommandException("Only admins can convert custom commands into macros!");
				}

				String newName = reader.readString().orElse(name);

				DiscordCustomCommand command = context.gc.customCommands.query().eq("command_name", name).first();

				if (command == null) {
					throw new DiscordCommandException("Custom command not found!");
				}

				List<String> content = new ArrayList<>();

				for (String s : command.getCommandList()) {
					if (s.startsWith("reply ")) {
						content.add(s.substring(6));
					} else if (s.startsWith("reply_embed ")) {
						content.add(s.substring(12));
					}
				}

				if (content.isEmpty()) {
					throw new DiscordCommandException("Custom command doesnt have any reply commands!");
				}

				Document document = new Document();
				document.put("name", newName);
				document.put("command_name", newName.toLowerCase());
				document.put("content", String.join("\n", content).replace("\\\\n", "\n").replace("\\n", "\n"));
				document.put("author", context.sender.getId().asLong());
				document.put("created", new Date());
				document.put("uses", 0);
				document.put("type", "text");
				context.gc.macros.insert(document);

				context.reply("Macro '" + newName + "' created");
				command.delete();
			}
			case "list", "-l" -> {
				User author = reader.readUser().orElse(null);

				List<String> list = new ArrayList<>();
				(author == null ? context.gc.macros.query() : context.gc.macros.query().eq("author", author.getId().asLong())).forEach(m -> list.add(m.getName()));
				list.sort(String.CASE_INSENSITIVE_ORDER);
				context.reply(list.isEmpty() ? "-" : String.join(", ", list));
			}
			case "rename", "ren" -> {
				if (first == null) {
					throw new DiscordCommandException("Macro not found!");
				} else if (first.getAuthor() != context.sender.getId().asLong() && !context.isAdmin()) {
					throw new DiscordCommandException("You can't edit this macro!");
				}

				long id = first.setSlashCommand(false);
				String newName = reader.readString().orElse("");

				if (newName.isEmpty()) {
					throw new DiscordCommandException("Invalid name!");
				} else if (context.gc.macros.query().eq("command_name", newName.toLowerCase()).first() != null) {
					throw new DiscordCommandException("Name already taken!");
				}

				first.update("name", newName);
				first.update("command_name", newName.toLowerCase());

				if (id != 0L) {
					first.setSlashCommand(true);
				}

				context.reply("Macro " + first.getName() + " renamed to " + newName);
			}
			case "slash_command" -> {
				if (first == null) {
					throw new DiscordCommandException("Macro not found!");
				} else if (!context.isAdmin()) {
					throw new DiscordCommandException("You have to be admin to change macro slash command!");
				}

				boolean b = reader.readBoolean().orElse(true);
				first.setSlashCommand(b);

				if (b) {
					context.reply("Slash command for " + first.getName() + " enabled");
				} else {
					context.reply("Slash command for " + first.getName() + " disabled");
				}
			}
			default -> {
				throw new DiscordCommandException("Unknown subcommand! Valid commands: add, remove, edit, info, print, convert, list, rename");
			}
		}
	};
}
