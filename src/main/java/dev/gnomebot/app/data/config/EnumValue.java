package dev.gnomebot.app.data.config;

import org.jetbrains.annotations.NotNull;

public record EnumValue(String value, String name) implements Comparable<EnumValue> {
	public EnumValue(String value) {
		this(value, value);
	}

	@Override
	public int compareTo(@NotNull EnumValue o) {
		return name.compareToIgnoreCase(o.name);
	}
}
