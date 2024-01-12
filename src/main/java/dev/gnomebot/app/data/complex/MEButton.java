package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SimpleStringReader;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;

import java.util.List;
import java.util.UUID;

public class MEButton extends MEButtonBase {
	public static Button.Style readStyle(String s) {
		return switch (s) {
			case "blurple" -> Button.Style.PRIMARY;
			case "green" -> Button.Style.SUCCESS;
			case "red" -> Button.Style.DANGER;
			default -> Button.Style.SECONDARY;
		};
	}

	public Button.Style style = Button.Style.SECONDARY;
	public int type = 0;

	@Override
	public void getLines(List<String> lines) {
		var sb = new StringBuilder();

		if (type == 1) {
			sb.append("macro ");
		} else if (type == 2) {
			sb.append("macro-edit ");
		} else {
			sb.append("button ");
		}

		sb.append(SimpleStringReader.escape(target));

		if (!label.isEmpty() || emoji != null || style != Button.Style.SECONDARY) {
			sb.append(' ');
			sb.append(SimpleStringReader.escape(label));
		}

		if (emoji != null || style != Button.Style.SECONDARY) {
			sb.append(' ');
			sb.append(emoji == null ? "-" : SimpleStringReader.escape(Utils.reactionToString(emoji)));
		}

		if (style != Button.Style.SECONDARY) {
			sb.append(' ');
			sb.append(switch (style) {
				case PRIMARY -> "blurple";
				case SUCCESS -> "green";
				case DANGER -> "red";
				default -> "gray";
			});
		}

		lines.add(sb.toString());
	}

	@Override
	public void acceptOption(ComplexMessageContext ctx, String name, SimpleStringReader reader) {
		if (name.equals("color")) {
			style = readStyle(reader.readString().orElse(""));
		} else {
			super.acceptOption(ctx, name, reader);
		}
	}

	@Override
	public ActionComponent toActionComponent(GuildCollections sourceGuild, GuildCollections targetGuild, long sender) {
		var l = label.isEmpty() ? null : label;
		var id = target;

		if (type == 1 || type == 2) {
			var macro = sourceGuild.getMacro(target);

			if (macro == null) {
				id = "none/" + UUID.randomUUID();
			} else {
				id = (type == 2 ? "edit-macro/" : "macro/") + macro.guild.guildId + "/" + target + "/" + sender;
			}
		}

		return switch (style) {
			case PRIMARY -> Button.primary(id, emoji, l);
			case SUCCESS -> Button.success(id, emoji, l);
			case DANGER -> Button.danger(id, emoji, l);
			default -> Button.secondary(id, emoji, l);
		};
	}
}
