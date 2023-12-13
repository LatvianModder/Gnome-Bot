package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Image;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class WhoisCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("whois")
			.description("Information about a user")
			.add(user("user").required())
			.run(WhoisCommand::runChatInput);

	private static void runChatInput(ChatInputInteractionEventWrapper event) {
		run(event, event.get("user").asUser().get(), event.get("user").asOptionalMember().orElse(null), true);
	}

	public static void memberInteraction(Member member, ComponentEventWrapper event) {
		run(event, member, member, false);
	}

	private static void run(DeferrableInteractionEventWrapper<?> event, User user, @Nullable Member member, boolean chat) {
		if (chat) {
			event.acknowledge();
		}

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
