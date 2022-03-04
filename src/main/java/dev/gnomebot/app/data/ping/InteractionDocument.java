package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.data.WrappedCollection;
import dev.gnomebot.app.data.WrappedDocument;
import dev.gnomebot.app.util.MapWrapper;

/**
 * @author LatvianModder
 */
public class InteractionDocument extends WrappedDocument<InteractionDocument> {
	public InteractionDocument(WrappedCollection<InteractionDocument> c, MapWrapper d) {
		super(c, d);
	}

	public String getType() {
		return document.getString("type");
	}
}