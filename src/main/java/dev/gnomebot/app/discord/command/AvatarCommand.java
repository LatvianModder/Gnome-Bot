package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Image;

/**
 * @author LatvianModder
 */
public class AvatarCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("avatar")
			.description("Sends avatar image in full resolution")
			.add(user("user").required())
			.add(bool("guild"))
			.run(AvatarCommand::run);

	@RootCommand
	public static final CommandBuilder USER_COMMAND = root("Get Avatar")
			.userInteraction()
			.run(AvatarCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		boolean guild;

		if (event.isUserInteraction()) {
			event.acknowledgeEphemeral();
			guild = true;
		} else {
			event.acknowledge();
			guild = event.get("guild").asBoolean(true);
		}

		try {
			GuildCollections gc = event.context.gc.db.guildOrNull(Snowflake.of(event.get("user").asString("0").trim()));

			if (gc != null) {
				String s = gc.iconUrl.get();

				if (s.isEmpty()) {
					throw new GnomeException("Guild doesn't have an avatar!");
				}

				if (event.isUserInteraction()) {
					event.respond(s + "?size=4096");
					return;
				}

				byte[] data = URLRequest.of(s + "?size=4096").toBytes().block();
				event.respond(MessageBuilder.create(gc.toString()).addFile(gc.guildId.asString() + ".png", data));
				return;
			}
		} catch (Exception ex) {
		}

		User user = event.get("user").asUser().get();

		String avatarUrl;
		boolean animated;

		if (guild) {
			Member member = user.asMember(event.context.gc.guildId).block();
			animated = member.hasAnimatedGuildAvatar();
			avatarUrl = member.getGuildAvatarUrl(animated ? Image.Format.GIF : Image.Format.PNG).orElse(user.getAvatarUrl());
		} else {
			avatarUrl = user.getAvatarUrl();
			animated = user.hasAnimatedAvatar();
		}

		if (event.isUserInteraction()) {
			event.respond(avatarUrl + "?size=4096");
			return;
		}

		byte[] data = URLRequest.of(avatarUrl + "?size=4096").toBytes().block();
		event.respond(MessageBuilder.create(user.getMention()).addFile(user.getId().asString() + (animated ? ".gif" : ".png"), data));
	}
}
