package dev.gnomebot.app.discord.interaction;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.UUIDWrapper;
import org.bson.Document;

import java.util.UUID;

public abstract class CustomInteractionCallback {
	public interface Provider {
		CustomInteractionCallback create(MapWrapper map);
	}

	public final UUID id;

	public CustomInteractionCallback(MapWrapper data) {
		id = new UUID(data.getLong("idm"), data.getLong("idl"));
	}

	public abstract CustomInteractionType getType();

	public final String getComponentId() {
		return "custom/" + getType().id + "/" + UUIDWrapper.toString(id);
	}

	public abstract void execute(ComponentEventWrapper event);

	public abstract void save(Document document);
}
