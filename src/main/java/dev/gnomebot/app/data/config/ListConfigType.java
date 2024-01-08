package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record ListConfigType<T>(ConfigType<T, ?> parent) implements BasicConfigType<List<T>> {
	@Override
	public String getTypeName() {
		return parent.getTypeName() + "[]";
	}

	@Override
	public List<T> defaultKeyValue() {
		return List.of();
	}

	@Override
	public Object write(List<T> value) {
		return value.stream().map(parent::write).toList();
	}

	@Override
	public List<T> read(Object value) {
		return value instanceof List<?> list ? list.stream().map(parent::read).toList() : List.of();
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		if (value.isEmpty()) {
			return "";
		}

		for (var s : value.split(" ; ")) {
			var error = parent.validate(guild, type, s);

			if (!error.isEmpty()) {
				return error;
			}
		}

		return "";
	}

	@Override
	public String serialize(GuildCollections guild, int type, List<T> value) {
		return value.isEmpty() ? "" : value.stream().map(t -> parent.serialize(guild, type, t)).collect(Collectors.joining(" ; "));
	}

	@Override
	public List<T> deserialize(GuildCollections guild, int type, String value) {
		if (value.isEmpty()) {
			return List.of();
		}

		return Arrays.stream(value.split(" ; ")).map(s -> parent.deserialize(guild, type, s)).toList();
	}
}
