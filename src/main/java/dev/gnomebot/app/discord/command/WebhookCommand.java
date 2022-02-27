package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.UserWebhook;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Message;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class WebhookCommand extends ApplicationCommands {
	public static final Pattern WEBHOOK_PATTERN = Pattern.compile("https://.*discord(?:app)?.com/api/webhooks/(\\d+)/([\\w-]+)");

	@RootCommand
	public static final CommandBuilder COMMAND = root("webhook")
			.description("Manage your webhooks")
			.add(sub("add")
					.add(string("name").required())
					.add(string("url").required())
					.run(WebhookCommand::add)
			)
			.add(sub("remove")
					.add(string("name").required())
					.run(WebhookCommand::remove)
			)
			.add(sub("list")
					.run(WebhookCommand::list)
			)
			.add(sub("execute")
					.add(channel("channel"))
					.add(string("edit_id").description("Message ID to edit, leave blank to post new message"))
					.run(WebhookCommand::execute)
			);

	private static void add(ApplicationCommandEventWrapper event) {
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

	private static void remove(ApplicationCommandEventWrapper event) {
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

	private static void list(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();
		List<String> list = new ArrayList<>();

		for (UserWebhook webhook : event.context.gc.db.userWebhooks.query().eq("user", event.context.sender.getId().asLong())) {
			list.add("- " + webhook.getName());
		}

		event.respond(list.isEmpty() ? "None" : String.join("\n", list));
	}

	private static void execute(ApplicationCommandEventWrapper event) {
		event.context.checkSenderOwner();
		ChannelInfo ci = event.get("channel").asChannelInfoOrCurrent();
		Snowflake editId = event.get("edit_id").asSnowflake();

		if (editId.asLong() != 0L) {
			Message message = Objects.requireNonNull(ci.getMessage(editId));

			event.respondModal("webhook/" + ci.id.asString() + "/" + editId.asString(), "Execute Webhook",
					TextInput.paragraph("content", "Content", 0, 2000).required(false).prefilled(message.getContent()),
					TextInput.paragraph("extra", "Extra").required(false).placeholder(MacroCommand.EXTRA_PLACEHOLDER)
			);
		} else {
			event.respondModal("webhook/" + ci.id.asString() + "/0", "Execute Webhook",
					TextInput.paragraph("content", "Content", 0, 2000).required(false),
					TextInput.paragraph("extra", "Extra").required(false).placeholder(MacroCommand.EXTRA_PLACEHOLDER),
					TextInput.small("username", "Username", 0, 100).required(false).placeholder("Override username"),
					TextInput.small("avatar_url", "Avatar URL").required(false).placeholder("Override avatar")
			);
		}
	}
}