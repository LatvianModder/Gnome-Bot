package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.QuoteHandler;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Member;
import org.jetbrains.annotations.Nullable;

public class ReportCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("report")
			.description("Report a user to the admins")
			.add(realUser("user").required())
			.run(ReportCommand::run);

	public static final MessageInteractionBuilder MESSAGE_INTERACTION = messageInteraction("Report Message")
			.run(ReportCommand::messageInteraction);

	public static final UserInteractionBuilder USER_INTERACTION = userInteraction("Report Member")
			.run(ReportCommand::memberInteraction);

	private static void run(ChatInputInteractionEventWrapper event) {
		presentModal(event, event.get("user").asMember().orElse(null), 0L);
	}

	private static void memberInteraction(UserInteractionEventWrapper event) {
		presentModal(event, event.getMember(), 0L);
	}

	private static void messageInteraction(MessageInteractionEventWrapper event) {
		try {
			presentModal(event, event.message.getAuthorAsMember().block(), event.message.getId().asLong());
		} catch (Exception ex) {
			throw new GnomeException("Can't report non-members!");
		}
	}

	private static void presentModal(DeferrableInteractionEventWrapper<?> event, @Nullable Member member, long message) {
		if (member == null) {
			throw new GnomeException("Can't report non-members!");
		} else if (!event.context.gc.reportChannel.isSet()) {
			throw new GnomeException("Report channel is not set up!");
		} else if (member.getId().equals(event.context.sender.getId())) {
			throw new GnomeException("You can't report your own messages!");
		} else if (member.isBot()) {
			throw new GnomeException("You can't report bot messages!");
		} else if (event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw new GnomeException("You can't report admin messages!");
		}

		/*
		var options = new ArrayList<SelectMenu.Option>();

		for (var s : event.context.gc.reportOptions.get().split(" \\| ")) {
			options.add(SelectMenu.Option.of(s, s));
		}

		options.add(SelectMenu.Option.of("Other", "Other"));
		 */

		event.respondModal("report/" + member.getId().asString() + "/" + message, "Report " + member.getDisplayName(),
				// SelectMenu.of("reason", options).withMinValues(1).withPlaceholder("Select Reason..."),
				TextInput.small("reason", "Reason", "spam/hacks/DMs/etc.").required(true),
				TextInput.paragraph("additional_info", "Additional Info", "You can write additional info here").required(false)
		);
	}

	public static void reportCallback(ModalEventWrapper event, long userId, long messageId) {
		var member = event.context.gc.getMember(userId);

		if (member == null) {
			throw new GnomeException("Member not found!");
		}

		var role = event.context.gc.reportMentionRole.getRole();

		if (role == null) {
			event.respond("Thank you for your report!");
		} else {
			event.respond("Thank you for your report! <@&" + role.id + "> have been notified.");
		}

		var additionalInfo = event.get("additional_info").asString();

		event.context.gc.adminLogChannelEmbed(member.getUserData(), event.context.gc.adminLogChannel, spec -> {
			spec.description("## " + event.get("reason").asString());

			if (!additionalInfo.isEmpty()) {
				spec.field("Additional Info", additionalInfo);
			}

			if (messageId != 0L) {
				spec.field("Message", QuoteHandler.getMessageURL(event.context.gc.guildId, event.context.channelInfo.id, messageId));
			}

			spec.author(event.context.sender.getDisplayName() + " Reported:", event.context.sender.getAvatarUrl());
		});

		/*
		event.respond(msg -> {
			if (role == null) {
				msg.content("Select reason for reporting this message:");
			} else {
				msg.content("Select reason for reporting this message: (<@&" + role.id.asString() + "> will be pinged)");
			}

			List<SelectMenu.Option> options = new ArrayList<>();
			options.add(SelectMenu.Option.of("Cancel", "-"));

			for (String s : event.context.gc.reportOptions.get().split(" \\| ")) {
				options.add(SelectMenu.Option.of(s, s));
			}

			options.add(SelectMenu.Option.of("Other", "Other"));
			msg.addComponent(ActionRow.of(SelectMenu.of("report/" + m.getChannelId().asString() + "/" + m.getId().asString(), options).withPlaceholder("Select Reason...")).getData());
		});
		 */
	}
}
