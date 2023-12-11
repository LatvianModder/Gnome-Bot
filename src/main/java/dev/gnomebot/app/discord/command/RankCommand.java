package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.json.JSONObject;
import discord4j.core.object.entity.Member;

public class RankCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("rank")
			.description("Rank")
			.add(time("timespan", true, false))
			.add(user("member"))
			.add(channel("channel"))
			.run(RankCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledge();

		// event.respond("Currently this command is out of order! Sorry for inconvenience!");

		Member m = event.get("member").asMember().orElse(event.context.sender);
		long days = event.get("timespan").asDays().orElse(90L);

		event.context.handler.app.queueBlockingTask(task -> {
			try {
				var leaderboardJson = Utils.internalRequest("api/guild/activity/leaderboard/" + event.context.gc.guildId.asString() + "/" + days).timeout(5000).toJsonArray().block();
				String id = m.getId().asString();

				for (var e : leaderboardJson) {
					var o = (JSONObject) e;

					if (o.asString("id").equals(id)) {
						// event.response().createFollowupMessage("**Rank:**  #0   |   **XP:**  0").subscribe();

						EmbedBuilder embed = EmbedBuilder.create();
						embed.author(m.getDisplayName(), m.getAvatarUrl());

						if (o.asInt("rank") == 69) {
							embed.inlineField("Rank", "#69, nice");
						} else {
							embed.inlineField("Rank", "#" + o.asInt("rank"));
						}

						embed.inlineField("XP", FormattingUtils.format(o.asLong("xp")));

						if (event.context.gc.isMM() && event.context.gc.regularMessages.get() > 0 && !event.context.gc.regularRole.is(m)) {
							long totalMessages = event.context.gc.members.findFirst(m).getTotalMessages();

							if (totalMessages < event.context.gc.regularMessages.get()) {
								if (totalMessages < MessageHandler.MM_MEMBER) {
									embed.inlineField("Member Rank", ((long) (totalMessages * 10000D / (double) MessageHandler.MM_MEMBER) / 100D) + "%");
								} else {
									embed.inlineField("Regular Rank", ((long) (totalMessages * 10000D / (double) event.context.gc.regularMessages.get()) / 100D) + "%");
								}
							}
						}

						event.respond(embed);
						return;
					}
				}

				event.respond(EmbedBuilder.create()
						.author(m.getDisplayName(), m.getAvatarUrl())
						.inlineField("Rank", "Unranked")
						.inlineField("XP", "0")
				);
			} catch (Exception ex) {
				ex.printStackTrace();
				event.respond("Failed to connect to API!");
			}
		});
	}
}
