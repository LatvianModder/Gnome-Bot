package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StringListConfig extends BaseConfig<List<String>> {
	public StringListConfig(GuildCollections g, String n, List<String> def) {
		super(g, n, def);
	}

	@Override
	public String getType() {
		return "string";
	}

	@Override
	public void fromDB(Object o) {
		set(new ArrayList<>((Collection<String>) o));
	}

	@Override
	public String toJson() {
		return String.join(" | ", get());
	}

	@Override
	public void fromJson(Object json) {
		String s = String.valueOf(json).trim();
		set(s.isEmpty() ? Collections.emptyList() : Arrays.asList(s.split(" \\| ")));
	}

	public boolean isEmpty() {
		return get().isEmpty();
	}
}
