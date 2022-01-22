package dev.gnomebot.app.util;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface BlockingTaskCallback {
	void run(BlockingTask task) throws Exception;

	default void afterAddedBlocking() {
	}
}
