package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.util.MapWrapper;

public class UserWebhook extends WrappedDocument<UserWebhook> {
	public UserWebhook(WrappedCollection<UserWebhook> c, MapWrapper d) {
		super(c, d);
	}

	public String getWebhookID() {
		return document.getString("webhook_id");
	}

	public String getWebhookToken() {
		return document.getString("webhook_token");
	}

	public long getUserID() {
		return document.getLong("user");
	}

	public WebHook createWebhook() {
		return new WebHook(getWebhookID() + "/" + getWebhookToken());
	}
}