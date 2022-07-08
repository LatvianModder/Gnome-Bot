package dev.gnomebot.app.script.event;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.script.WrappedUser;
import dev.gnomebot.app.util.MessageBuilder;

public class ComponentEventJS extends EventJS {
	public final String id;
	public final WrappedUser user;
	public final transient ComponentEventWrapper eventWrapper;

	public ComponentEventJS(String id, WrappedUser user, ComponentEventWrapper eventWrapper) {
		this.id = id;
		this.user = user;
		this.eventWrapper = eventWrapper;
	}

	public void acknowledge() {
		cancel();
		eventWrapper.acknowledge();
	}

	public void acknowledgeEphemeral() {
		cancel();
		eventWrapper.acknowledgeEphemeral();
	}

	public void reply(MessageBuilder message) {
		cancel();
		eventWrapper.respond(message);
	}
}
