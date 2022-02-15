package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Image;

import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class WhoisCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("whois")
			.description("Information about a user")
			.add(user("user").required())
			.run(WhoisCommand::run);

	@RootCommand
	public static final CommandBuilder USER_COMMAND = root("User Info")
			.userInteraction()
			.run(WhoisCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		if (event.isUserInteraction()) {
			event.acknowledgeEphemeral();
		} else {
			event.acknowledge();
		}

		User user = event.get("user").asUser().get();

		EmbedBuilder embed = EmbedBuilder.create()
				.title(user.getTag())
				.inlineField("Created", Utils.formatRelativeDate(user.getId().getTimestamp()))
				.thumbnail(user.getAvatarUrl(Image.Format.PNG).orElse(user.getDefaultAvatarUrl()));

		try {
			Member member = user.asMember(event.context.gc.guildId).block();

			if (member != null) {
				embed.inlineField("Joined", Utils.formatRelativeDate(member.getJoinTime().orElse(null)));

				if (!member.getRoleIds().isEmpty()) {
					embed.field("Roles", member.getRoleIds().stream().map(r -> "<@&" + r.asString() + ">").collect(Collectors.joining(" ")));
				}
			}
		} catch (Exception ex) {
		}

		event.respond(embed);
	}
}
