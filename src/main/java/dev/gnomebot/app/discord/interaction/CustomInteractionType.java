package dev.gnomebot.app.discord.interaction;

public final class CustomInteractionType {
	public final String id;
	public final CustomInteractionCallback.Provider callback;
	public boolean persistent;
	public boolean keep;

	public CustomInteractionType(String id, CustomInteractionCallback.Provider callback) {
		this.id = id;
		this.callback = callback;
		persistent = false;
		keep = false;
	}

	public CustomInteractionType persistent() {
		persistent = true;
		return this;
	}

	public CustomInteractionType keep() {
		keep = true;
		return this;
	}
}
