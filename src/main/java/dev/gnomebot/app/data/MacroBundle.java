package dev.gnomebot.app.data;

import dev.gnomebot.app.data.complex.ComplexMessageRenderContext;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.object.component.SelectMenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MacroBundle {
	public record Item(Macro macro, String id, String description) {
	}

	public static Object parse(GuildCollections gc, String content) {
		var bundle = new MacroBundle(gc);

		for (var line : content.split("\n")) {
			if (line.isBlank() || line.startsWith("//")) {
				continue;
			}

			var reader = new CommandReader(gc, line);

			var macro = gc.getMacro(reader.readString().orElse(""));

			if (macro != null) {
				var id = reader.readString().orElse(macro.stringId);
				bundle.macros.put(id, new Item(macro, id, reader.readString().orElse(macro.getDescription())));

				if (bundle.macros.size() >= 25) {
					break;
				}
			}
		}

		return bundle;
	}

	public final GuildCollections gc;
	public final Map<String, Item> macros;

	public MacroBundle(GuildCollections gc) {
		this.gc = gc;
		this.macros = new LinkedHashMap<>();
	}

	private MessageBuilder renderDefault(ComplexMessageRenderContext ctx) {
		var ctx2 = ctx.copy();
		ctx2.sourceGuild = gc;

		return MessageBuilder.create(String.join(" • ", macros.keySet())).addComponentRow(createSelectMenu(ctx2)).noEmbeds();
	}

	public MessageBuilder render(ComplexMessageRenderContext ctx) {
		if (ctx.reader == null) {
			return renderDefault(ctx);
		}

		var item = macros.get(ctx.reader.readString().orElse(""));

		if (item == null) {
			return renderDefault(ctx);
		}

		return item.macro.createMessage(ctx);
	}

	public SelectMenu createSelectMenu(ComplexMessageRenderContext ctx) {
		var options = new ArrayList<SelectMenu.Option>();

		for (var item : macros.values()) {
			var m = item.macro();

			var option = SelectMenu.Option.of(m.displayName.isEmpty() ? item.id : m.displayName, m.id.toString());

			if (m == ctx.macro) {
				option = option.withDefault(true);
			}

			if (m.emoji != null) {
				option = option.withEmoji(m.emoji);
			}

			option = option.withDescription(m.getDescription());
			options.add(option);
		}

		return SelectMenu.of("macro-menu/" + ctx.sourceGuild.guildId + "/" + ctx.sender, options);
	}
}
