package dev.gnomebot.app.script.event;

import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.script.WrappedMessage;

public class MessageEventJS extends EventJS {
	public final WrappedMessage message;
	public final long totalMessages;
	public final long totalXp;
	public CommandReader reader;

	public MessageEventJS(WrappedMessage message, long totalMessages, long totalXp) {
		this.message = message;
		this.message.messageEvent = this;
		this.totalMessages = totalMessages;
		this.totalXp = totalXp;
	}
}
