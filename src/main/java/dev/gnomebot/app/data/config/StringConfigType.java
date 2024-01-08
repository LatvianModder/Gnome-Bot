package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import dev.latvian.apps.webutils.FormattingUtils;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public record StringConfigType(@Nullable Pattern pattern) implements BasicConfigType<String> {
	public static final StringConfigType DEFAULT = new StringConfigType(null);
	public static final StringConfigType NON_EMPTY = new StringConfigType(Pattern.compile(".+"));

	@Override
	public String getTypeName() {
		return "string";
	}

	@Override
	public String defaultKeyValue() {
		return "";
	}

	@Override
	public String read(Object value) {
		return String.valueOf(value);
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		return pattern == null || pattern.matcher(value).find() ? "" : "Doesn't match pattern " + FormattingUtils.toRegexString(pattern) + "!";
	}

	@Override
	public String serialize(GuildCollections guild, int type, String value) {
		return value;
	}

	@Override
	public String deserialize(GuildCollections guild, int type, String value) {
		return value;
	}
}
