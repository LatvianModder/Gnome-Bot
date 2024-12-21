package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ContentType;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.data.complex.ComplexMessageRenderContext;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.component.TextInput;

import java.time.Instant;
import java.util.ArrayList;
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
		for (var macro : event.context.gc.getMacroMap().values()) {
			event.suggestions.add(macro.getChatCommandSuggestion());
		}
	}

	private static void suggestOwnMacro(ChatCommandSuggestionEvent event) {
		var admin = event.context.isAdmin();

		for (var macro : event.context.gc.getMacroMap().values()) {
			if (admin || macro.author == event.context.sender.getId().asLong()) {
				event.suggestions.add(macro.getChatCommandSuggestion());
			}
		}
	}

	private static void add(ChatInputInteractionEventWrapper event) {
		var name = event.get("name").asString();

		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		} else if (name.length() > 50) {
			throw new GnomeException("Macro name too long! Max 50 characters.");
		}

		if (event.context.gc.macroExists(name)) {
			throw new GnomeException("Macro with that name already exists!");
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

		if (event.context.gc.macroExists(name)) {
			throw new GnomeException("Macro with that name already exists!");
		}

		var content = ContentType.decodeMentions(event.get("content").asString());

		if (content.isEmpty()) {
			throw new GnomeException("Can't have empty content!");
		}

		var macro = new Macro(event.context.gc);
		event.context.gc.db.findMacroId(macro);
		macro.stringId = name.toLowerCase();
		macro.name = name;
		macro.author = event.context.sender.getId().asLong();
		macro.created = Instant.now();

		event.context.gc.getMacroMap().put(macro.stringId, macro);
		event.context.gc.getMacroUseMap().remove(macro.id.getAsInt());
		event.context.gc.saveMacroMap();
		event.context.gc.saveMacroUseMap();

		macro.setContent(content);

		event.respond(MessageBuilder.create("Macro '" + name + "' created!").ephemeral(false));
	}

	private static void edit(ChatInputInteractionEventWrapper event) {
		var macro = event.context.gc.getMacroFromCommand(event.get("name").asString());

		event.respondModal("edit-macro/" + macro.stringId, "Editing macro '" + macro.name + "'",
				TextInput.small("rename", "Rename", 1, 50).required(false).prefilled(macro.name),
				TextInput.paragraph("content", "Content").prefilled(ContentType.encodeMentions(macro.getContent()))
		);
	}

	public static void editMacroCallback(ModalEventWrapper event, String name) {
		var macro = event.context.gc.getMacroFromCommand(name);

		if (macro.author != event.context.sender.getId().asLong() && !event.context.isAdmin()) {
			throw new GnomeException("You can only edit your own macros!");
		}

		var rename = event.get("rename").asString(macro.name);

		if (!rename.equals(macro.name)) {
			if (rename.length() > 50) {
				throw new GnomeException("Macro name too long! Max 50 characters.");
			}

			if (event.context.gc.macroExists(rename)) {
				throw new GnomeException("Macro with that name already exists!");
			}

			macro.rename(rename);
		}

		macro.setContent(ContentType.decodeMentions(event.get("content").asString(macro.getContent())));

		event.context.gc.saveMacroMap();
		event.context.channelInfo.createMessage(event.context.sender.getMention() + " updated macro " + macro.chatFormatted(true) + "!").block();

		var ctx = new ComplexMessageRenderContext(event.context.gc, event.context.sender.getId().asLong());

		macro.createMessageOrTimeout(ctx).thenAccept(preview -> {
			preview.content("Preview:\n\n" + preview.getContent());
			preview.ephemeral(true);
			event.respond(preview);
		});
	}

	private static void remove(ChatInputInteractionEventWrapper event) {
		var macro = event.context.gc.getMacroFromCommand(event.get("name").asString());

		if (macro.author != event.context.sender.getId().asLong()) {
			event.context.checkSenderAdmin();
		}

		event.acknowledge();

		var s = "Macro '" + macro.name + "' removed!";

		try {
			macro.setSlashCommand(false);
		} catch (Exception ex) {
			ex.printStackTrace();
			s += "\nWith error: " + ex;
		}

		macro.setContent("");
		event.context.gc.db.allMacros.remove(macro.id.getAsInt());
		event.context.gc.getMacroMap().remove(macro.stringId);
		event.context.gc.getMacroUseMap().remove(macro.id.getAsInt());
		event.context.gc.saveMacroMap();
		event.context.gc.saveMacroUseMap();
		event.respond(s);
	}

	private static void find(ChatInputInteractionEventWrapper event) {
		event.acknowledge();
		var author = event.get("author").asUser().orElse(null);
		var nameStr = event.get("name").asString().toLowerCase();
		var name = nameStr.isEmpty() ? null : Pattern.compile(nameStr, Pattern.CASE_INSENSITIVE);
		var contentStr = event.get("content").asString().toLowerCase();
		var content = contentStr.isEmpty() ? null : Pattern.compile(contentStr, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		var includeHidden = event.get("include-hidden").asBoolean(false);

		var list = new ArrayList<Macro>();

		for (var macro : event.context.gc.getMacroMap().values()) {
			if (author == null || macro.author == author.getId().asLong()) {
				if (name == null || name.matcher(macro.name).find()) {
					if (content == null || content.matcher(macro.getContent()).find()) {
						if (includeHidden || !macro.isHidden()) {
							list.add(macro);
						}
					}

					if (content != null) {
						macro.invalidateCache();
					}
				}
			}
		}

		list.sort(null);

		if (list.isEmpty()) {
			event.respond("No macros found!");
		} else {
			event.respond(list.stream().map(m -> m.name.replaceAll("_", "\\\\_")).collect(Collectors.joining(" • ")));
		}
	}

	private static void info(ChatInputInteractionEventWrapper event) {
		var macro = event.context.gc.getMacroFromCommand(event.get("name").asString());

		event.acknowledge();
		var list = new ArrayList<String>();
		list.add("Author: <@" + SnowFlake.str(macro.author) + ">");
		list.add("Created: " + Utils.formatRelativeDate(macro.created));
		list.add("Uses: " + macro.getUses());
		event.respond(EmbedBuilder.create().url(event.context.gc.db.app.url(macro.url())).title("Macro '" + macro.name + "'").description(list));
	}

	public static void macroButtonCallback(ComponentEventWrapper event, long guildId, String name, long senderId) {
		var macro = (guildId == event.context.gc.guildId ? event.context.gc : event.context.gc.db.guild(guildId)).getMacroFromCommand(name);

		// event.acknowledge();

		if (senderId != 0L && senderId == event.context.sender.getId().asLong()) {
			macro.addUse();
			var ctx = new ComplexMessageRenderContext(event.context.gc, senderId);
			macro.createMessageOrTimeout(ctx).thenAccept(m -> event.edit().respond(m.ephemeral(true)));
		} else {
			macro.addUse();
			var ctx = new ComplexMessageRenderContext(event.context.gc, event.context.sender.getId().asLong());
			macro.createMessageOrTimeout(ctx).thenAccept(m -> event.respond(m.ephemeral(true)));
		}
	}
}
