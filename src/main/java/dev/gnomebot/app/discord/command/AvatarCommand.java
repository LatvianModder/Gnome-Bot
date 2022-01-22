package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.URLRequest;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

/**
 * @author LatvianModder
 */
public class AvatarCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("avatar")
			.description("Sends avatar image in full resolution")
			.add(user("user").required())
			.run(AvatarCommand::run);

	@RootCommand
	public static final CommandBuilder USER_COMMAND = root("Get Avatar")
			.userInteraction()
			.run(AvatarCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		if (event.isUserInteraction()) {
			event.acknowledgeEphemeral();
		} else {
			event.acknowledge();
		}

		try {
			GuildCollections gc = event.context.gc.db.guildOrNull(Snowflake.of(event.get("user").asString("0").trim()));

			if (gc != null) {
				String s = gc.iconUrl.get();

				if (s.isEmpty()) {
					throw new DiscordCommandException("Guild doesn't have an avatar!");
				}

				if (event.isUserInteraction()) {
					event.respond(s + "?size=4096");
					return;
				}

				byte[] data = URLRequest.of(s + "?size=4096").toBytes().block();
				event.respondFile(builder -> builder.content(gc.toString()), gc.guildId.asString() + ".png", data);
				return;
			}
		} catch (Exception ex) {
		}

		User user = event.get("user").asUser().get();
		String s = user.getAvatarUrl();

		if (event.isUserInteraction()) {
			event.respond(s + "?size=4096");
			return;
		}

		byte[] data = URLRequest.of(s + "?size=4096").toBytes().block();
		event.respondFile(builder -> builder.content(user.getMention()), user.getId().asString() + (user.hasAnimatedAvatar() ? ".gif" : ".png"), data);
	}
}
