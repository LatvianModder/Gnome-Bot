package dev.gnomebot.app.discord.command;

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

		event.embedResponse(spec -> {
			spec.title(user.getTag());
			spec.addField("Created", Utils.formatRelativeDate(user.getId().getTimestamp()), true);
			spec.thumbnail(user.getAvatarUrl(Image.Format.PNG).orElse(user.getDefaultAvatarUrl()));

			try {
				Member member = user.asMember(event.context.gc.guildId).block();

				if (member != null) {
					spec.addField("Joined", Utils.formatRelativeDate(member.getJoinTime().orElse(null)), true);

					if (!member.getRoleIds().isEmpty()) {
						spec.addField("Roles", member.getRoleIds().stream().map(r -> "<@&" + r.asString() + ">").collect(Collectors.joining(" ")), false);
					}
				}
			} catch (Exception ex) {
			}
		});
	}
}
