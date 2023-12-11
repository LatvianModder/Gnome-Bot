package dev.gnomebot.app.util;

@FunctionalInterface
public interface BlockingTaskCallback {
	void run(BlockingTask task) throws Exception;

	default void afterAddedBlocking() {
	}
}
