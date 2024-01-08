package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.ping.Ping;
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

		var s = path.split("/");

		if (s.length == 1) {
			id = Utils.NO_SNOWFLAKE;
			token = "";
			threadId = "";
		} else {
			id = Utils.snowflake(s[0]);
			token = s[1];
			threadId = s.length >= 3 ? s[2] : "";
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

	public Snowflake execute(MessageBuilder message) {
		String body = "{}";

		try {
			MultipartRequest<? extends WebhookExecuteRequest> request = message.toMultipartWebhookExecuteRequest();
			body = Utils.bodyToString(request.getFiles().isEmpty() ? request.getJsonPayload() : request); // support multipart
			var result = URLRequest.of(getUrl(id.asString(), token))
					.query("wait", true)
					.query("thread_id", threadId.isEmpty() ? null : threadId)
					.contentType(request.getFiles().isEmpty() ? "application/json" : "multipart/form-data")
					.toJsonObject()
					.outString(body)
					.hiddenUrlPart(token)
					.block();

			if (result.containsKey("id")) {
				return Utils.snowflake(result.asString("id"));
			}

			return Utils.NO_SNOWFLAKE;
		} catch (Exception ex) {
			App.error("Failed to execute webhook " + id.asString() + " with body " + body);
			ex.printStackTrace();
			return Utils.NO_SNOWFLAKE;
		}
	}

	public Mono<MessageData> edit(String messageId, MessageBuilder message) {
		return getWebhookService().modifyWebhookMessage(id.asLong(), token, messageId, message.toWebhookMessageEditRequest());
	}

	public Mono<Void> delete(String messageId) {
		return getWebhookService().deleteWebhookMessage(id.asLong(), token, messageId);
	}

	@Override
	public void relayPing(Snowflake targetId, PingData pingData, Ping ping) {
		try {
			var targetUserName = targetId.asLong() == 0L ? "Gnome" : pingData.gc().db.app.discordHandler.getUserName(targetId).orElse(targetId.asString());
			App.info("Ping for WebHook[" + id.asString() + " of " + targetUserName + "] from " + pingData.username() + " @ **" + pingData.gc() + "** in " + pingData.channel().getName() + ": " + pingData.content() + " (" + ping.pattern() + ")");

			execute(MessageBuilder.create()
					.content("[Ping ➤](" + pingData.url() + ") from " + pingData.url() + "\n" + pingData.content())
					.webhookName(pingData.username())
					.webhookAvatarUrl(pingData.avatar())
			);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
