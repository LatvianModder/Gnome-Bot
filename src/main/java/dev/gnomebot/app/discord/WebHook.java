package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Webhook;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.rest.service.WebhookService;
import discord4j.rest.util.MultipartRequest;
import reactor.core.publisher.Mono;

/**
 * @author LatvianModder
 */
public class WebHook {
	public static final String BASE_URL = "https://discord.com/api/webhooks/";

	public final Snowflake id;
	public final String token;
	public final String threadId;

	public WebHook(String path) {
		threadId = "";

		int si = path.indexOf('/');

		if (si == -1) {
			id = Snowflake.of(0L);
			token = "";
		} else {
			id = Snowflake.of(path.substring(0, si));
			token = path.substring(si + 1);
		}
	}

	public WebHook(Webhook webhook) {
		id = webhook.getId();
		token = webhook.getToken().orElse("unknown");
		threadId = "";
	}

	private WebHook(WebHook from, String t) {
		id = from.id;
		token = from.token;
		threadId = t;
	}

	public WebHook withThread(String thread) {
		return new WebHook(this, thread);
	}

	public WebhookService getWebhookService() {
		return App.instance.discordHandler.client.getRestClient().getWebhookService();
	}

	public Mono<MessageData> executeAndReturn(WebhookExecuteRequest request) {
		return getWebhookService().executeWebhook(id.asLong(), token, true, MultipartRequest.ofRequest(request));
	}

	public boolean execute(WebhookExecuteRequest request) {
		String body = "{}";

		try {
			body = Utils.bodyToString(request);
			URLRequest.of(BASE_URL + id.asString() + "/" + token).query("wait", true).query("thread_id", threadId.isEmpty() ? null : threadId).contentType("application/json").toJson().outString(body).hiddenUrlPart(token).block();
			return true;
		} catch (Exception ex) {
			App.error("Failed to execute webhook " + id.asString() + " with body " + body);
			ex.printStackTrace();
			return false;
		}
	}

	public Mono<MessageData> edit(String messageId, WebhookMessageEditRequest request) {
		return getWebhookService().modifyWebhookMessage(id.asLong(), token, messageId, request);
	}

	public Mono<Void> delete(String messageId) {
		return getWebhookService().deleteWebhookMessage(id.asLong(), token, messageId);
	}
}
