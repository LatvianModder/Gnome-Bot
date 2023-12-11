package dev.gnomebot.app.util;

public class BlockingTask {
	public final BlockingTaskCallback callback;
	public boolean cancelled;

	public BlockingTask(BlockingTaskCallback c) {
		callback = c;
		cancelled = false;
	}
}
