package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SimpleStringReader;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;

import java.util.ArrayList;
import java.util.List;

public class MELayoutComponent implements ComplexMessageContext.PropertyHolder {
	public List<MEComponent> components = new ArrayList<>();
	public int type = 0; // 0 = row

	public LayoutComponent toLayoutComponent(GuildCollections sourceGuild, GuildCollections targetGuild, long sender) {
		return ActionRow.of(components.stream().map(c -> c.toActionComponent(sourceGuild, targetGuild, sender)).toList());
	}

	@Override
	public void acceptProperty(ComplexMessageContext ctx, String name, SimpleStringReader reader) {
		switch (name) {
			case "url" -> {
				var button = new MEURLButton();
				components.add(button);
				button.target = reader.readString().orElse("");
				button.label = reader.readString().orElse("");
				button.emoji = reader.readEmoji().orElse(null);
				ctx.optionHolder = button;
			}
			case "macro", "macro-edit", "button" -> {
				var button = new MEButton();
				components.add(button);

				if (name.equals("macro")) {
					button.type = 1;
				} else if (name.equals("macro-edit")) {
					button.type = 2;
				}

				button.target = reader.readString().orElse("");
				button.label = reader.readString().orElse(button.target);
				button.emoji = reader.readEmoji().orElse(null);
				button.style = reader.readString().map(MEButton::readStyle).orElse(Button.Style.SECONDARY);
				ctx.optionHolder = button;
			}
			case "select-menu" -> {
				var menu = new MESelectMenu();
				components.add(menu);
				menu.id = reader.readString().orElse("");
				ctx.optionHolder = menu;
			}
		}
	}
}
