package dev.gnomebot.app.discord.command;

import com.google.gson.JsonArray;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.UserWebhook;
import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.component.MessageComponent;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.discordjson.possible.Possible;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
					.add(string("content").description("Use ^https://rawlink.txt to fetch content from URL"))
					.add(string("buttons").description("Use ^https://rawlink.json to fetch json from URL"))
					.add(string("edit_id").description("Message ID to edit, dont set to post new message"))
					.add(string("thread_id").description("Thread ID to post in"))
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

		WebHook w = event.get("name").asWebhook().orElse(null);

		if (w == null) {
			throw new DiscordCommandException("Webhook not found! Try `/webhook list`");
		}

		String content = event.get("content").asContentOrFetch();
		String buttons = event.get("buttons").asContentOrFetch();

		Possible<List<ComponentData>> components = Possible.absent();

		if (!buttons.isEmpty()) {
			JsonArray a = Utils.GSON.fromJson(buttons, JsonArray.class);
			components = Possible.of(Utils.parseRows(a).stream().map(MessageComponent::getData).collect(Collectors.toList()));
		}

		if (content.isEmpty() && components.isAbsent()) {
			throw new DiscordCommandException("No content or components!");
		}

		String editId = event.get("edit_id").asString();
		String threadId = event.get("thread_id").asString();

		if (!threadId.isEmpty()) {
			w = w.withThread(threadId);
		}

		if (editId.isEmpty()) {
			w.execute(WebhookExecuteRequest.builder()
					.allowedMentions(DiscordMessage.noMentions().toData())
					.content(content)
					.components(components)
					.build()
			);
		} else {
			w.edit(editId, WebhookMessageEditRequest.builder()
					.allowedMentionsOrNull(DiscordMessage.noMentions().toData())
					.content(content.isEmpty() ? Possible.absent() : Possible.of(Optional.of(content)))
					.components(components)
					.build()
			).block();
		}

		event.respond("Done!");
	}
}