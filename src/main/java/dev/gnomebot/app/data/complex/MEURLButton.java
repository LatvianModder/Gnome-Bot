package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SimpleStringReader;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;

import java.util.List;

public class MEURLButton extends MEButtonBase {
	@Override
	public void getLines(List<String> lines) {
		var sb = new StringBuilder();
		sb.append("url ");
		sb.append(SimpleStringReader.escape(target));

		if (!label.isEmpty() || emoji != null) {
			sb.append(' ');
			sb.append(SimpleStringReader.escape(label));
		}

		if (emoji != null) {
			sb.append(' ');
			sb.append(Utils.reactionToString(emoji));
		}

		lines.add(sb.toString());
	}

	@Override
	public ActionComponent toActionComponent(GuildCollections sourceGuild, GuildCollections targetGuild, long sender) {
		return Button.link(target, emoji, label.isEmpty() ? null : label);
	}
}
