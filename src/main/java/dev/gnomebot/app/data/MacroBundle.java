package dev.gnomebot.app.data;

import dev.gnomebot.app.data.complex.ComplexMessageRenderContext;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.ansi.log.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class MacroBundle {
	public record Item(Macro macro, String id, String description) {
	}

	public static Object parse(GuildCollections gc, String content) {
		var bundle = new MacroBundle();

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

	public final Map<String, Item> macros = new LinkedHashMap<>();

	public MessageBuilder render(ComplexMessageRenderContext ctx) {
		if (ctx.reader == null) {
			return MessageBuilder.create(String.join(" • ", macros.keySet())).noComponents().noEmbeds();
		}

		Log.info(ctx.reader.toString());

		var item = macros.get(ctx.reader.readString().orElse(""));

		if (item == null) {
			return MessageBuilder.create(String.join(" • ", macros.keySet())).noComponents().noEmbeds();
		}

		return item.macro.createMessage(ctx);
	}
}
