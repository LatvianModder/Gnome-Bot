package dev.gnomebot.app.util;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.EmbedColor;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmbedAuthorData;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFooterData;
import discord4j.discordjson.json.EmbedImageData;
import discord4j.discordjson.json.EmbedThumbnailData;
import discord4j.discordjson.json.ImmutableEmbedAuthorData;
import discord4j.discordjson.json.ImmutableEmbedFooterData;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmbedBuilder {
	public static EmbedBuilder create() {
		return new EmbedBuilder();
	}

	public static EmbedBuilder create(String description) {
		return create().description(description);
	}

	public static EmbedBuilder create(String title, String description) {
		return create().title(title).description(description);
	}

	public String title;
	public String description;
	public String url;
	public Instant timestamp;
	public Color defaultColor;
	public Color color;
	public String footerText;
	public String footerIconUrl;
	public String image;
	public String thumbnail;
	public String authorName;
	public String authorUrl;
	public String authorIconUrl;
	public List<EmbedCreateFields.Field> fields;

	private EmbedBuilder() {
		title = null;
		description = null;
		url = null;
		timestamp = null;
		defaultColor = EmbedColor.GRAY;
		color = null;
		footerText = null;
		footerIconUrl = null;
		image = null;
		thumbnail = null;
		authorName = null;
		authorUrl = null;
		authorIconUrl = null;
		fields = null;
	}

	public static EmbedBuilder of(Object object) {
		if (object == null) {
			return null;
		} else if (object instanceof CharSequence) {
			return create(object.toString());
		} else if (object instanceof Map map) {
			EmbedBuilder builder = EmbedBuilder.create();

			if (map.get("title") instanceof CharSequence s) {
				builder.title(s.toString());
			}

			if (map.get("description") instanceof CharSequence s) {
				builder.description(s.toString());
			}

			if (map.get("url") instanceof CharSequence s) {
				builder.url(s.toString());
			}

			if (map.get("image") instanceof CharSequence s) {
				builder.image(s.toString());
			}

			if (map.get("thumbnail") instanceof CharSequence s) {
				builder.thumbnail(s.toString());
			}

			if (map.containsKey("color")) {
				builder.color(EmbedColor.of(map.get("color")));
			}

			return builder;
		}

		App.error("Invalid script embbed: " + object);
		return EmbedBuilder.create("Invalid script embed!");
	}

	public EmbedBuilder title(String title) {
		this.title = title;
		return this;
	}

	public EmbedBuilder description(String description) {
		this.description = description;
		return this;
	}

	public EmbedBuilder description(List<String> description) {
		return description(String.join("\n", description));
	}

	public EmbedBuilder url(String url) {
		this.url = url;
		return this;
	}

	public EmbedBuilder timestamp(Instant timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public EmbedBuilder timestamp() {
		return timestamp(Instant.now());
	}

	public EmbedBuilder defaultColor(Color defaultColor) {
		this.defaultColor = defaultColor;
		return this;
	}

	public EmbedBuilder color(Color color) {
		this.color = color;
		return this;
	}

	public EmbedBuilder redColor() {
		return color(EmbedColor.RED);
	}

	public EmbedBuilder greenColor() {
		return color(EmbedColor.GREEN);
	}

	public EmbedBuilder tealColor() {
		return color(EmbedColor.TEAL);
	}

	public EmbedBuilder blueColor() {
		return color(EmbedColor.BLUE);
	}

	public EmbedBuilder footer(String text, @Nullable String iconUrl) {
		this.footerText = text;
		this.footerIconUrl = iconUrl;
		return this;
	}

	public EmbedBuilder footer(String text) {
		return footer(text, null);
	}

	public EmbedBuilder image(String image) {
		this.image = image;
		return this;
	}

	public EmbedBuilder thumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
		return this;
	}

	public EmbedBuilder author(String name, @Nullable String iconUrl, @Nullable String url) {
		this.authorName = name;
		this.authorIconUrl = iconUrl;
		this.authorUrl = url;
		return this;
	}

	public EmbedBuilder author(String text, @Nullable String iconUrl) {
		return author(text, iconUrl, null);
	}

	public EmbedBuilder fields(List<EmbedCreateFields.Field> fields) {
		this.fields = fields;
		return this;
	}

	public EmbedBuilder field(EmbedCreateFields.Field field) {
		if (this.fields == null) {
			this.fields = new ArrayList<>();
		}

		this.fields.add(field);
		return this;
	}

	public EmbedBuilder field(String name, String value, boolean inline) {
		return field(EmbedCreateFields.Field.of(name, value, inline));
	}

	public EmbedBuilder field(String name, String value) {
		return field(name, value, false);
	}

	public EmbedBuilder inlineField(String name, String value) {
		return field(name, value, true);
	}

	// Specs //

	public EmbedCreateSpec toEmbedCreateSpec() {
		EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

		if (this.title != null && !this.title.isEmpty()) {
			builder.title(FormattingUtils.trim(this.title, 256));
		}

		if (this.description != null && !this.description.isEmpty()) {
			builder.description(FormattingUtils.trim(this.description, 4096));
		}

		if (this.url != null && !this.url.isEmpty()) {
			builder.url(this.url);
		}

		if (this.timestamp != null) {
			builder.timestamp(this.timestamp);
		}

		builder.color(this.color == null ? this.defaultColor : this.color);

		if (this.footerText != null) {
			builder.footer(FormattingUtils.trim(this.footerText, 2048), this.footerIconUrl);
		}

		if (this.image != null) {
			builder.image(this.image);
		}

		if (this.thumbnail != null) {
			builder.thumbnail(this.thumbnail);
		}

		if (this.authorName != null) {
			builder.author(FormattingUtils.trim(this.authorName, 256), this.authorUrl, this.authorIconUrl);
		}

		if (this.fields != null) {
			builder.fields(this.fields);
		}

		return builder.build();
	}

	public EmbedData toEmbedData() {
		var builder = EmbedData.builder();

		if (this.title != null && !this.title.isEmpty()) {
			builder.title(FormattingUtils.trim(this.title, 256));
		}

		if (this.description != null && !this.description.isEmpty()) {
			builder.description(FormattingUtils.trim(this.description, 4096));
		}

		if (this.url != null && !this.url.isEmpty()) {
			builder.url(this.url);
		}

		if (this.timestamp != null) {
			builder.timestamp(this.timestamp.toString());
		}

		builder.color(this.color == null ? this.defaultColor.getRGB() : this.color.getRGB());

		if (this.footerText != null) {
			ImmutableEmbedFooterData.Builder footer = EmbedFooterData.builder();
			footer.text(FormattingUtils.trim(this.footerText, 2048));

			if (this.footerIconUrl != null) {
				footer.iconUrl(this.footerIconUrl);
			}

			builder.footer(footer.build());
		}

		if (this.image != null) {
			builder.image(EmbedImageData.builder().url(this.image).build());
		}

		if (this.thumbnail != null) {
			builder.thumbnail(EmbedThumbnailData.builder().url(this.thumbnail).build());
		}

		if (this.authorName != null) {
			ImmutableEmbedAuthorData.Builder author = EmbedAuthorData.builder();
			author.name(FormattingUtils.trim(this.authorName, 256));

			if (this.authorUrl != null) {
				author.url(this.authorUrl);
			}

			if (this.authorIconUrl != null) {
				author.iconUrl(this.authorIconUrl);
			}

			builder.author(author.build());
		}

		if (this.fields != null) {
			builder.fields(this.fields.stream().map(EmbedCreateFields.Field::asRequest).toList());
		}

		return builder.build();
	}

	@Override
	public String toString() {
		return "EmbedBuilder{" +
				"title='" + title + '\'' +
				", description='" + description + '\'' +
				", url='" + url + '\'' +
				", timestamp=" + timestamp +
				", defaultColor=" + defaultColor +
				", color=" + color +
				", footerText='" + footerText + '\'' +
				", footerIconUrl='" + footerIconUrl + '\'' +
				", image='" + image + '\'' +
				", thumbnail='" + thumbnail + '\'' +
				", authorName='" + authorName + '\'' +
				", authorUrl='" + authorUrl + '\'' +
				", authorIconUrl='" + authorIconUrl + '\'' +
				", fields=" + fields.stream().map(EmbedCreateFields.Field::asRequest).toList() +
				'}';
	}
}
