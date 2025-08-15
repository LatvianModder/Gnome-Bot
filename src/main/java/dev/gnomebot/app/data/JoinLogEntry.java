package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

public class JoinLogEntry extends WrappedDocument<JoinLogEntry> {
	public JoinLogEntry(WrappedCollection<JoinLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	public long user() {
		return document.getLong("user");
	}

	public long memberFor() {
		return document.getLong("member_for");
	}
}