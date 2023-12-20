package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.SimpleStringReader;
import discord4j.rest.util.Color;

import java.util.ArrayList;
import java.util.List;

public class MEEmbed implements ComplexMessageContext.TextHolder, ComplexMessageContext.PropertyHolder {
	public static class Field implements ComplexMessageContext.TextHolder {
		public String name = "";
		public List<String> value = new ArrayList<>();
		public boolean inline = false;

		@Override
		public void acceptText(ComplexMessageContext ctx, String s) {
			value.add(s);
		}
	}

	public String title = "";
	public String url = "";
	public Color color = null;
	public List<String> description = new ArrayList<>();
	public List<Field> fields = new ArrayList<>();
	public String image = "";
	public String thumbnail = "";

	@Override
	public void acceptText(ComplexMessageContext ctx, String s) {
		description.add(s);
	}

	public void getLines(List<String> lines) {
		if (!title.isEmpty()) {
			lines.add("embed " + title);
		} else {
			lines.add("embed");
		}

		for (var desc : description) {
			lines.add(("> " + desc).trim());
		}

		if (color != null) {
			lines.add("color " + EmbedColor.colorName(color.getRGB() & 0xFFFFFF));
		}

		if (!url.isEmpty()) {
			lines.add("url " + url);
		}

		for (var field : fields) {
			lines.add((field.inline ? "inline-field " : "field ") + field.name);

			for (var fl : field.value) {
				lines.add(("> " + fl).trim());
			}
		}

		if (!image.isEmpty()) {
			lines.add("image " + image);
		}

		if (!thumbnail.isEmpty()) {
			lines.add("thumbnail " + thumbnail);
		}
	}

	@Override
	public void acceptProperty(ComplexMessageContext ctx, String name, SimpleStringReader reader) {
		switch (name) {
			case "title" -> title = reader.readRemainingString().orElse("");
			case "url" -> url = reader.readRemainingString().orElse("");
			case "color" -> color = reader.readString().map(EmbedColor::of).orElse(null);
			case "field", "inline-field" -> {
				var field = new Field();
				fields.add(field);
				field.name = reader.readRemainingString().orElse("");
				field.inline = name.equals("inline-field");
				ctx.textHolder = field;
			}
			case "image" -> image = reader.readRemainingString().orElse("");
			case "thumbnail" -> thumbnail = reader.readRemainingString().orElse("");
		}
	}

	public EmbedBuilder toEmbedBuilder() {
		var builder = EmbedBuilder.create();

		if (!title.isEmpty()) {
			builder.title = title;
		}

		if (!description.isEmpty()) {
			builder.description(description);
		}

		if (color != null) {
			builder.color = color;
		}

		if (!url.isEmpty()) {
			builder.url = url;
		}

		for (var field : fields) {
			if (!field.value.isEmpty()) {
				builder.field(field.name, String.join("\n", field.value), field.inline);
			}
		}

		if (!image.isEmpty()) {
			builder.image = image;
		}

		if (!thumbnail.isEmpty()) {
			builder.thumbnail = thumbnail;
		}

		return builder;
	}
}
