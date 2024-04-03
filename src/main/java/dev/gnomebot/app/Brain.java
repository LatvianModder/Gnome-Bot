package dev.gnomebot.app;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Brain {
	public static final Object LOCK = new Object();
	public static final Brain ALL = new Brain(0L);
	public static final Map<Long, Brain> GUILD = new HashMap<>();

	public final long id;
	public final LinkedList<BrainEvent> events;

	public Brain(long id) {
		this.id = id;
		this.events = new LinkedList<>();
	}

	public void event(BrainEvent event) {
		events.addFirst(event);

		while (events.size() > 40 * 40) {
			events.removeLast();
		}
	}
}
