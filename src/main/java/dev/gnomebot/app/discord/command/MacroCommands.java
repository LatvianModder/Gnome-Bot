package dev.gnomebot.app.discord.command;

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
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MacroCommands extends ApplicationCommands {
	public static final String EXTRA_PLACEHOLDER = "For info about Extras run /about macro";
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

	private static final Pattern FORMAT_ESCAPE = Pattern.compile("([*_~@])");

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
			.add(sub("list")
					.add(user("author"))
					.add(string("name"))
					.run(MacroCommands::list)
			)
			.add(sub("info")
					.add(string("name").required().suggest(MacroCommands::suggestAnyMacro))
					.run(MacroCommands::info)
			)
			.add(sub("slash_command")
					.add(string("name").required().suggest(MacroCommands::suggestAnyMacro))
					.add(bool("enabled"))
					.run(MacroCommands::slashCommand)
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

	private static void add(ChatInputInteractionEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		} else if (name.length() > 50) {
			throw new GnomeException("Macro name too long! Max 50 characters.");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro != null) {
			if (macro.getAuthor() == event.context.sender.getId().asLong()) {
				event.respondModal("edit_macro/" + macro.getName(), "Editing macro '" + macro.getName() + "'",
						TextInput.small("rename", "Rename", 1, 50).required(false).prefilled(macro.getName()),
						TextInput.paragraph("content", "Content").prefilled(macro.getContent()),
						TextInput.paragraph("extra", "Extra").required(false).prefilled(String.join("\n", macro.getExtra())).placeholder(EXTRA_PLACEHOLDER)
				);

				return;
			} else {
				throw new GnomeException("Macro with that name already exists!");
			}
		}

		event.respondModal("add_macro/" + name, "Adding macro '" + name + "'",
				TextInput.paragraph("content", "Content"),
				TextInput.paragraph("extra", "Extra").required(false).placeholder(EXTRA_PLACEHOLDER)
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

		String content = event.get("content").asString()
				.replaceAll("<@&(\\d+)>", "role:$1")
				.replaceAll("<@(\\d+)>", "user:$1")
				.replace("@here", "mention:here")
				.replace("@everyone", "mention:everyone");

		if (content.isEmpty()) {
			throw new GnomeException("Can't have empty content!");
		}

		List<String> extra = new ArrayList<>(Arrays.stream(event.get("extra").asString().trim().split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList());

		Document document = new Document();
		document.put("name", name);
		document.put("content", content);

		extra.remove("clear");

		if (!extra.isEmpty()) {
			document.put("extra", extra);
		}

		document.put("author", event.context.sender.getId().asLong());
		document.put("created", new Date());
		document.put("uses", 0);
		document.put("type", "text");
		event.context.gc.macros.insert(document);
		event.context.gc.updateMacroMap();

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

		event.respondModal("edit_macro/" + macro.getName(), "Editing macro '" + macro.getName() + "'",
				TextInput.small("rename", "Rename", 1, 50).required(false).prefilled(macro.getName()),
				TextInput.paragraph("content", "Content").prefilled(macro.getContent()),
				TextInput.paragraph("extra", "Extra").required(false).prefilled(String.join("\n", macro.getExtra())).placeholder(EXTRA_PLACEHOLDER)
		);
	}

	public static void editMacroCallback(ModalEventWrapper event, String name) {
		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		}

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro not found!");
		} else if (macro.getAuthor() != event.context.sender.getId().asLong() && !event.context.isAdmin()) {
			throw new GnomeException("You can only edit your own macros!");
		}

		String rename = event.get("rename").asString(macro.getName());

		if (!rename.equals(macro.getName())) {
			if (rename.length() > 50) {
				throw new GnomeException("Macro name too long! Max 50 characters.");
			}

			if (event.context.gc.getMacro(rename) != null) {
				throw new GnomeException("Macro with that name already exists!");
			}

			macro.rename(rename);
		}

		var content = event.get("content").asString(macro.getContent());
		var extra = Arrays.stream(event.get("extra").asString().trim().split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();

		macro.updateContent(content, extra);

		event.context.gc.updateMacroMap();

		if (macro.getSlashCommand() != 0L) {
			event.context.channelInfo.createMessage(event.context.sender.getMention() + " updated macro </" + macro.getName() + ":" + Long.toUnsignedString(macro.getSlashCommand()) + ">!").block();
		} else {
			event.context.channelInfo.createMessage(event.context.sender.getMention() + " updated macro `" + macro.getName() + "`!").block();
		}

		var preview = Macro.createMessage(content, extra, event.context.sender.getId(), false);
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
		} else if (macro.getAuthor() != event.context.sender.getId().asLong()) {
			event.context.checkSenderAdmin();
		}

		event.acknowledge();

		String s = "Macro '" + macro.getName() + "' removed!";

		try {
			macro.setSlashCommand(false);
		} catch (Exception ex) {
			ex.printStackTrace();
			s += "\nWith error: " + ex;
		}

		macro.delete();
		event.context.gc.updateMacroMap();
		event.respond(s);
	}

	private static void list(ChatInputInteractionEventWrapper event) {
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
		list.add("Author: <@" + Snowflake.of(macro.getAuthor()).asString() + ">");
		list.add("Created: " + Utils.formatRelativeDate(macro.getDate().toInstant()));
		list.add("Uses: " + macro.getUses());
		event.respond(EmbedBuilder.create().title("Macro '" + macro.getName() + "'").description(list));
	}

	private static void slashCommand(ChatInputInteractionEventWrapper event) {
		String name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		}

		event.context.checkSenderAdmin();

		Macro macro = event.context.gc.getMacro(name);

		if (macro == null) {
			throw new GnomeException("Macro not found!");
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
				event.edit().respond(macro.createMessage(owner, false).ephemeral(true));
			}
		} else {
			macro.addUse();
			event.respond(macro.createMessage(event.context.sender.getId(), false).ephemeral(true));
		}
	}
}