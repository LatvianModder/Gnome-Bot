package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ContentType;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.User;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MacroCommands extends ApplicationCommands {
	public static final String COMPLEX_PLACEHOLDER = "For info about Complex Messages run /about macro";
	public static final String HELP = """
			Extras allow you to add buttons to your macro, change it into script or embed, etc. List of available properties:
							
			`clear`
			Remove all extras when editing
							
			`hidden`
			/macro list will not show this macro
							
			`embed ["title"] [#RRGGBB]`
			Changes macro into embed with optional title
							
			`embed_field <"name"> <"value">`
			Changes macro into embed with optional title
							
			`inline_embed_field <"title"> <"value">`
			Changes macro into embed with optional title
							
			`script <js>`
			Instead of printing text, it runs Text as script instead (WIP!)
							
			`url <"name"> <"url">`
			Adds a URL button
							
			`macro <"name"> <macro> [gray|blurple|green|red] [emoji]`
			Adds a macro button (creates new ephemeral message)
							
			`edit_macro <"name"> <macro> [gray|blurple|green|red] [emoji]`
			Adds a macro button (edits original message)
							
			`newrow`
			Adds new component row (not required for first row)
			""";

	public static final Pattern FORMAT_ESCAPE = Pattern.compile("([*_~@])");

	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("macro")
			.description("Manage macros")
			.add(sub("add")
					.add(string("name").required())
					.run(MacroCommands::add)
			)
			.add(sub("edit")
					.add(string("name").required().suggest(MacroCommands::suggestOwnMacro))
					.run(MacroCommands::edit)
			)
			.add(sub("remove")
					.add(string("name").required().suggest(MacroCommands::suggestOwnMacro))
					.run(MacroCommands::remove)
			)
			.add(sub("find")
					.add(user("author"))
					.add(string("name"))
					.add(string("content"))
					.add(bool("include-hidden"))
					.run(MacroCommands::find)
			)
			.add(sub("info")
					.add(string("name").required().suggest(MacroCommands::suggestAnyMacro))
					.run(MacroCommands::info)
			);

	private static void suggestAnyMacro(ChatCommandSuggestionEvent event) {
		for (Macro macro : event.context.gc.getMacroMap().values()) {
			event.suggestions.add(macro.getChatCommandSuggestion());
		}
	}

	private static void suggestOwnMacro(ChatCommandSuggestionEvent event) {
		boolean admin = event.context.isAdmin();

		for (Macro macro : event.context.gc.getMacroMap().values()) {
			if (admin || macro.author == event.context.sender.getId().asLong()) {
				event.suggestions.add(macro.getChatCommandSuggestion());
			}
		}
	}

	private static void add(ChatInputInteractionEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		} else if (name.length() > 50) {
			throw new GnomeException("Macro name too long! Max 50 characters.");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro != null) {
			if (macro.author == event.context.sender.getId().asLong()) {
				event.respondModal("edit-macro/" + macro.id, "Editing macro '" + macro.name + "'",
						TextInput.small("rename", "Rename", 1, 50).required(false).prefilled(macro.name),
						TextInput.paragraph("content", "Content").prefilled(ContentType.encodeMentions(macro.content))
				);

				return;
			} else {
				throw new GnomeException("Macro with that name already exists!");
			}
		}

		event.respondModal("add-macro/" + name, "Adding macro '" + name + "'",
				TextInput.paragraph("content", "Content")
		);
	}

	public static void addMacroCallback(ModalEventWrapper event, String name) {
		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		} else if (name.length() > 50) {
			throw new GnomeException("Macro name too long! Max 50 characters.");
		}

		if (event.context.gc.getMacro(name) != null) {
			throw new GnomeException("Macro with that name already exists!");
		}

		var content = ContentType.decodeMentions(event.get("content").asString());

		if (content.isEmpty()) {
			throw new GnomeException("Can't have empty content!");
		}

		var macro = new Macro(event.context.gc);
		macro.id = name.toLowerCase();
		macro.name = name;
		macro.content = content;
		macro.author = event.context.sender.getId().asLong();
		macro.created = Instant.now();
		macro.uses = 0;

		event.context.gc.getMacroMap().put(macro.id, macro);
		event.context.gc.saveMacroMap();

		event.respond(MessageBuilder.create("Macro '" + name + "' created!").ephemeral(false));
	}

	private static void edit(ChatInputInteractionEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro not found!");
		}

		event.respondModal("edit-macro/" + macro.id, "Editing macro '" + macro.name + "'",
				TextInput.small("rename", "Rename", 1, 50).required(false).prefilled(macro.name),
				TextInput.paragraph("content", "Content").prefilled(ContentType.encodeMentions(macro.content))
		);
	}

	public static void editMacroCallback(ModalEventWrapper event, String name) {
		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro not found!");
		} else if (macro.author != event.context.sender.getId().asLong() && !event.context.isAdmin()) {
			throw new GnomeException("You can only edit your own macros!");
		}

		String rename = event.get("rename").asString(macro.name);

		if (!rename.equals(macro.name)) {
			if (rename.length() > 50) {
				throw new GnomeException("Macro name too long! Max 50 characters.");
			}

			if (event.context.gc.getMacro(rename) != null) {
				throw new GnomeException("Macro with that name already exists!");
			}

			macro.rename(rename);
		}

		macro.updateContent(ContentType.decodeMentions(event.get("content").asString(macro.content)));

		event.context.gc.saveMacroMap();
		event.context.channelInfo.createMessage(event.context.sender.getMention() + " updated macro " + macro.chatFormatted(true) + "!").block();

		var preview = macro.createMessage(null, event.context.sender.getId());
		preview.content("Preview:\n\n" + preview.getContent());
		preview.ephemeral(true);
		event.respond(preview);
	}

	private static void remove(ChatInputInteractionEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro not found!");
		} else if (macro.author != event.context.sender.getId().asLong()) {
			event.context.checkSenderAdmin();
		}

		event.acknowledge();

		String s = "Macro '" + macro.name + "' removed!";

		try {
			macro.setSlashCommand(false);
		} catch (Exception ex) {
			ex.printStackTrace();
			s += "\nWith error: " + ex;
		}

		event.context.gc.getMacroMap().remove(macro.id);
		event.context.gc.saveMacroMap();
		event.respond(s);
	}

	private static void find(ChatInputInteractionEventWrapper event) {
		event.acknowledge();
		User author = event.get("author").asUser().orElse(null);
		String nameStr = event.get("name").asString().toLowerCase();
		var name = nameStr.isEmpty() ? null : Pattern.compile(nameStr, Pattern.CASE_INSENSITIVE);
		String contentStr = event.get("content").asString().toLowerCase();
		var content = contentStr.isEmpty() ? null : Pattern.compile(contentStr, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		boolean includeHidden = event.get("include-hidden").asBoolean(false);

		var list = new ArrayList<Macro>();

		for (var macro : event.context.gc.getMacroMap().values()) {
			if (author == null || macro.author == author.getId().asLong()) {
				if (name == null || name.matcher(macro.name).find()) {
					if (content == null || content.matcher(macro.content).find()) {
						if (includeHidden || !macro.isHidden()) {
							list.add(macro);
						}
					}
				}
			}
		}

		list.sort(null);

		if (list.isEmpty()) {
			event.respond("No macros found!");
		} else {
			event.respond(list.stream().map(Macro::chatFormatted).collect(Collectors.joining(" • ")));
		}
	}

	private static void info(ChatInputInteractionEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro not found!");
		}

		event.acknowledge();
		List<String> list = new ArrayList<>();
		list.add("Author: <@" + Snowflake.of(macro.author).asString() + ">");
		list.add("Created: " + Utils.formatRelativeDate(macro.created));
		list.add("Uses: " + macro.uses);
		event.respond(EmbedBuilder.create().url(App.url("panel/" + event.context.gc.guildId.asString() + "/macros/" + macro.id)).title("Macro '" + macro.name + "'").description(list));
	}

	public static void macroButtonCallback(ComponentEventWrapper event, String name, @Nullable Snowflake owner) {
		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro '" + name + "' not found!");
		}

		if (owner != null) {
			if (owner.asLong() != event.context.sender.getId().asLong()) {
				event.acknowledge();
			} else {
				macro.addUse();
				event.edit().respond(macro.createMessage(null, owner).ephemeral(true));
			}
		} else {
			macro.addUse();
			event.respond(macro.createMessage(null, event.context.sender.getId()).ephemeral(true));
		}
	}
}