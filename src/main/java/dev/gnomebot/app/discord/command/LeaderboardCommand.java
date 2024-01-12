package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import io.javalin.http.HttpStatus;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;

public class LeaderboardCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("leaderboard")
			.description("Leaderboard")
			.add(time("timespan", true, false))
			.add(integer("limit"))
			.add(channel("channel"))
			.add(role("role"))
			.run(LeaderboardCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();

		var limit = Math.max(1L, Math.min(event.get("limit").asLong(20L), 10000L));

		if (limit > 100L) {
			event.context.checkSenderAdmin();
		}

		var days = event.get("timespan").asDays().orElse(90L);
		var channelInfo = event.get("channel").asChannelInfo().orElse(null);
		var role = event.get("role").asRole().orElse(null);

		var url = "api/guild/activity/leaderboard-image/" + event.context.gc.guildId + "/" + days + "?limit=" + limit;

		if (channelInfo != null) {
			url += "&channel=" + channelInfo.id;
		}

		if (role != null) {
			url += "&role=" + role.id;
		}

		var req = Utils.internalRequest(url).timeout(30000).toImage();

		try {
			var imageData = new ByteArrayOutputStream();
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
