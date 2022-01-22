package dev.gnomebot.app.util;

/**
 * @author LatvianModder
 */
public class ScheduledTask {
	public final ScheduledTaskCallback callback;
	public long end;

	public ScheduledTask(ScheduledTaskCallback c, long e) {
		callback = c;
		end = e;
	}
}
