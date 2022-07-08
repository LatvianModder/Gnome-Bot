package dev.gnomebot.app.script.event;

import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.command.CommandOption;
import dev.gnomebot.app.script.WrappedUser;

public class ModalEventJS extends ComponentEventJS {
	public final transient ModalEventWrapper modalEventWrapper;

	public ModalEventJS(String id, WrappedUser user, ModalEventWrapper modalEventWrapper) {
		super(id, user, modalEventWrapper);
		this.modalEventWrapper = modalEventWrapper;
	}

	public boolean has(String id) {
		return modalEventWrapper.has(id);
	}

	public CommandOption get(String id) {
		return modalEventWrapper.get(id);
	}
}
