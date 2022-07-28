package dev.gnomebot.app.script.event;

public class EventJS {
	public transient boolean cancelled = false;

	public void cancel() {
		cancelled = true;
	}
}
