package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.channel.ChannelInfo;
import dev.gnomebot.app.data.ping.Ping;
import dev.gnomebot.app.data.ping.PingData;
import dev.gnomebot.app.data.ping.PingDestination;
import dev.gnomebot.app.data.ping.UserPingConfig;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.log.Log;
import discord4j.core.object.entity.Webhook;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.service.WebhookService;
import reactor.core.publisher.Mono;

public class WebHookDestination implements PingDestination {
	public static final String BASE_URL = "https://discord.com/api/webhooks/";

	public static String getUrl(long id, String token) {
		return BASE_URL + SnowFlake.str(id) + "/" + token;
	}

	public final ChannelInfo channel;
	public final long id;
	public final String token;
	public final long threadId;

	public WebHookDestination(String path) {
		channel = null;

		var s = path.split("/");

		if (s.length == 1) {
			id = 0L;
			token = "";
			threadId = 0L;
		} else {
			id = SnowFlake.num(s[0]);
			token = s[1];
			threadId = SnowFlake.num(s.length >= 3 ? s[2] : "");
		}
	}

	public WebHookDestination(ChannelInfo c, Webhook webhook) {
		channel = c;
		id = webhook.getId().asLong();
		token = webhook.getToken().orElse("unknown");
		threadId = 0L;
	}

	private WebHookDestination(WebHookDestination from, ChannelInfo c, long t) {
		channel = c;
		id = from.id;
		token = from.token;
		threadId = t;
	}

	public WebHookDestination withThread(ChannelInfo c, long thread) {
		return threadId == thread ? this : new WebHookDestination(this, c, thread);
	}

	@Override
	public String toString() {
		return "WebHook{" + SnowFlake.str(id) + "/" + SnowFlake.str(threadId) + '}';
	}

	public WebhookService getWebhookService() {
		return App.instance.discordHandler.client.getRestClient().getWebhookService();
	}

	public Mono<MessageData> executeAndReturn(MessageBuilder message) {
		return getWebhookService().executeWebhook(id, token, true, message.toMultipartWebhookExecuteRequest());
	}

	public long execute(MessageBuilder message) {
		var body = "{}";

		try {
			var request = message.toMultipartWebhookExecuteRequest();
			body = Utils.bodyToString(request.getFiles().isEmpty() ? request.getJsonPayload() : request); // support multipart
			var result = URLRequest.of(getUrl(id, token))
					.query("wait", true)
					.query("thread_id", threadId == 0L ? null : SnowFlake.str(threadId))
					.contentType(request.getFiles().isEmpty() ? "application/json" : "multipart/form-data")
					.toJsonObject()
					.outString(body)
					.hiddenUrlPart(token)
					.block();

			if (result.containsKey("id")) {
				return SnowFlake.num(result.asString("id"));
			}

			return 0L;
		} catch (Exception ex) {
			Log.error("Failed to execute webhook " + SnowFlake.str(id) + " with body " + body);
			ex.printStackTrace();
			return 0L;
		}
	}

	public Mono<MessageData> edit(long messageId, MessageBuilder message) {
		return getWebhookService().modifyWebhookMessage(id, token, SnowFlake.str(messageId), message.toWebhookMessageEditRequest());
	}

	public Mono<Void> delete(long messageId) {
		return getWebhookService().deleteWebhookMessage(id, token, SnowFlake.str(messageId));
	}

	@Override
	public void relayPing(long targetId, PingData pingData, Ping ping, UserPingConfig config) {
		try {
			var targetUserName = targetId == 0L ? "Gnome" : pingData.gc().db.app.discordHandler.getUserName(targetId).orElse(SnowFlake.str(targetId));
			// Log.debug("Ping for WebHook[" + SnowFlake.str(id) + " of " + targetUserName + "] from " + pingData.username() + " @ **" + pingData.gc() + "** in " + pingData.channel().getName() + ": " + pingData.content() + " (" + ping.pattern() + ")");

			var content = new StringBuilder();

			if (config.silent()) {
				content.append("@silent ");
			}

			content.append("[Ping ➤](").append(pingData.url()).append(") from ").append(pingData.url()).append("\n").append(pingData.content());

			execute(MessageBuilder.create()
					.content(content.toString())
					.webhookName(pingData.username())
					.webhookAvatarUrl(pingData.avatar())
			);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
