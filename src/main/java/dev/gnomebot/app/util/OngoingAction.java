package dev.gnomebot.app.util;

import java.util.HashMap;
import java.util.Map;

public class OngoingAction {
	private static final Runnable NOTHING = () -> {
	};

	private static final Map<String, OngoingAction> MAP = new HashMap<>();

	public static OngoingAction start(String id, Runnable onStopped) {
		stop(id);
		var a = new OngoingAction(onStopped);
		MAP.put(id, a);
		return a;
	}

	public static OngoingAction start(String id) {
		return start(id, NOTHING);
	}

	public static void stop(String id) {
		var action = MAP.remove(id);

		if (action != null && action.running) {
			action.running = false;
			action.onStopped.run();
		}
	}

	private final Runnable onStopped;
	private boolean running = true;

	private OngoingAction(Runnable r) {
		onStopped = r;
	}

	public boolean isRunning() {
		return running;
	}
}
