package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import org.bson.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StringListConfigKey extends ConfigKey<List<String>> {
	public StringListConfigKey(GuildCollections g, String n, List<String> def) {
		super(g, n, def);
	}

	@Override
	public String getType() {
		return "string";
	}

	@Override
	public String serialize() {
		return get().isEmpty() ? "" : String.join("\n", get());
	}

	@Override
	public void deserialize(String value) {
		set(value.isEmpty() ? List.of() : Arrays.asList(value.split("\n")));
	}

	@Override
	public void read(Document document) {
		set(document.getList(id, String.class, Collections.emptyList()));
	}

	public boolean isEmpty() {
		return get().isEmpty();
	}
}
