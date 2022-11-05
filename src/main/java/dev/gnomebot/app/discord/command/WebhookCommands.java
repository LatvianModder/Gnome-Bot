package dev.gnomebot.app.discord.command;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.data.UserWebhook;
import dev.gnomebot.app.data.WebhookExecuteExtra;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Message;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class WebhookCommands extends ApplicationCommands {
	public static final Pattern WEBHOOK_PATTERN = Pattern.compile("https://.*discord(?:app)?.com/api/(?:v\\d+/)?webhooks/(\\d+)/([\\w-]+)");

	@RegisterCommand
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
					.add(string("edit_id").description("Message ID to edit, leave blank to post new message"))
					.run(WebhookCommands::execute)
			);

	private static void add(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		String n = event.get("name").asString().trim().toLowerCase();

		try {
			Snowflake.of(n);
			throw new GnomeException("Invalid or taken name!");
		} catch (Exception ex) {
		}

		if (n.isEmpty() || n.length() > 50 || event.context.gc.db.userWebhooks.query().eq("name", n).eq("user", event.context.sender.getId().asLong()).first() != null) {
			throw new GnomeException("Invalid or taken name!");
		}

		Matcher matcher = WEBHOOK_PATTERN.matcher(event.get("url").asString());

		if (!matcher.matches()) {
			throw new GnomeException("Invalid webhook URL!");
		}

		Document document = new Document();
		document.put("user", event.context.sender.getId().asLong());
		document.put("name", n);
		document.put("webhook_id", matcher.group(1));
		document.put("webhook_token", matcher.group(2));
		event.context.gc.db.userWebhooks.insert(document);

		event.respond("Webhook '" + n + "' added!");
		event.context.gc.db.app.pingHandler.update();
	}

	private static void remove(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		String n = event.get("name").asString().trim().toLowerCase();

		UserWebhook webhook = event.context.gc.db.userWebhooks.query().eq("name", n).eq("user", event.context.sender.getId().asLong()).first();

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

		for (UserWebhook webhook : event.context.gc.db.userWebhooks.query().eq("user", event.context.sender.getId().asLong())) {
			list.add("- " + webhook.getName() + " - ||[URL](<" + WebHook.getUrl(webhook.getWebhookID(), webhook.getWebhookToken()) + ">)||");
		}

		event.respond(list.isEmpty() ? "None" : String.join("\n", list));
	}

	private static void execute(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderOwner();
		ChannelInfo ci = event.get("channel").asChannelInfoOrCurrent();
		Snowflake editId = event.get("edit_id").asSnowflake();

		if (editId.asLong() != 0L) {
			Message message = Objects.requireNonNull(ci.getMessage(editId));
			WebhookExecuteExtra info = event.context.gc.db.webhookExecuteExtra.findFirst(editId.asLong());

			event.respondModal("webhook/" + ci.id.asString() + "/" + editId.asString(), "Execute Webhook",
					TextInput.paragraph("content", "Content", 0, 2000).required(false).prefilled(message.getContent()),
					TextInput.paragraph("extra", "Extra").required(false).prefilled(info == null ? "" : info.getExtra()).placeholder(MacroCommands.EXTRA_PLACEHOLDER)
			);
		} else {
			event.respondModal("webhook/" + ci.id.asString() + "/0", "Execute Webhook",
					TextInput.paragraph("content", "Content", 0, 2000).required(false),
					TextInput.paragraph("extra", "Extra").required(false).placeholder(MacroCommands.EXTRA_PLACEHOLDER),
					TextInput.small("username", "Username", 0, 100).required(false).placeholder("Override username"),
					TextInput.small("avatar_url", "Avatar URL").required(false).placeholder("Override avatar")
			);
		}
	}

	public static void executeCallback(ModalEventWrapper event, Snowflake channelId, Snowflake editId) {
		event.context.checkSenderOwner();
		ChannelInfo ci = event.context.gc.getOrMakeChannelInfo(channelId);
		WebHook webHook = ci.getWebHook().orElse(null);

		if (webHook == null) {
			throw new GnomeException("Failed to retrieve webhook!");
		}

		String content = event.get("content").asString();
		List<String> extra = Arrays.asList(event.get("extra").asString().split("\n"));
		MessageBuilder message = Macro.createMessage(content, extra, Utils.NO_SNOWFLAKE, false);
		message.webhookName(event.get("username").asString(event.context.gc.toString()));
		message.webhookAvatarUrl(event.get("avatar_url").asString(event.context.gc.iconUrl.get()));

		Snowflake id = editId.asLong() == 0L ? webHook.execute(message) : editId;

		if (editId.asLong() != 0L) {
			webHook.edit(editId.asString(), message).block();

			WebhookExecuteExtra info = event.context.gc.db.webhookExecuteExtra.findFirst(editId.asLong());

			if (info != null) {
				info.update(Updates.set("extra", String.join("\n", extra)));
			}

			event.respond("Done!");
		} else if (id != Utils.NO_SNOWFLAKE) {
			Document doc = new Document();
			doc.put("_id", id.asLong());
			doc.put("extra", String.join("\n", extra));
			event.context.gc.db.webhookExecuteExtra.insert(doc);
			event.respond("Done!");
		} else {
			throw new GnomeException("Failed to send webhook!");
		}

		// Click this button (Or do `??gamenight` in <#397702104204050434> if it doesn't work):
		// macro "Toggle Game Night Role" gamenight
	}
}