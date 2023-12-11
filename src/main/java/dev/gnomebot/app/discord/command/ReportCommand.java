package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Member;

import java.util.ArrayList;
import java.util.List;

public class ReportCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("report")
			.description("Report a user to the admins")
			.add(user("user").required())
			.run(ReportCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		if (!event.context.gc.reportChannel.isSet()) {
			throw new GnomeException("Report channel is not set up!");
		}

		Member member = event.get("user").asMember().orElse(null);

		if (member == null) {
			throw new GnomeException("Can't report non-members!");
		} else if (member.getId().equals(event.context.sender.getId())) {
			throw new GnomeException("You can't report your own messages!");
		} else if (member.isBot()) {
			throw new GnomeException("You can't report bot messages!");
		} else if (event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw new GnomeException("You can't report admin messages!");
		}

		presentModal(event, event.context.channelInfo.id, member);
	}

	public static void presentModal(DeferrableInteractionEventWrapper<?> event, Snowflake channel, Member member) {
		List<SelectMenu.Option> options = new ArrayList<>();

		for (String s : event.context.gc.reportOptions.get().split(" \\| ")) {
			options.add(SelectMenu.Option.of(s, s));
		}

		options.add(SelectMenu.Option.of("Other", "Other"));

		event.respondModal("report/" + channel.asString() + "/" + member.getId().asString(), "Report " + member.getDisplayName(),
				// SelectMenu.of("reason", options).withMinValues(1).withPlaceholder("Select Reason..."),
				TextInput.paragraph("additional_info", "Additional Info", "You can write additional info here").required(false)
		);
	}

	public static void reportCallback(ModalEventWrapper event, Snowflake channel, Snowflake user) {
		if (true) {
			event.respond("Reporting isn't implemented yet! You'll have to ping admins");
			return;
		}

		CachedRole role = event.context.gc.reportMentionRole.getRole();

		if (role == null) {
			event.respond("Thank you for your report!");
		} else {
			event.respond("Thank you for your report! <@&" + role.id.asString() + "> have been notified.");
		}

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
