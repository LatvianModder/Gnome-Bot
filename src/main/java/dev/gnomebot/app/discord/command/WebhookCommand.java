package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.UserWebhook;
import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
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
					.add(string("name").required())
					//.add(string("content").description("Use ^https://rawlink.txt to fetch content from URL"))
					//.add(string("buttons").description("Use ^https://rawlink.json to fetch json from URL"))
					//.add(string("edit_id").description("Message ID to edit, dont set to post new message"))
					//.add(string("thread_id").description("Thread ID to post in"))
					.run(WebhookCommand::execute)
			);

	private static void add(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledgeEphemeral();
		String n = event.get("name").asString().trim().toLowerCase();

		if (n.isEmpty() || event.context.gc.db.userWebhooks.query().eq("name", n).eq("user", event.context.sender.getId().asLong()).first() != null) {
			throw new DiscordCommandException("Invalid or taken name!");
		}

		Matcher matcher = WEBHOOK_PATTERN.matcher(event.get("url").asString());

		if (!matcher.matches()) {
			throw new DiscordCommandException("Invalid webhook URL!");
		}

		Document document = new Document();
		document.put("user", event.context.sender.getId().asLong());
		document.put("name", n);
		document.put("webhook_id", matcher.group(1));
		document.put("webhook_token", matcher.group(2));
		event.context.gc.db.userWebhooks.insert(document);

		event.respond("Webhook '" + n + "' added!");
	}

	private static void remove(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledgeEphemeral();
		String n = event.get("name").asString().trim().toLowerCase();

		UserWebhook webhook = event.context.gc.db.userWebhooks.query().eq("name", n).eq("user", event.context.sender.getId().asLong()).first();

		if (webhook == null) {
			throw new DiscordCommandException("Webhook not found! Try `/webhook list`");
		}

		webhook.delete();
		event.respond("Webhook deleted!");
	}

	private static void list(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledgeEphemeral();
		List<String> list = new ArrayList<>();

		for (UserWebhook webhook : event.context.gc.db.userWebhooks.query().eq("user", event.context.sender.getId().asLong())) {
			list.add("- " + webhook.getName());
		}

		event.respond(list.isEmpty() ? "None" : String.join("\n", list));
	}

	private static void execute(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		WebHook w = event.get("name").asWebhook().orElse(null);

		if (w == null) {
			throw new DiscordCommandException("Webhook not found! Try `/webhook list`");
		}

		throw new DiscordCommandException("WIP!");

		/*
		String content = event.get("content").asContentOrFetch();
		String buttons = event.get("buttons").asContentOrFetch();

		List<LayoutComponent> components = null;

		if (!buttons.isEmpty()) {
			JsonArray a = Utils.GSON.fromJson(buttons, JsonArray.class);
			components = Utils.parseRows(a);
		}

		if (content.isEmpty() && components == null) {
			throw new DiscordCommandException("No content or components!");
		}

		String editId = event.get("edit_id").asString();
		String threadId = event.get("thread_id").asString();

		if (!threadId.isEmpty()) {
			w = w.withThread(threadId);
		}

		MessageBuilder builder = MessageBuilder.create();

		if (!content.isEmpty()) {
			builder.content(content);
		}

		builder.components(components);

		if (editId.isEmpty()) {
			w.execute(builder);
		} else {
			w.edit(editId, builder).block();
		}

		event.respond("Done!");
		 */
	}
}