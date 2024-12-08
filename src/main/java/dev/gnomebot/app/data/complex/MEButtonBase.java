package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.util.SimpleStringReader;
import discord4j.core.object.reaction.ReactionEmoji;

public abstract class MEButtonBase extends MEComponent implements ComplexMessageParseContext.OptionHolder {
	public String target = "";
	public String label = "";
	public ReactionEmoji emoji = null;

	@Override
	public void acceptOption(ComplexMessageParseContext ctx, SimpleStringReader reader) {
		acceptOption(ctx, reader.readString().orElse(""), reader);
	}

	public void acceptOption(ComplexMessageParseContext ctx, String name, SimpleStringReader reader) {
		switch (name) {
			case "label" -> label = reader.readString().orElse("");
			case "emoji" -> emoji = reader.readEmoji().orElse(null);
		}
	}
}
