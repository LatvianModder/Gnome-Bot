package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SimpleStringReader;
import dev.latvian.apps.ansi.log.Log;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComplexMessage implements ComplexMessageParseContext.TextHolder {
	public static boolean has(Message message) {
		return !message.getData().embeds().isEmpty() || !message.getData().components().isAbsent();
	}

	public static ComplexMessage parse(GuildCollections gc, String content) {
		if (content.isEmpty()) {
			return new ComplexMessage(gc);
		}

		return parse(gc, Arrays.asList(content.split("\n")));
	}

	public static void appendContent(List<String> lines, List<String> content) {
		if (content.size() >= 3) {
			lines.add(">>>");
			lines.addAll(content);
			lines.add("<<<");
		} else if (!content.isEmpty()) {
			for (var line : content) {
				lines.add("> " + line);
			}
		}
	}

	public static ComplexMessage parse(GuildCollections gc, List<String> lines) {
		var complex = new ComplexMessage(gc);
		var ctx = new ComplexMessageParseContext();
		ctx.textHolder = complex;

		List<String> contentBlockLines = null;

		for (var line : lines) {
			line = line.strip();

			if (App.debug) {
				Log.info("Parsing line " + line);
			}

			if (contentBlockLines != null) {
				if (line.equals("<<<")) {
					contentBlockLines.forEach(ctx::acceptText);
					contentBlockLines = null;
				} else {
					contentBlockLines.add(line);
				}

				continue;
			} else if (line.isEmpty()) {
				continue;
			}

			var reader = new SimpleStringReader(line);
			var start = reader.readString().orElse("");

			switch (start) {
				case "//" -> {
					// noop
				}
				case ">>>" -> contentBlockLines = new ArrayList<>();
				case "embed" -> {
					var c = complex.newEmbed();
					c.title = reader.readRemainingString().orElse("");
					ctx.propertyHolder = c;
					ctx.textHolder = c;
				}
				case "---", "***" -> ctx.propertyHolder = complex.newLayoutComponent(start.equals("***") ? 1 : 0);
				case ">" -> ctx.acceptText(reader.readRemainingString().orElse(""));
				case "-" -> ctx.acceptProperty(reader.readString().orElse(""), reader);
				case "+" -> ctx.acceptOption(reader);
				default -> ctx.acceptProperty(start, reader);
			}
		}

		return complex;
	}

	public static ComplexMessage of(GuildCollections gc, Message message) {
		var complex = new ComplexMessage(gc);

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

			if (layoutComponent instanceof LayoutComponent lc2) {
				for (var component : lc2.getChildren()) {
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
							button.target = b.getCustomId();
							button.label = b.getLabel().orElse("");
							button.emoji = b.getEmoji().orElse(null);
							button.style = b.getStyle();

							if (button.target.startsWith("macro/")) {
								button.type = 1;
								button.target = button.target.split("/")[2];
							} else if (button.target.startsWith("edit-macro/") || button.target.startsWith("edit_macro/")) {
								button.type = 2;
								button.target = button.target.split("/")[2];
							}
						}
					}
				}

				if (!lc.components.isEmpty()) {
					complex.components.add(lc);
				}
			}
		}

		return complex;
	}

	public final GuildCollections sourceGuild;
	public List<String> content = new ArrayList<>();
	public List<MEEmbed> embeds = new ArrayList<>();
	public List<MELayoutComponent> components = new ArrayList<>();

	public ComplexMessage(GuildCollections sourceGuild) {
		this.sourceGuild = sourceGuild;
	}

	@Override
	public void acceptText(ComplexMessageParseContext ctx, String s) {
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
		appendContent(lines, content);

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

	public void apply(MessageBuilder builder, ComplexMessageRenderContext ctx) {
		var ctx2 = ctx.copy();
		ctx2.sourceGuild = sourceGuild;
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
				builder.addComponent(lc.toLayoutComponent(ctx2));
			}
		}
	}
}
