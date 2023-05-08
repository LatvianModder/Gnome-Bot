package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;
import discord4j.common.util.Snowflake;

public class ThreadLocation extends WrappedDocument<ThreadLocation> {
	public final Snowflake channel;
	public final Snowflake thread;

	public ThreadLocation(WrappedCollection<ThreadLocation> c, MapWrapper d) {
		super(c, d);
		channel = Snowflake.of(d.getLong("channel"));
		thread = Snowflake.of(d.getLong("thread"));
	}
}