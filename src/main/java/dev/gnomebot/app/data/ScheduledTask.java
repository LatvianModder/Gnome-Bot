package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ScheduledTask extends WrappedDocument<ScheduledTask> {
	public enum Type {
		UNKNOWN("unknown"),
		UNMUTE("unmute"),
		REMIND_ME("remind_me");

		public static final Map<String, Type> MAP = Arrays.stream(values()).collect(Collectors.toMap(k -> k.name, v -> v));

		public final String name;

		Type(String n) {
			name = n;
		}
	}

	public final GuildCollections gc;
	public final Type type;
	public final long time;
	public final long channel;
	public final long user;
	public final String content;

	public ScheduledTask(GuildCollections g, WrappedCollection<ScheduledTask> c, MapWrapper d) {
		super(c, d);
		gc = g;
		type = Type.MAP.getOrDefault(d.getString("type"), Type.UNKNOWN);
		time = d.getLong("time");
		channel = d.getLong("channel");
		user = d.getLong("user");
		content = d.getString("content");
	}
}