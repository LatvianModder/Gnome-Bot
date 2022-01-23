package dev.gnomebot.app.script;

import dev.gnomebot.app.App;
import dev.gnomebot.app.script.event.ButtonEventJS;
import dev.gnomebot.app.script.event.EventHandler;
import dev.gnomebot.app.script.event.MessageEventJS;

public class GuildScripts {
	public final transient App app;
	public final WrappedId id;
	public final EventHandler<MessageEventJS> onMessage = new EventHandler<>();
	public final EventHandler<MessageEventJS> onAfterMessage = new EventHandler<>();
	public final EventHandler<ButtonEventJS> onButton = new EventHandler<>();

	public GuildScripts(App a, WrappedId i) {
		app = a;
		id = i;
	}
}
