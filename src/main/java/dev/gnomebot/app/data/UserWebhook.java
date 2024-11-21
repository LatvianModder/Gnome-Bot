package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.WebHookDestination;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.SnowFlake;

public class UserWebhook extends WrappedDocument<UserWebhook> {
	public UserWebhook(WrappedCollection<UserWebhook> c, MapWrapper d) {
		super(c, d);
	}

	public long getWebhookID() {
		var o = document.get("webhook_id", 0L);

		if (o instanceof Number n) {
			return n.longValue();
		}

		return SnowFlake.num(String.valueOf(o));
	}

	public String getWebhookToken() {
		return document.getString("webhook_token");
	}

	public long getUserID() {
		return document.getLong("user");
	}

	public WebHookDestination createWebhook() {
		return new WebHookDestination(getWebhookID() + "/" + getWebhookToken());
	}
}