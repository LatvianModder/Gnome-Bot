package dev.gnomebot.app.script;

import dev.gnomebot.app.App;
import dev.gnomebot.app.script.event.MessageEventJS;

import java.util.ArrayList;
import java.util.List;

public class GuildScripts {
	public final transient App app;
	public final WrappedId id;
	public transient List<MessageEventJS> onMessageHandlers;
	public transient List<MessageEventJS> onAfterMessageHandlers;

	public GuildScripts(App a, WrappedId i) {
		app = a;
		id = i;
		onMessageHandlers = null;
		onAfterMessageHandlers = null;
	}

	public void setOnMessage(MessageEventJS event) {
		if (onMessageHandlers == null) {
			onMessageHandlers = new ArrayList<>(1);
		}

		onMessageHandlers.add(event);
	}

	public void setOnAfterMessage(MessageEventJS event) {
		if (onAfterMessageHandlers == null) {
			onAfterMessageHandlers = new ArrayList<>(1);
		}

		onAfterMessageHandlers.add(event);
	}
}
