package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ContentType;
import dev.gnomebot.app.data.complex.ComplexMessage;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Message;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WebhookCommands extends ApplicationCommands {
	public static final Pattern WEBHOOK_PATTERN = Pattern.compile("https://.*discord(?:app)?.com/api/(?:v\\d+/)?webhooks/(\\d+)/([\\w-]+)");

	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("webhook")
			.description("Manage your webhooks")
			.add(sub("add")
					.add(string("name").required())
					.add(string("url").required())
					.run(WebhookCommands::add)
			)
			.add(sub("remove")
					.add(string("name").required())
					.run(WebhookCommands::remove)
			)
			.add(sub("list")
					.run(WebhookCommands::list)
			)
			.add(sub("execute")
					.add(channel("channel"))
					.run(WebhookCommands::execute)
			);

	private static void add(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		var n = event.get("name").asString().trim().toLowerCase();

		if (SnowFlake.num(n) != 0L) {
			throw new GnomeException("Invalid or taken name!");
		}

		if (n.isEmpty() || n.length() > 50 || event.context.gc.db.userWebhooksDB.query().eq("name", n).eq("user", event.context.sender.getId().asLong()).first() != null) {
			throw new GnomeException("Invalid or taken name!");
		}

		var matcher = WEBHOOK_PATTERN.matcher(event.get("url").asString());

		if (!matcher.matches()) {
			throw new GnomeException("Invalid webhook URL!");
		}

		var document = new Document();
		document.put("user", event.context.sender.getId().asLong());
		document.put("name", n);
		document.put("webhook_id", SnowFlake.num(matcher.group(1)));
		document.put("webhook_token", matcher.group(2));
		event.context.gc.db.userWebhooksDB.insert(document);

		event.respond("Webhook '" + n + "' added!");
		event.context.gc.db.app.pingHandler.update();
	}

	private static void remove(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		var n = event.get("name").asString().trim().toLowerCase();

		var webhook = event.context.gc.db.userWebhooksDB.query().eq("name", n).eq("user", event.context.sender.getId().asLong()).first();

		if (webhook == null) {
			throw new GnomeException("Webhook not found! Try `/webhook list`");
		}

		webhook.delete();
		event.respond("Webhook deleted!");
		event.context.gc.db.app.pingHandler.update();
	}

	private static void list(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		List<String> list = new ArrayList<>();

		for (var webhook : event.context.gc.db.userWebhooksDB.query().eq("user", event.context.sender.getId().asLong())) {
			list.add("- " + webhook.getName() + " - ||[URL](<" + WebHook.getUrl(webhook.getWebhookID(), webhook.getWebhookToken()) + ">)||");
		}

		event.respond(list.isEmpty() ? "None" : String.join("\n", list));
	}

	private static void execute(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderOwner();
		var ci = event.get("channel").asChannelInfoOrCurrent();

		event.respondModal("webhook/" + ci.id + "/0", "Execute Webhook",
				TextInput.paragraph("content", "Content", 0, 2000).required(false),
				TextInput.small("username", "Username", 0, 100).required(false).placeholder("Override username"),
				TextInput.small("avatar_url", "Avatar URL").required(false).placeholder("Override avatar")
		);
	}

	public static void executeCallback(ModalEventWrapper event, long channelId, long editId) {
		event.context.checkSenderOwner();
		var ci = event.context.gc.getOrMakeChannelInfo(channelId);
		var webHook = ci.getWebHook().orElse(null);

		if (webHook == null) {
			throw new GnomeException("Failed to retrieve webhook!");
		}

		var content = ContentType.parse(event.context.gc, event.get("content").asString());
		var message = content.a().render(event.context.gc, null, content.b(), 0L);

		if (editId == 0L) {
			message.webhookName(event.get("username").asString(event.context.gc.toString()));
			message.webhookAvatarUrl(event.get("avatar_url").asString(event.context.gc.iconUrl));
			var id = webHook.execute(message);

			if (id != 0L) {
				event.respond("Done!");
			} else {
				throw new GnomeException("Failed to send webhook!");
			}
		} else {
			try {
				webHook.edit(editId, message).block();
				event.respond("Done!");
			} catch (Exception ex) {
				throw new GnomeException("Failed to edit webhook message!");
			}
		}
	}

	public static void editMessage(Message message, ComponentEventWrapper event) {
		event.context.checkSenderOwner();

		if (ComplexMessage.has(message)) {
			var lines = ComplexMessage.of(event.context.gc, message).getLines();

			event.respondModal("webhook/" + event.context.channelInfo.id + "/" + message.getId().asString(), "Edit Webhook Message",
					TextInput.paragraph("content", "Content").required(true).prefilled(String.join("\n", lines)).placeholder(MacroCommands.COMPLEX_PLACEHOLDER)
			);
		} else {
			event.respondModal("webhook/" + event.context.channelInfo.id + "/" + message.getId().asString(), "Edit Webhook Message",
					TextInput.paragraph("content", "Content").required(true).prefilled(message.getContent()).placeholder(MacroCommands.COMPLEX_PLACEHOLDER)
			);
		}
	}
}