package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;

public class VerifyMinecraftCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.context.checkGlobalPerms(Permission.MODERATE_MEMBERS);

		event.acknowledge();
		var user = event.get("user").asUser().get();
		event.respond(message(user));
	}

	public static MessageBuilder message(User user) {
		return MessageBuilder.create(user.getMention() + ", click the `Verify` button below to prove that you own Minecraft!")
				.addComponentRow(Button.success("verify-minecraft/" + user.getId().asString(), "Verify"))
				.allowUserMentions(user.getId().asLong());
	}
}
