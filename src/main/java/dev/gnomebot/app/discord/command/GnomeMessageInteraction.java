package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.command.admin.DisplayCommands;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.SelectMenu;
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

	public record Action(String id, String name, Callback callback, String description, AuthLevel auth, Predicate<Message> predicate, @Nullable ReactionEmoji emoji) {
		public Action(String id, String name, Callback callback) {
			this(id, name, callback, "", AuthLevel.MEMBER, message -> true, null);
		}

		public Action description(String description) {
			return new Action(id, name, callback, description, auth, predicate, emoji);
		}

		public Action admin() {
			return new Action(id, name, callback, description, AuthLevel.ADMIN, predicate, emoji);
		}

		public Action owner() {
			return new Action(id, name, callback, description, AuthLevel.OWNER, predicate, emoji);
		}

		public Action predicate(Predicate<Message> predicate) {
			return new Action(id, name, callback, description, auth, predicate, emoji);
		}

		public Action emoji(ReactionEmoji emoji) {
			return new Action(id, name, callback, description, auth, predicate, emoji);
		}

		public Action emoji(String emoji) {
			return emoji(ReactionEmoji.unicode(emoji));
		}
	}

	public static final List<Action> ACTIONS = List.of(
			// new Action("report", "Report", ReportCommand::messageInteraction).description("Report this message").emoji("🚨"),
			new Action("webhook-edit", "Edit Message", WebhookCommands::editMessage).admin().emoji("✏️").predicate(message -> !message.getData().webhookId().isAbsent()),
			new Action("debug-components", "Debug Components", DisplayCommands::debugComponents).admin().emoji("\uD83D\uDC1B").predicate(message -> !message.getComponents().isEmpty())
	);

	private static void run(MessageInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		//event.context.checkSenderAdmin();

		var options = new ArrayList<SelectMenu.Option>();
		var authLevel = event.context.gc.getAuthLevel(event.context.sender);

		for (var action : ACTIONS) {
			if (authLevel.is(action.auth()) && action.predicate.test(event.message)) {
				var option = SelectMenu.Option.of(action.name(), action.id());

				if (action.emoji() != null) {
					option = option.withEmoji(action.emoji());
				}

				if (!action.description.isEmpty()) {
					option = option.withDescription(action.description());
				}

				options.add(option);
			}
		}

		if (options.isEmpty()) {
			event.respond(MessageBuilder.create("No actions are available for this message"));
		} else {
			event.respond(MessageBuilder.create("Available actions:").addComponentRow(SelectMenu.of("message-action/" + event.message.getId().asString(), options)));
		}
	}

	public static void callback(ComponentEventWrapper event, List<String> values) throws Exception {
		var message = event.context.channelInfo.getMessage(Snowflake.of(event.path[1]));

		if (message == null) {
			throw new GnomeException("Message not found!");
		}

		var actionId = values.get(0);

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
