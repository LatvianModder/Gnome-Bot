package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.data.MacroBundle;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;

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
			return bundle.createSelectMenu(ctx);
		} else {
			return Button.danger("none", "Macro '" + macroId + "' is '" + macro.getCachedContent().a().name + "' type");
		}
	}
}
