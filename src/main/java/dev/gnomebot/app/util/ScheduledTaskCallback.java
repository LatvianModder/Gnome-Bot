package dev.gnomebot.app.util;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface ScheduledTaskCallback {
	void run(ScheduledTask task) throws Exception;

	default void afterAddedScheduled() {
	}
}