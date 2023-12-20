package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.complex.ComplexMessage;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.command.admin.DisplayCommands;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class GnomeMessageInteraction extends ApplicationCommands {
	public static final MessageInteractionBuilder MESSAGE_INTERACTION = messageInteraction("Gnome Actions")
			.run(GnomeMessageInteraction::run);

	@FunctionalInterface
	public interface Callback {
		void messageInteraction(Message message, ComponentEventWrapper event) throws Exception;
	}

	public record Action(String id, String name, Callback callback, AuthLevel auth, Predicate<Message> predicate, @Nullable ReactionEmoji emoji) {
		public Action(String id, String name, Callback callback) {
			this(id, name, callback, AuthLevel.MEMBER, message -> true, null);
		}

		public Action admin() {
			return new Action(id, name, callback, AuthLevel.ADMIN, predicate, emoji);
		}

		public Action owner() {
			return new Action(id, name, callback, AuthLevel.OWNER, predicate, emoji);
		}

		public Action predicate(Predicate<Message> predicate) {
			return new Action(id, name, callback, auth, predicate, emoji);
		}

		public Action emoji(ReactionEmoji emoji) {
			return new Action(id, name, callback, auth, predicate, emoji);
		}

		public Action emoji(String emoji) {
			return emoji(ReactionEmoji.unicode(emoji));
		}
	}

	public static final List<Action> ACTIONS = List.of(
			new Action("webhook-edit", "Edit Webhook Message", WebhookCommands::editMessage).owner().emoji("✏️").predicate(m -> !m.getData().webhookId().isAbsent() && m.getData().interaction().isAbsent()),
			new Action("debug-complex", "Debug Complex Message", DisplayCommands::debugComplexMessage).emoji("\uD83D\uDC1B").predicate(ComplexMessage::has)
	);

	private static void run(MessageInteractionEventWrapper event) {
		event.acknowledgeEphemeral();

		var options = new ArrayList<ActionComponent>();
		var authLevel = event.context.gc.getAuthLevel(event.context.sender);

		for (var action : ACTIONS) {
			if (authLevel.is(action.auth()) && action.predicate.test(event.message)) {
				options.add(Button.secondary("message-action/" + event.message.getId().asString() + "/" + action.id(), action.emoji(), action.name()));
			}
		}

		if (options.isEmpty()) {
			event.respond(MessageBuilder.create("No actions are available for this message"));
		} else {
			event.respond(MessageBuilder.create("Available actions:").dynamicComponents(options));
		}
	}

	public static void callback(ComponentEventWrapper event, Snowflake messageId, String actionId) throws Exception {
		var message = event.context.channelInfo.getMessage(messageId);

		if (message == null) {
			throw new GnomeException("Message not found!");
		}

		for (var action : ACTIONS) {
			if (action.id.equals(actionId)) {
				if (action.auth == AuthLevel.OWNER) {
					event.context.checkSenderOwner();
				} else if (action.auth == AuthLevel.ADMIN) {
					event.context.checkSenderAdmin();
				}

				App.info(event.context.gc + "/" + event.context.sender.getDisplayName() + " used message action '" + action.name + "' on " + message.getChannelId().asString() + "/" + message.getId().asString());

				if (action.callback != null) {
					action.callback.messageInteraction(message, event);
				} else {
					throw new GnomeException("WIP!");
				}

				return;
			}
		}

		throw new GnomeException("Action not found!");
	}
}
