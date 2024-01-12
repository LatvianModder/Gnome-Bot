package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

public class ThreadLocation extends WrappedDocument<ThreadLocation> {
	public final long channel;
	public final long thread;

	public ThreadLocation(WrappedCollection<ThreadLocation> c, MapWrapper d) {
		super(c, d);
		channel = d.getLong("channel");
		thread = d.getLong("thread");
	}
}