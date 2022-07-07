package dev.gnomebot.app.script.event;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.script.WrappedUser;

public class ButtonEventJS extends EventJS {
	public final String id;
	public final WrappedUser user;
	public final transient ComponentEventWrapper eventWrapper;

	public ButtonEventJS(String id, WrappedUser user, ComponentEventWrapper eventWrapper) {
		this.id = id;
		this.user = user;
		this.eventWrapper = eventWrapper;
	}

	@Override
	public void cancel() {
		super.cancel();
		eventWrapper.acknowledge();
	}
}
