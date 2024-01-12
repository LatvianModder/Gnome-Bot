package dev.gnomebot.app.discord.command;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.data.Vote;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.QuoteHandler;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.StartThreadSpec;
import discord4j.rest.util.Permission;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FeedbackCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("feedback")
			.description("Open a suggestion form that will be post message in feedback channel for community to vote")
			.run(FeedbackCommand::submit);

	private static void submit(ChatInputInteractionEventWrapper event) {
		var feedbackChannel = event.context.gc.feedbackChannel.messageChannel().orElse(null);

		if (feedbackChannel == null) {
			throw new GnomeException("Feedback channel is not set up on this server!");
		} else if (!event.context.gc.feedbackSuggestRole.is(event.context.sender)) {
			throw new GnomeException("To submit feedback you need " + event.context.gc.feedbackSuggestRole + " role!");
		}

		event.respondModal("feedback", "Submit Feedback", TextInput.paragraph("feedback", "Feedback", 15, 1500).placeholder("Write your feedback here! Please, don't send joke messages."));
	}

	public static void submitCallback(ModalEventWrapper event) {
		//event.respond("Feedback sent!");

		// event.acknowledgeEphemeral();
		var feedbackChannel = event.context.gc.feedbackChannel.messageChannel().orElse(null);

		if (feedbackChannel == null) {
			throw new GnomeException("Feedback channel is not set up on this server!");
		}

		var suggestion = event.get("feedback").asString();

		var number = ++event.context.gc.feedbackNumber;
		event.context.gc.saveInfo();

		event.context.referenceMessage = false;

		event.context.checkBotPerms(feedbackChannel, Permission.ADD_REACTIONS, Permission.SEND_MESSAGES);

		var m = feedbackChannel.createMessage(EmbedBuilder.create()
				.url(App.url("feedback/" + event.context.gc.guildId + "/" + number))
				.title("Loading suggestion #" + number + "...")
		).block();

		var document = new Document();
		document.put("_id", m.getId().asLong());
		document.put("author", event.context.sender.getId().asLong());
		document.put("timestamp", Date.from(m.getTimestamp()));
		document.put("number", number);
		document.put("content", suggestion);
		document.put("status", 0);
		var votes = new BasicDBObject();
		votes.put(event.context.sender.getId().asString(), true);
		document.put("votes", votes);
		event.context.gc.feedback.insert(document);
		m.edit(MessageEditSpec.builder().addEmbed(event.context.gc.feedback.findFirst(m).edit(event.context.gc, event.context.gc.anonymousFeedback.get() ? null : EmbedCreateFields.Footer.of(event.context.sender.getTag(), event.context.sender.getAvatarUrl()))).build()).block();

		try {
			m.startThread(StartThreadSpec.builder()
					.name("Discussion of " + number)
					.autoArchiveDuration(ThreadChannel.AutoArchiveDuration.DURATION3)
					.build()
			).block();

		} catch (Exception ex) {
			App.error("Failed to create a thread for suggestion " + event.context.gc + "/#" + number);
		}

		m.edit(MessageEditSpec.builder().addComponent(ActionRow.of(
				Button.secondary("feedback/" + number + "/upvote", Emojis.VOTEUP),
				Button.secondary("feedback/" + number + "/mehvote", Emojis.VOTENONE),
				Button.secondary("feedback/" + number + "/downvote", Emojis.VOTEDOWN),
				Button.link(QuoteHandler.getChannelURL(event.context.gc.guildId, m.getId().asLong()), "Discussion")
		)).build()).block();

		event.respond(MessageBuilder.create("Your feedback has been submitted!").addComponentRow(Button.link(QuoteHandler.getMessageURL(event.context.gc.guildId, m.getChannelId().asLong(), m.getId().asLong()), "Open")));
	}

	private static void changeStatus(ChatInputInteractionEventWrapper event, DiscordFeedback.Status status) {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		var id = event.get("id").asInt();

		if (id <= 0 || id > event.context.gc.feedbackNumber) {
			throw error("Suggestion not found!");
		}

		var feedback = event.context.gc.feedback.query().eq("number", id).first();

		if (feedback == null) {
			throw error("Suggestion not found!");
		}

		var reason = event.get("reason").asString("Not specified");

		List<Bson> updates = new ArrayList<>();
		updates.add(Updates.set("reason", reason));
		updates.add(Updates.set("reason_author", event.context.sender.getId().asLong()));
		updates.add(Updates.set("status", status.id));
		feedback.update(updates);

		feedback.document.map.put("reason", reason);
		feedback.document.map.put("reason_author", event.context.sender.getId().asLong());
		feedback.document.map.put("status", status.id);

		event.context.gc.findMessage(feedback.getUID(), event.context.gc.feedbackChannel.messageChannel().orElse(null)).ifPresent(message -> {
			var footer = Utils.getFooter(message);
			message.edit(MessageEditSpec.builder().addEmbed(feedback.edit(event.context.gc, footer)).build()).subscribe();

			if (!status.canEdit()) {
				Utils.editComponents(message, null);
			}
		});

		event.respond("Status of #" + id + " changed to " + status.titleSuffix);
	}

	public static void approve(ChatInputInteractionEventWrapper event) {
		changeStatus(event, DiscordFeedback.Status.APPROVED);
	}

	public static void deny(ChatInputInteractionEventWrapper event) {
		changeStatus(event, DiscordFeedback.Status.DENIED);
	}

	public static void consider(ChatInputInteractionEventWrapper event) {
		changeStatus(event, DiscordFeedback.Status.CONSIDERED);
	}

	public static void cleanup(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderAdmin();
		event.acknowledge();

		var feedbackChannel = event.context.gc.feedbackChannel.messageChannel().orElse(null);

		if (feedbackChannel == null) {
			throw error("Feedback channel is not set up on this server!");
		}

		event.context.handler.app.queueBlockingTask(cancelled -> {
			try {
				var i = 0;

				for (var feedback : event.context.gc.feedback.query().neq("deleted", true).neq("status", 0)) {
					feedback.update(Updates.set("deleted", true));
					App.info("Deleting #" + feedback.getNumber() + " - " + feedback.getStatus() + ": " + feedback.getReason());

					try {
						feedbackChannel.getMessage(feedback.getUID()).delete().block();
						i++;
					} catch (Exception ex) {
					}
				}

				event.respond("Done! Deleted " + i + " feedback messages");
			} catch (Exception ex) {
			}
		});
	}

	public static void feedbackButtonCallback(ComponentEventWrapper event, int number, Vote vote) {
		var feedback = event.context.gc.feedback.query().eq("number", number).first();

		if (feedback == null) {
			event.acknowledge();
			return;
		}

		var m = event.context.channelInfo.getMessage(feedback.getUID());

		if (!feedback.getStatus().canEdit()) {
			throw new GnomeException("You can't vote for this suggestion, it's already decided on!");
		}

		if (event.context.gc.feedbackVoteRole.is(event.context.sender)) {
			event.acknowledge();

			if (feedback.setVote(event.context.sender.getId().asString(), vote)) {
				var footer = Utils.getFooter(m);
				m.edit(MessageEditSpec.builder().addEmbed(feedback.edit(event.context.gc, footer)).build()).subscribe();
			}
		} else {
			throw new GnomeException("You can't vote for this suggestion, you have to have " + event.context.gc.regularRole + " role!");
		}
	}
}