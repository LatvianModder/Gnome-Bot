package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

public class CommandLogEntry extends WrappedDocument<CommandLogEntry> {
	public CommandLogEntry(WrappedCollection<CommandLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	public long user() {
		return document.getLong("user");
	}

	public long channel() {
		return document.getLong("channel");
	}

	public long message() {
		return document.getLong("message");
	}

	public String command() {
		return document.getString("command");
	}

	public String fullCommand() {
		return document.getString("full_command");
	}
}