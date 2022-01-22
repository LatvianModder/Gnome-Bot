package dev.gnomebot.app.script;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Deletable {
	void delete(@Nullable String reason);

	default void delete() {
		delete(null);
	}
}
