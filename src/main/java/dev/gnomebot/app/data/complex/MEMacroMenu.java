package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.data.MacroBundle;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;

import java.util.ArrayList;
import java.util.List;

public class MEMacroMenu extends MEComponent {
	public String macroId = "";

	@Override
	public void getLines(List<String> lines) {
		lines.add("macro-menu " + macroId);
	}

	@Override
	public ActionComponent toActionComponent(ComplexMessageRenderContext ctx) {
		var macro = ctx.sourceGuild.getMacro(macroId);

		if (macro == null) {
			return Button.danger("none", "Macro '" + macroId + "' not found");
		} else if (macro.getCachedContent().b() instanceof MacroBundle bundle) {
			var options = new ArrayList<SelectMenu.Option>();

			for (var item : bundle.macros.values()) {
				var option = SelectMenu.Option.of(item.macro().getDisplayName(), item.macro().name);

				if (item.macro() == ctx.macro) {
					option = option.withDefault(true);
				}

				if (item.macro().emoji != null) {
					option = option.withEmoji(item.macro().emoji);
				}

				option = option.withDescription(item.macro().getDescription());
				options.add(option);
			}

			return SelectMenu.of("macro-menu/" + ctx.sourceGuild.guildId + "/" + macro.id + "/" + ctx.sender, options);
		} else {
			return Button.danger("none", "Macro '" + macroId + "' is '" + macro.getCachedContent().a().name + "' type");
		}
	}
}
