package dev.gnomebot.app.script.event;

public class EventJS {
	public transient boolean cancelled;

	public void cancel() {
		cancelled = true;
	}
}
