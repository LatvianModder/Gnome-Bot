package dev.gnomebot.app.discord.command;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.QuoteHandler;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.ThreadMessageRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Permission;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public class FeedbackCommands extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("feedback")
			.description("Feedback")
			.add(sub("submit")
					.description("Create a suggestion that will be posted in feedback channel for community to vote")
					.add(string("text").required())
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

	private static void submit(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledgeEphemeral();
		ChannelInfo feedbackChannel = event.context.gc.feedbackChannel.messageChannel().orElse(null);

		if (feedbackChannel == null) {
			throw error("Feedback channel is not set up on this server!");
		}

		String suggestion = event.get("text").asString();

		if (suggestion.length() < 15) {
			throw error("Your suggestion title is too short, requires at least 15 characters!");
		}

		if (!event.context.gc.feedbackSuggestRole.is(event.context.sender)) {
			throw error("To submit feedback you need " + event.context.gc.feedbackSuggestRole + " role!");
		}

		int number = event.context.gc.feedbackNumber.get() + 1;
		event.context.gc.feedbackNumber.set(number);
		event.context.gc.feedbackNumber.save();

		event.context.referenceMessage = false;

		event.context.checkBotPerms(feedbackChannel, Permission.ADD_REACTIONS, Permission.SEND_MESSAGES);

		Message m = feedbackChannel.createMessage(MessageCreateSpec.builder()
				.addEmbed(EmbedCreateSpec.builder()
						.color(EmbedColors.GRAY)
						.url("https://gnomebot.dev/feedback/" + event.context.gc.guildId.asString() + "/" + number)
						.title("Loading suggestion #" + number + "...")
						.build()
				)
				.build()
		).block();

		Document document = new Document();
		document.put("_id", m.getId().asLong());
		document.put("author", event.context.sender.getId().asLong());
		document.put("timestamp", Date.from(m.getTimestamp()));
		document.put("number", number);
		document.put("content", suggestion);
		document.put("status", 0);
		BasicDBObject votes = new BasicDBObject();
		votes.put(event.context.sender.getId().asString(), true);
		document.put("votes", votes);
		event.context.gc.feedback.insert(document);
		m.edit(MessageEditSpec.builder().addEmbed(event.context.gc.feedback.findFirst(m).edit(event.context.gc, event.context.gc.anonymousFeedback.get() ? null : EmbedCreateFields.Footer.of(event.context.sender.getTag(), event.context.sender.getAvatarUrl()))).build()).block();

		try {
			Utils.THREAD_ROUTE.newRequest(m.getChannelId().asLong(), m.getId().asLong())
					.body(new ThreadMessageRequest("Discussion of " + number))
					.exchange(event.context.handler.client.getCoreResources().getRouter())
					.skipBody()
					.block();
		} catch (Exception ex) {
			App.error("Failed to create a thread for suggestion " + event.context.gc + "/#" + number);
		}

		m.edit(MessageEditSpec.builder().addComponent(ActionRow.of(
				Button.secondary("feedback/" + number + "/upvote", Emojis.VOTEUP),
				Button.secondary("feedback/" + number + "/mehvote", Emojis.VOTENONE),
				Button.secondary("feedback/" + number + "/downvote", Emojis.VOTEDOWN),
				Button.link(QuoteHandler.getChannelURL(event.context.gc.guildId, m.getId()), "Discussion")
		)).build()).block();

		event.respond("Your feedback has been submitted!", ActionRow.of(Button.link(QuoteHandler.getMessageURL(event.context.gc.guildId, m.getChannelId(), m.getId()), "Open")));
	}

	private static void changeStatus(ApplicationCommandEventWrapper event, DiscordFeedback.Status status) throws DiscordCommandException {
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