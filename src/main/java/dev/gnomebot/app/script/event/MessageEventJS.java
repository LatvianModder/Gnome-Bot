package dev.gnomebot.app.script.event;

import dev.gnomebot.app.script.WrappedMessage;

@FunctionalInterface
public interface MessageEventJS {
	void onMessage(WrappedMessage message);
}
