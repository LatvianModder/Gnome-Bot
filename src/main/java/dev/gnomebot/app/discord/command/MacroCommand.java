package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class MacroCommand extends ApplicationCommands {
	private static final String EXTRA_PLACEHOLDER = "For info about Extras run /about macro";

	private static final Pattern FORMAT_ESCAPE = Pattern.compile("([*_~@])");

	@RootCommand
	public static final CommandBuilder COMMAND = root("macro")
			.description("Manage macros")
			.add(sub("add")
					.add(string("name").required())
					.run(MacroCommand::add)
			)
			.add(sub("edit")
					.add(string("name").required().suggest(MacroCommand::suggestOwnMacro))
					.run(MacroCommand::edit)
			)
			.add(sub("remove")
					.add(string("name").required().suggest(MacroCommand::suggestOwnMacro))
					.run(MacroCommand::remove)
			)
			.add(sub("list")
					.add(user("author"))
					.add(string("name"))
					.run(MacroCommand::list)
			)
			.add(sub("info")
					.add(string("name").required().suggest(MacroCommand::suggestAnyMacro))
					.run(MacroCommand::info)
			)
			.add(sub("slash_command")
					.add(string("name").required().suggest(MacroCommand::suggestAnyMacro))
					.add(bool("enabled"))
					.run(MacroCommand::slashCommand)
			);

	private static void suggestAnyMacro(ChatCommandSuggestionEvent event) {
		for (Macro macro : event.context.gc.getMacroMap().values()) {
			event.suggestions.add(macro.getChatCommandSuggestion());
		}
	}

	private static void suggestOwnMacro(ChatCommandSuggestionEvent event) {
		boolean admin = event.context.isAdmin();

		for (Macro macro : event.context.gc.getMacroMap().values()) {
			if (admin || macro.getAuthor() == event.context.sender.getId().asLong()) {
				event.suggestions.add(macro.getChatCommandSuggestion());
			}
		}
	}

	private static void add(ApplicationCommandEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new DiscordCommandException("Macro name can't be empty!");
		} else if (name.length() > 50) {
			throw new DiscordCommandException("Macro name too long! Max 50 characters.");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro != null) {
			if (macro.getAuthor() == event.context.sender.getId().asLong()) {
				event.presentModal("edit_macro/" + macro.getName(), "Editing macro '" + macro.getName() + "'",
						TextInput.small("rename", "Rename", 1, 50).required(false).prefilled(macro.getName()),
						TextInput.paragraph("content", "Content").prefilled(macro.getContent()),
						TextInput.paragraph("extra", "Extra").required(false).prefilled(String.join("\n", macro.getExtra())).placeholder(EXTRA_PLACEHOLDER)
				);

				return;
			} else {
				throw new DiscordCommandException("Macro with that name already exists!");
			}
		}

		event.presentModal("add_macro/" + name, "Adding macro '" + name + "'",
				TextInput.paragraph("content", "Content"),
				TextInput.paragraph("extra", "Extra").required(false).placeholder(EXTRA_PLACEHOLDER)
		);
	}

	private static void edit(ApplicationCommandEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new DiscordCommandException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new DiscordCommandException("Macro not found!");
		} else if (macro.getAuthor() != event.context.sender.getId().asLong()) {
			event.context.checkSenderAdmin();
		}

		event.presentModal("edit_macro/" + macro.getName(), "Editing macro '" + macro.getName() + "'",
				TextInput.small("rename", "Rename", 1, 50).required(false).prefilled(macro.getName()),
				TextInput.paragraph("content", "Content").prefilled(macro.getContent()),
				TextInput.paragraph("extra", "Extra").required(false).prefilled(String.join("\n", macro.getExtra())).placeholder(EXTRA_PLACEHOLDER)
		);
	}

	private static void remove(ApplicationCommandEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new DiscordCommandException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new DiscordCommandException("Macro not found!");
		} else if (macro.getAuthor() != event.context.sender.getId().asLong()) {
			event.context.checkSenderAdmin();
		}

		event.acknowledge();
		macro.setSlashCommand(false);
		macro.delete();
		event.context.gc.updateMacroMap();
		event.respond("Macro '" + macro.getName() + "' removed!");
	}

	private static void list(ApplicationCommandEventWrapper event) {
		event.acknowledge();
		User author = event.get("author").asUser().orElse(null);
		String name = event.get("name").asString().toLowerCase();

		List<String> list = new ArrayList<>();

		for (Macro macro : event.context.gc.getMacroMap().values()) {
			if (author == null || macro.getAuthor() == author.getId().asLong()) {
				if (name.isEmpty() || macro.getName().toLowerCase().contains(name)) {
					if (!macro.getExtra().contains("hidden")) {
						list.add(macro.getName());
					}
				}
			}
		}

		list.sort(String.CASE_INSENSITIVE_ORDER);

		if (list.isEmpty()) {
			event.respond("No macros found!");
		} else {
			event.respond(list.stream().map(s -> FORMAT_ESCAPE.matcher(s).replaceAll("\\\\$1")).collect(Collectors.joining(" • ")));
		}
	}

	private static void info(ApplicationCommandEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new DiscordCommandException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new DiscordCommandException("Macro not found!");
		}

		event.acknowledge();
		List<String> list = new ArrayList<>();
		list.add("Author: <@" + Snowflake.of(macro.getAuthor()).asString() + ">");
		list.add("Created: " + Utils.formatRelativeDate(macro.getDate().toInstant()));
		list.add("Uses: " + macro.getUses());
		event.respond(EmbedBuilder.create().title("Macro '" + macro.getName() + "'").description(list));
	}

	private static void slashCommand(ApplicationCommandEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new DiscordCommandException("Macro name can't be empty!");
		}

		event.context.checkSenderAdmin();

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new DiscordCommandException("Macro not found!");
		}

		event.acknowledge();

		boolean enabled = event.get("enabled").asBoolean(true);
		macro.setSlashCommand(enabled);
		event.context.gc.updateMacroMap();

		if (enabled) {
			event.respond("Slash command for " + macro.getName() + " enabled");
		} else {
			event.respond("Slash command for " + macro.getName() + " disabled");
		}
	}
}