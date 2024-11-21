package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

public class JoinLogEntry extends WrappedDocument<JoinLogEntry> {
	public JoinLogEntry(WrappedCollection<JoinLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	public long getUser() {
		return document.getLong("user");
	}

	public long getMemberFor() {
		return document.getLong("member_for");
	}
}