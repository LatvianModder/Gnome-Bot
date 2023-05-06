package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.User;
import io.javalin.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * @author LatvianModder
 */
public class MentionLeaderboardCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("mention_leaderboard")
			.description("Mention Leaderboard")
			.add(sub("user")
					.description("User Mention Leaderboard")
					.add(user("mention").required())
					.add(time("timespan", true, false))
					.add(integer("limit"))
					.add(channel("channel"))
					.run(event -> run(event, true))
			)
			.add(sub("role")
					.description("Role Mention Leaderboard")
					.add(role("mention").required())
					.add(time("timespan", true, false))
					.add(integer("limit"))
					.add(channel("channel"))
					.run(event -> run(event, false))
			);


	private static void run(ChatInputInteractionEventWrapper event, boolean isUser) throws Exception {
		event.acknowledge();
		event.context.checkSenderAdmin();

		var mentionId = (isUser ? event.get("mention").asUser().map(User::getId) : event.get("mention").asRole().map(m -> m.id)).orElse(null);

		if (mentionId == null) {
			throw new GnomeException("Mention not found!");
		}

		long limit = Math.max(1L, Math.min(event.get("limit").asLong(20L), 10000L));

		long days = event.get("timespan").asDays().orElse(90L);
		ChannelInfo channelInfo = event.get("channel").asChannelInfo().orElse(null);
		CachedRole role = event.get("role").asRole().orElse(null);

		String url = "api/guild/activity/" + (isUser ? "user" : "role") + "-mention-leaderboard-image/" + event.context.gc.guildId.asString() + "/" + mentionId.asString() + "/" + days + "?limit=" + limit;

		if (channelInfo != null) {
			url += "&channel=" + channelInfo.id.asString();
		}

		if (role != null) {
			url += "&role=" + role.id.asString();
		}

		URLRequest<BufferedImage> req = Utils.internalRequest(url).timeout(30000).toImage();

		try {
			ByteArrayOutputStream imageData = new ByteArrayOutputStream();
			ImageIO.write(req.block(), "PNG", imageData);
			event.respond(MessageBuilder.create().addFile("leaderboard.png", imageData.toByteArray()));
		} catch (URLRequest.UnsuccesfulRequestException ex) {
			if (ex.status == HttpStatus.BAD_REQUEST) {
				event.respond("This leaderboard has no data!");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			event.respond(req.getFullUrl());
		}
	}
}
