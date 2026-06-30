package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

public class EchoLogEntry extends WrappedDocument<EchoLogEntry> {
	public EchoLogEntry(WrappedCollection<EchoLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	public long channel() {
		return document.getLong("channel");
	}

	public long author() {
		return document.getLong("author");
	}

	public String content() {
		return document.getString("content");
	}
}