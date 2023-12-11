package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.Button;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.ArrayList;

public class GnomeAdminMessageCommand extends ApplicationCommands {
	@RegisterCommand
	public static final MessageInteractionBuilder COMMAND = messageInteraction("Gnome Admin")
			.defaultMemberPermissions(PermissionSet.of(Permission.MODERATE_MEMBERS))
			.run(GnomeAdminMessageCommand::run);

	private static void run(MessageInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		var buttons = new ArrayList<ActionComponent>();

		if (!event.message.getData().webhookId().isAbsent()) {
			buttons.add(Button.success("webhook-edit/" + event.message.getId().asString(), "Edit"));
		}

		if (!event.message.getComponents().isEmpty()) {
			buttons.add(Button.primary("debug-components/" + event.message.getId().asString(), "Debug Components"));
		}

		if (buttons.isEmpty()) {
			event.respond(MessageBuilder.create("No actions are available for this message"));
		} else {
			event.respond(MessageBuilder.create("Available actions:").dynamicComponents(buttons));
		}
	}

	public static void debugComponentsCallback(ComponentEventWrapper event, Snowflake id) {
		var out = DiscordMessage.textFromComponents(event.context.channelInfo.getMessage(id));
		event.edit().respond(out.isEmpty() ? "No Data" : ("```\n" + out + "\n```"));
	}
}
