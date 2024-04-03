package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.ansi.Log;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.reaction.ReactionEmoji;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class GnomeMemberInteraction extends ApplicationCommands {
	public static final UserInteractionBuilder USER_INTERACTION = userInteraction("Gnome Actions")
			.run(GnomeMemberInteraction::run);

	@FunctionalInterface
	public interface Callback {
		void memberInteraction(Member member, ComponentEventWrapper event) throws Exception;
	}

	public record Action(String id, String name, Callback callback, AuthLevel auth, Predicate<Member> predicate, @Nullable ReactionEmoji emoji) {
		public Action(String id, String name, Callback callback) {
			this(id, name, callback, AuthLevel.MEMBER, member -> true, null);
		}

		public Action description(String description) {
			return new Action(id, name, callback, auth, predicate, emoji);
		}

		public Action admin() {
			return new Action(id, name, callback, AuthLevel.ADMIN, predicate, emoji);
		}

		public Action owner() {
			return new Action(id, name, callback, AuthLevel.OWNER, predicate, emoji);
		}

		public Action predicate(Predicate<Member> predicate) {
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
			new Action("avatar", "Avatar", AvatarCommand::memberInteraction).emoji("\uD83C\uDFA8"),
			new Action("whois", "Member Info", WhoisCommand::memberInteraction).emoji("ℹ️")
	);

	private static void run(UserInteractionEventWrapper event) {
		var member = event.getMember();

		if (member == null) {
			throw new GnomeException("Member not found!");
		}

		var options = new ArrayList<ActionComponent>();
		var authLevel = event.context.gc.getAuthLevel(event.context.sender);

		for (var action : ACTIONS) {
			if (authLevel.is(action.auth()) && action.predicate.test(member)) {
				options.add(Button.secondary("member-action/" + member.getId().asString() + "/" + action.id(), action.emoji(), action.name()));
			}
		}

		if (options.isEmpty()) {
			event.respond(MessageBuilder.create("No actions are available for this message"));
		} else {
			event.respond(MessageBuilder.create("Available actions:").dynamicComponents(options));
		}
	}

	public static void callback(ComponentEventWrapper event, long memberId, String actionId) throws Exception {
		var member = event.context.gc.getMember(memberId);

		if (member == null) {
			throw new GnomeException("Member not found!");
		}

		for (var action : ACTIONS) {
			if (action.id.equals(actionId)) {
				if (action.auth == AuthLevel.OWNER) {
					event.context.checkSenderOwner();
				} else if (action.auth == AuthLevel.ADMIN) {
					event.context.checkSenderAdmin();
				}

				Log.info(event.context.gc + "/" + event.context.sender.getDisplayName() + " used member action '" + action.name + "' on " + member.getDisplayName());

				if (action.callback != null) {
					action.callback.memberInteraction(member, event);
				} else {
					throw new GnomeException("WIP!");
				}

				return;
			}
		}

		throw new GnomeException("Action not found!");
	}
}
