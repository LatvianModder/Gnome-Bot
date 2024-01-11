package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SelectMenuType;
import dev.gnomebot.app.util.SimpleStringReader;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.channel.Channel;

import java.util.ArrayList;
import java.util.List;

public class MESelectMenu extends MEComponent implements ComplexMessageContext.OptionHolder {
	public String id = "";
	public SelectMenuType type = SelectMenuType.OPTIONS;
	public int minValues = -1;
	public int maxValues = -1;
	public String placeholder = "";
	public boolean disabled = false;
	public List<SelectMenu.Option> options = new ArrayList<>();
	public List<Channel.Type> channelTypes = null;

	@Override
	public void getLines(List<String> lines) {
		lines.add("select-menu " + SimpleStringReader.escape(id) + (placeholder.isEmpty() ? "" : (" " + SimpleStringReader.escape(placeholder))));

		if (type == SelectMenuType.USER) {
			lines.add("+ user");
		} else if (type == SelectMenuType.ROLE) {
			lines.add("+ role");
		} else if (type == SelectMenuType.MENTIONABLE) {
			lines.add("+ mentionable");
		} else if (type == SelectMenuType.CHANNEL) {
			for (var c : channelTypes) {
				lines.add("+ channel " + c.name().toLowerCase());
			}
		}

		if (minValues != -1) {
			lines.add("+ min-values " + minValues);
		}

		if (maxValues != -1) {
			lines.add("+ max-values " + maxValues);
		}

		if (disabled) {
			lines.add("+ disabled");
		}

		for (var option : options) {
			var sb = new StringBuilder();
			sb.append("+ ");
			sb.append(SimpleStringReader.escape(option.getValue()));
			sb.append(' ');
			sb.append(SimpleStringReader.escape(option.getLabel()));

			if (option.getDescription().isPresent() || option.getEmoji().isPresent()) {
				sb.append(' ');
				sb.append(SimpleStringReader.escape(option.getDescription().orElse("")));
			}

			if (option.getEmoji().isPresent()) {
				sb.append(' ');
				sb.append(SimpleStringReader.escape(Utils.reactionToString(option.getEmoji().get())));
			}

			lines.add(sb.toString());
		}
	}

	@Override
	public ActionComponent toActionComponent(GuildCollections sourceGuild, GuildCollections targetGuild, Snowflake sender) {
		var menu = switch (type) {
			case USER -> SelectMenu.ofUser(id);
			case ROLE -> SelectMenu.ofRole(id);
			case MENTIONABLE -> SelectMenu.ofMentionable(id);
			case CHANNEL -> SelectMenu.ofChannel(id, channelTypes);
			default -> SelectMenu.of(id, options);
		};

		if (minValues != -1) {
			menu = menu.withMinValues(minValues);
		}

		if (maxValues != -1) {
			menu = menu.withMaxValues(maxValues);
		}

		if (!placeholder.isEmpty()) {
			menu = menu.withPlaceholder(placeholder);
		}

		if (disabled) {
			menu = menu.disabled();
		}

		return menu;
	}

	@Override
	public void acceptOption(ComplexMessageContext ctx, SimpleStringReader reader) {
		var first = reader.readString().orElse("");

		switch (first) {
			case "user" -> {
				type = SelectMenuType.USER;
				return;
			}
			case "role" -> {
				type = SelectMenuType.ROLE;
				return;
			}
			case "mentionable" -> {
				type = SelectMenuType.MENTIONABLE;
				return;
			}
			case "channel" -> {
				type = SelectMenuType.CHANNEL;

				if (channelTypes == null) {
					channelTypes = new ArrayList<>();
				}

				var ctype = reader.readString().orElse("");

				if (!ctype.isEmpty()) {
					channelTypes.add(Channel.Type.valueOf(ctype.toUpperCase()));
				}

				return;
			}
			case "min-values" -> {
				minValues = reader.readLong().orElse(-1L).intValue();
				return;
			}
			case "max-values" -> {
				maxValues = reader.readLong().orElse(-1L).intValue();
				return;
			}
			case "disabled" -> {
				disabled = true;
				return;
			}
		}

		var isDefault = first.equals("#default");
		var value = isDefault ? reader.readString().orElse("") : first;
		var label = reader.readString().orElse("Option " + (options.size() + 1));

		var option = isDefault ? SelectMenu.Option.ofDefault(label, value) : SelectMenu.Option.of(label, value);

		var desc = reader.readString().orElse("");

		if (!desc.isEmpty()) {
			option = option.withDescription(desc);
		}

		var emoji = reader.readEmoji().orElse(null);

		if (emoji != null) {
			option = option.withEmoji(emoji);
		}

		options.add(option);
	}
}
