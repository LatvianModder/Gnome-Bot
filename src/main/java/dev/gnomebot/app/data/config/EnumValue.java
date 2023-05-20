package dev.gnomebot.app.data.config;

import org.jetbrains.annotations.NotNull;

public record EnumValue(String value, String name) implements Comparable<EnumValue> {
	@Override
	public int compareTo(@NotNull EnumValue o) {
		return name.compareToIgnoreCase(o.name);
	}
}
