package dev.gnomebot.app.script.event;

import dev.latvian.mods.rhino.util.HideFromJS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class EventHandler<T extends EventJS> {
	private List<Consumer<T>> consumers;

	public void listen(Consumer<T> consumer) {
		if (consumers == null) {
			consumers = new ArrayList<>();
		}

		consumers.add(consumer);
	}

	public boolean hasListeners() {
		return consumers != null;
	}

	@HideFromJS
	public boolean post(T event, boolean canCancel) {
		if (consumers == null) {
			return false;
		}

		event.cancelled = false;

		for (Consumer<T> consumer : consumers) {
			try {
				consumer.accept(event);

				if (canCancel && event.cancelled) {
					return true;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return false;
	}
}
