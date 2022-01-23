package dev.gnomebot.app.script.event;

import dev.gnomebot.app.script.WrappedUser;

public class ButtonEventJS extends EventJS {
	public final String id;
	public final WrappedUser user;

	public ButtonEventJS(String id, WrappedUser user) {
		this.id = id;
		this.user = user;
	}
}
