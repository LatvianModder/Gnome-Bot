package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Image;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class WhoisCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("whois")
			.description("Information about a user")
			.add(user("user").required())
			.run(WhoisCommand::runChatInput);

	@RegisterCommand
	public static final UserInteractionBuilder USER_COMMAND = userInteraction("User Info")
			.run(WhoisCommand::runUser);

	private static void runChatInput(ChatInputInteractionEventWrapper event) {
		event.acknowledge();
		run(event, event.get("user").asUser().get(), event.get("user").asOptionalMember().orElse(null));
	}

	private static void runUser(UserInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		run(event, event.user, event.getMember());
	}

	private static void run(ApplicationCommandInteractionEventWrapper<?> event, User user, @Nullable Member member) {
		EmbedBuilder embed = EmbedBuilder.create()
				.title(user.getTag())
				.inlineField("Created", Utils.formatRelativeDate(user.getId().getTimestamp()))
				.thumbnail(user.getAvatarUrl(Image.Format.PNG).orElse(user.getDefaultAvatarUrl()));

		if (member != null) {
			embed.inlineField("Joined", Utils.formatRelativeDate(member.getJoinTime().orElse(null)));

			if (!member.getRoleIds().isEmpty()) {
				embed.field("Roles", member.getRoleIds().stream().map(r -> "<@&" + r.asString() + ">").collect(Collectors.joining(" ")));
			}
		}

		event.respond(embed);
	}
}
