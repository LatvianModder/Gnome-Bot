package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.ping.PingData;
import dev.gnomebot.app.data.ping.PingDestination;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Webhook;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.service.WebhookService;
import discord4j.rest.util.MultipartRequest;
import reactor.core.publisher.Mono;

/**
 * @author LatvianModder
 */
public class WebHook implements PingDestination {
	public static final String BASE_URL = "https://discord.com/api/webhooks/";

	public static String getUrl(String id, String token) {
		return BASE_URL + id + "/" + token;
	}

	public final ChannelInfo channel;
	public final Snowflake id;
	public final String token;
	public final String threadId;

	public WebHook(String path) {
		channel = null;
		threadId = "";

		int si = path.indexOf('/');

		if (si == -1) {
			id = Utils.NO_SNOWFLAKE;
			token = "";
		} else {
			id = Snowflake.of(path.substring(0, si));
			token = path.substring(si + 1);
		}
	}

	public WebHook(ChannelInfo c, Webhook webhook) {
		channel = c;
		id = webhook.getId();
		token = webhook.getToken().orElse("unknown");
		threadId = "";
	}

	private WebHook(WebHook from, ChannelInfo c, String t) {
		channel = c;
		id = from.id;
		token = from.token;
		threadId = t;
	}

	public WebHook withThread(ChannelInfo c, String thread) {
		return threadId.equals(thread) ? this : new WebHook(this, c, thread);
	}

	@Override
	public String toString() {
		return "WebHook{" + id.asString() + "/" + threadId + '}';
	}

	public WebhookService getWebhookService() {
		return App.instance.discordHandler.client.getRestClient().getWebhookService();
	}

	public Mono<MessageData> executeAndReturn(MessageBuilder message) {
		return getWebhookService().executeWebhook(id.asLong(), token, true, message.toMultipartWebhookExecuteRequest());
	}

	public boolean execute(MessageBuilder message) {
		String body = "{}";

		try {
			MultipartRequest<? extends WebhookExecuteRequest> request = message.toMultipartWebhookExecuteRequest();
			body = Utils.bodyToString(request.getFiles().isEmpty() ? request.getJsonPayload() : request); // support multipart
			URLRequest.of(getUrl(id.asString(), token))
					.query("wait", true)
					.query("thread_id", threadId.isEmpty() ? null : threadId)
					.contentType(request.getFiles().isEmpty() ? "application/json" : "multipart/form-data")
					.toJson()
					.outString(body)
					.hiddenUrlPart(token)
					.block();
			return true;
		} catch (Exception ex) {
			App.error("Failed to execute webhook " + id.asString() + " with body " + body);
			ex.printStackTrace();
			return false;
		}
	}

	public Mono<MessageData> edit(String messageId, MessageBuilder message) {
		return getWebhookService().modifyWebhookMessage(id.asLong(), token, messageId, message.toWebhookMessageEditRequest());
	}

	public Mono<Void> delete(String messageId) {
		return getWebhookService().deleteWebhookMessage(id.asLong(), token, messageId);
	}

	@Override
	public void relayPing(PingData pingData) {
		try {
			execute(MessageBuilder.create()
					.content("[Ping ➤](" + pingData.url() + ") " + pingData.content())
					.webhookName(pingData.username() + " [" + pingData.gc() + "]")
					.webhookAvatarUrl(pingData.avatar())
			);
		} catch (Exception ex) {
		}
	}
}
