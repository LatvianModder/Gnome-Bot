package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

public class Paste extends WrappedDocument<Paste> {
	public Paste(WrappedCollection<Paste> c, MapWrapper d) {
		super(c, d);
	}

	public long getChannelID() {
		return document.getLong("channel");
	}

	public long getMessageID() {
		return document.getLong("message");
	}

	public String getFilename() {
		return document.getString("filename");
	}

	public String getUser() {
		return document.getString("user", "Unknown");
	}
}