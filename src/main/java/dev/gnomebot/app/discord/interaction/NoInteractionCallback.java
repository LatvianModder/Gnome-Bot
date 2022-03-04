package dev.gnomebot.app.discord.interaction;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.util.MapWrapper;
import org.bson.Document;

public class NoInteractionCallback extends CustomInteractionCallback {
	public NoInteractionCallback(MapWrapper data) {
		super(data);
	}

	@Override
	public CustomInteractionType getType() {
		return CustomInteractionTypes.NONE;
	}

	@Override
	public void execute(ComponentEventWrapper event) {
	}

	@Override
	public void save(Document document) {
	}
}
