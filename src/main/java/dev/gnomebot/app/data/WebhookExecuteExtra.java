package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

/**
 * @author LatvianModder
 */
public class WebhookExecuteExtra extends WrappedDocument<WebhookExecuteExtra> {
	public WebhookExecuteExtra(WrappedCollection<WebhookExecuteExtra> c, MapWrapper d) {
		super(c, d);
	}

	public String getExtra() {
		return document.getString("extra");
	}
}