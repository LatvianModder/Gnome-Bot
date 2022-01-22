package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import discord4j.core.spec.EmbedCreateSpec;

/**
 * @author LatvianModder
 */
public class ModMailCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("modmail")
			.description("Sends message all admins can see in a private channel. Don't send joke messages")
			.add(string("message").required())
			.run(ModMailCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledgeEphemeral();
		String message = event.get("message").asString();

		event.context.gc.adminMessagesChannel.messageChannel().ifPresent(c -> {
			c.createMessage(EmbedCreateSpec.builder()
					.author("Mod Mail", null, event.context.sender.getAvatarUrl())
					.description(event.context.sender.getMention() + ":\n" + message)
					.build()
			).subscribe();
		});

		event.respond("Message sent!");
	}
}
