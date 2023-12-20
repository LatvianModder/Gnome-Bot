package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SimpleStringReader;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComplexMessage implements ComplexMessageContext.TextHolder {
	public static boolean has(Message message) {
		return !message.getData().embeds().isEmpty() || !message.getData().components().isAbsent();
	}

	public static ComplexMessage parse(String content) {
		if (content.isEmpty()) {
			return new ComplexMessage();
		}

		return parse(Arrays.asList(content.split("\n")));
	}

	public static ComplexMessage parse(List<String> lines) {
		var complex = new ComplexMessage();
		var ctx = new ComplexMessageContext();
		ctx.textHolder = complex;

		for (var line : lines) {
			if (line.isEmpty()) {
				continue;
			}

			var reader = new SimpleStringReader(line);
			var start = reader.readString().orElse("");

			switch (start) {
				case "//" -> {
					// noop
				}
				case "embed" -> {
					var c = complex.newEmbed();
					ctx.propertyHolder = c;
					ctx.textHolder = c;
				}
				case "---", "***" -> ctx.propertyHolder = complex.newLayoutComponent(start.equals("***") ? 1 : 0);
				case ">" -> ctx.acceptText(reader.readRemainingString().orElse("").trim());
				case "-" -> ctx.acceptProperty(reader.readString().orElse(""), reader);
				case "+" -> ctx.acceptOption(reader);
				default -> ctx.acceptProperty(start, reader);
			}
		}

		return complex;
	}

	public static ComplexMessage of(Message message) {
		var complex = new ComplexMessage();

		if (!message.getContent().trim().isEmpty()) {
			complex.content.addAll(Arrays.asList(message.getContent().split("\n")));
		}

		for (var embed : message.getEmbeds()) {
			var e = complex.newEmbed();

			if (embed.getTitle().isPresent()) {
				e.title = embed.getTitle().get();
			}

			if (embed.getDescription().isPresent()) {
				e.description.addAll(Arrays.asList(embed.getDescription().get().split("\n")));
			}

			if (embed.getColor().isPresent()) {
				e.color = embed.getColor().get();
			}

			if (embed.getUrl().isPresent()) {
				e.url = embed.getUrl().get();
			}

			for (var field : embed.getFields()) {
				var f = new MEEmbed.Field();
				e.fields.add(f);
				f.name = field.getName();
				f.inline = field.isInline();

				if (!field.getValue().isEmpty()) {
					f.value.addAll(Arrays.asList(field.getValue().split("\n")));
				}
			}

			if (embed.getImage().isPresent()) {
				e.image = embed.getImage().get().getUrl();
			}

			if (embed.getThumbnail().isPresent()) {
				e.thumbnail = embed.getThumbnail().get().getUrl();
			}
		}

		for (var layoutComponent : message.getComponents()) {
			var lc = new MELayoutComponent();

			if (!(layoutComponent instanceof ActionRow)) {
				lc.type = 1;
			}

			for (var component : layoutComponent.getChildren()) {
				if (component instanceof Button b) {
					if (b.getStyle() == Button.Style.LINK) {
						var button = new MEURLButton();
						lc.components.add(button);
						button.target = b.getUrl().orElse("");
						button.label = b.getLabel().orElse("");
						button.emoji = b.getEmoji().orElse(null);
					} else {
						var button = new MEButton();
						lc.components.add(button);
						button.target = b.getCustomId().orElse("");
						button.label = b.getLabel().orElse("");
						button.emoji = b.getEmoji().orElse(null);
						button.style = b.getStyle();

						if (button.target.startsWith("macro/")) {
							button.type = 1;
							button.target = button.target.substring(6, button.target.lastIndexOf('/'));
						} else if (button.target.startsWith("edit-macro/") || button.target.startsWith("edit_macro/")) {
							button.type = 2;
							button.target = button.target.substring(11, button.target.lastIndexOf('/'));
						}
					}
				}
			}

			if (!lc.components.isEmpty()) {
				complex.components.add(lc);
			}
		}

		return complex;
	}

	public List<String> content = new ArrayList<>();
	public List<MEEmbed> embeds = new ArrayList<>();
	public List<MELayoutComponent> components = new ArrayList<>();

	@Override
	public void acceptText(ComplexMessageContext ctx, String s) {
		content.add(s);
	}

	public MEEmbed newEmbed() {
		var embed = new MEEmbed();
		embeds.add(embed);
		return embed;
	}

	public MELayoutComponent newLayoutComponent(int type) {
		var lc = new MELayoutComponent();
		lc.type = type;
		components.add(lc);
		return lc;
	}

	public List<String> getLines() {
		var lines = new ArrayList<String>();
		lines.add("// complex");

		for (var line : content) {
			lines.add(("> " + line).trim());
		}

		for (var embed : embeds) {
			embed.getLines(lines);
		}

		for (var lc : components) {
			lines.add(lc.type == 1 ? "***" : "---");

			for (var c : lc.components) {
				c.getLines(lines);
			}
		}

		return lines;
	}

	public void apply(MessageBuilder builder, Snowflake sender) {
		builder.content(content);

		if (embeds.isEmpty()) {
			builder.noEmbeds();
		} else {
			for (var embed : embeds) {
				builder.addEmbed(embed.toEmbedBuilder());
			}
		}

		if (components.isEmpty()) {
			builder.noComponents();
		} else {
			for (var lc : components) {
				builder.addComponent(lc.toLayoutComponent(sender));
			}
		}
	}
}
