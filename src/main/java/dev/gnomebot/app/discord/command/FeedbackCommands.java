package dev.gnomebot.app.discord.command;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.MessageEditSpec;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class FeedbackCommands extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("feedback")
			.description("Feedback")
			.add(sub("submit")
					.description("Open a suggestion form that will be post message in feedback channel for community to vote")
					.run(FeedbackCommands::submit)
			)
			.add(sub("approve")
					.description("Approves feedback")
					.add(string("reason"))
					.add(integer("id"))
					.run(FeedbackCommands::approve)
			)
			.add(sub("deny")
					.description("Denies feedback")
					.add(string("reason"))
					.add(integer("id"))
					.run(FeedbackCommands::deny)
			)
			.add(sub("consider")
					.description("Considers feedback")
					.add(string("reason"))
					.add(integer("id"))
					.run(FeedbackCommands::consider)
			)
			.add(sub("cleanup")
					.description("Removes all approved and denied suggestions")
					.run(FeedbackCommands::cleanup)
			);

	private static void submit(ApplicationCommandEventWrapper event) {
		ChannelInfo feedbackChannel = event.context.gc.feedbackChannel.messageChannel().orElse(null);

		if (feedbackChannel == null) {
			throw new DiscordCommandException("Feedback channel is not set up on this server!");
		} else if (!event.context.gc.feedbackSuggestRole.is(event.context.sender)) {
			throw new DiscordCommandException("To submit feedback you need " + event.context.gc.feedbackSuggestRole + " role!");
		}

		event.presentModal("feedback", "Submit Feedback", TextInput.paragraph("feedback", "Feedback", 15, 1500).placeholder("Write your feedback here! Please, don't send joke messages."));
	}

	private static void changeStatus(ApplicationCommandEventWrapper event, DiscordFeedback.Status status) {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		int id = event.get("id").asInt();

		if (id <= 0 || id > event.context.gc.feedbackNumber.get()) {
			throw error("Suggestion not found!");
		}

		DiscordFeedback feedback = event.context.gc.feedback.query().eq("number", id).first();

		if (feedback == null) {
			throw error("Suggestion not found!");
		}

		String reason = event.get("reason").asString("Not specified");

		List<Bson> updates = new ArrayList<>();
		updates.add(Updates.set("reason", reason));
		updates.add(Updates.set("reason_author", event.context.sender.getId().asLong()));
		updates.add(Updates.set("status", status.id));
		feedback.update(updates);

		feedback.document.map.put("reason", reason);
		feedback.document.map.put("reason_author", event.context.sender.getId().asLong());
		feedback.document.map.put("status", status.id);

		event.context.gc.findMessage(Snowflake.of(feedback.getUID()), event.context.gc.feedbackChannel.messageChannel().orElse(null)).ifPresent(message -> {
			EmbedCreateFields.Footer footer = Utils.getFooter(message);
			message.edit(MessageEditSpec.builder().addEmbed(feedback.edit(event.context.gc, footer)).build()).subscribe();

			if (!status.canEdit()) {
				Utils.editComponents(message, null);
			}
		});

		event.respond("Status of #" + id + " changed to " + status.titleSuffix);
	}

	private static void approve(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		changeStatus(event, DiscordFeedback.Status.APPROVED);
	}

	private static void deny(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		changeStatus(event, DiscordFeedback.Status.DENIED);
	}

	private static void consider(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		changeStatus(event, DiscordFeedback.Status.CONSIDERED);
	}

	private static void cleanup(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.context.checkSenderAdmin();
		event.acknowledge();

		ChannelInfo feedbackChannel = event.context.gc.feedbackChannel.messageChannel().orElse(null);

		if (feedbackChannel == null) {
			throw error("Feedback channel is not set up on this server!");
		}

		event.context.handler.app.queueBlockingTask(cancelled -> {
			try {
				int i = 0;

				for (DiscordFeedback feedback : event.context.gc.feedback.query().neq("deleted", true).neq("status", 0)) {
					feedback.update(Updates.set("deleted", true));
					App.info("Deleting #" + feedback.getNumber() + " - " + feedback.getStatus() + ": " + feedback.getReason());

					try {
						feedbackChannel.getMessage(Snowflake.of(feedback.getUID())).delete().block();
						i++;
					} catch (Exception ex) {
					}
				}

				event.respond("Done! Deleted " + i + " feedback messages");
			} catch (Exception ex) {
			}
		});
	}
}