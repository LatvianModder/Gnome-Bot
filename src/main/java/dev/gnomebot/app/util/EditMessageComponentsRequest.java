package dev.gnomebot.app.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.possible.Possible;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@JsonSerialize(as = EditMessageComponentsRequest.class)
public class EditMessageComponentsRequest {
	private final List<ComponentData> components;

	public EditMessageComponentsRequest(@Nullable List<ComponentData> c) {
		components = c;
	}

	@JsonProperty("components")
	public Possible<Optional<List<ComponentData>>> components() {
		return Possible.of(Optional.ofNullable(components));
	}
}
