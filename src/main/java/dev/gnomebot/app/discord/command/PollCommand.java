package dev.gnomebot.app.discord.command;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

public class PollCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("poll")
			.description("Create a poll")
			.run(PollCommand::submit);

	private static final List<String> NO_YES = List.of("No", "Yes");

	private static void submit(ChatInputInteractionEventWrapper event) {
		event.respondModal("poll", "Create a Poll",
				TextInput.paragraph("question", "Question").required(true).placeholder("Is today a good day?"),
				TextInput.paragraph("options", "Options").required(false).placeholder("No\nYes")
		);
	}

	public static void submitCallback(ModalEventWrapper event) {
		var question = event.get("question").asString();
		var message = MessageBuilder.create("## " + question);
		var optionsStr = event.get("options").asString();
		var options = optionsStr.isEmpty() || optionsStr.equalsIgnoreCase("No\nYes") || optionsStr.equalsIgnoreCase("Yes\nNo") ? NO_YES : new LinkedHashSet<>(Arrays.stream(optionsStr.split("\n")).filter(s -> !s.isBlank()).limit(25L).toList());

		if (options.size() < 2) {
			throw new GnomeException("Poll must have at least 2 options!");
		} else if (options.size() > 25) {
			throw new GnomeException("Poll has too many options! (Max 25)");
		}

		int number = ++event.context.gc.pollNumber;
		event.context.gc.saveInfo();

		var components = new ArrayList<ActionComponent>();

		if (options == NO_YES) {
			components.add(Button.danger("poll-vote/" + number + "/0", "No (0)"));
			components.add(Button.success("poll-vote/" + number + "/1", "Yes (0)"));
		} else {
			int num = 0;

			for (var option : options) {
				var str = " (0)";
				components.add(Button.secondary("poll-vote/" + number + "/" + num, FormattingUtils.trim(option, 80 - str.length()) + str));
				num++;
			}
		}

		message.dynamicComponents(components);
		message.ephemeral(false);
		event.respond(message);

		var document = new Document();
		document.put("_id", event.event.getInteraction().getId().asLong());
		document.put("author", event.event.getInteraction().getUser().getId().asLong());
		document.put("timestamp", Date.from(event.event.getInteraction().getId().getTimestamp()));
		document.put("channel", event.context.channelInfo.id);
		document.put("number", number);
		document.put("content", question);
		document.put("options", options);
		document.put("votes", new BasicDBObject());
		event.context.gc.polls.insert(document);
	}

	public static void buttonCallback(ComponentEventWrapper event, int number, int vote) {
		var poll = event.context.gc.polls.query().eq("number", number).first();

		if (poll != null) {
			var options = poll.getOptions();

			if (options.equals(NO_YES)) {
				options = NO_YES;
			}

			int[] votes = new int[options.size()];

			poll.getVotes().map.put(event.context.sender.getId().asString(), vote);

			for (var entry : poll.getVotes().map.values()) {
				votes[((Number) entry).intValue()]++;
			}

			var components = new ArrayList<ActionComponent>();

			if (options == NO_YES) {
				components.add(Button.danger("poll-vote/" + number + "/0", "No (" + votes[0] + ")"));
				components.add(Button.success("poll-vote/" + number + "/1", "Yes (" + votes[1] + ")"));
			} else {
				int num = 0;

				for (var option : options) {
					var str = " (" + votes[num] + ")";
					components.add(Button.secondary("poll-vote/" + number + "/" + num, FormattingUtils.trim(option, 80 - str.length()) + str));
					num++;
				}
			}

			var message = MessageBuilder.create("## " + poll.getContent());
			message.dynamicComponents(components);
			message.ephemeral(false);
			event.edit().respond(message);

			event.context.gc.polls.getCollection().updateOne(Filters.eq(poll.getUID()), Updates.set("votes." + event.context.sender.getId().asLong(), vote));
		} else {
			event.acknowledge();
		}
	}
}