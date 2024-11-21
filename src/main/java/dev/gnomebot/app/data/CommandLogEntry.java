package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

public class CommandLogEntry extends WrappedDocument<CommandLogEntry> {
	public CommandLogEntry(WrappedCollection<CommandLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	public long getUser() {
		return document.getLong("user");
	}

	public long getChannel() {
		return document.getLong("channel");
	}

	public long getMessage() {
		return document.getLong("message");
	}

	public String getCommand() {
		return document.getString("command");
	}

	public String getFullCommand() {
		return document.getString("full_command");
	}
}