package dev.gnomebot.app.discord.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Member;

/**
 * @author LatvianModder
 */
public class RankCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("rank")
			.description("Rank")
			.add(time("timespan", true))
			.add(user("member"))
			.add(channel("channel"))
			.run(RankCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledge();

		Member m = event.get("member").asMember().orElse(event.context.sender);
		long days = event.get("timespan").asDays().orElse(90L);

		event.context.handler.app.queueBlockingTask(task -> {
			try {
				JsonArray leaderboardJson = Utils.readInternalJson("api/guild/activity/leaderboard/" + event.context.gc.guildId.asString() + "/" + days).getAsJsonArray();
				String id = m.getId().asString();

				for (JsonElement e : leaderboardJson) {
					JsonObject o = e.getAsJsonObject();

					if (o.get("id").getAsString().equals(id)) {
						// event.response().createFollowupMessage("**Rank:**  #0   |   **XP:**  0").subscribe();

						event.embedResponse(spec -> {
							spec.author(m.getDisplayName(), null, m.getAvatarUrl());

							if (o.get("rank").getAsInt() == 69) {
								spec.addField("Rank", "#69, nice", true);
							} else {
								spec.addField("Rank", "#" + o.get("rank").getAsInt(), true);
							}

							spec.addField("XP", Utils.format(o.get("xp").getAsLong()), true);

							if (event.context.gc.isMM() && event.context.gc.regularMessages.get() > 0 && !event.context.gc.regularRole.is(m)) {
								long totalMessages = event.context.gc.members.findFirst(m).getTotalMessages();

								if (totalMessages < event.context.gc.regularMessages.get()) {
									if (totalMessages < MessageHandler.MM_MEMBER) {
										spec.addField("Member Rank", ((long) (totalMessages * 10000D / (double) MessageHandler.MM_MEMBER) / 100D) + "%", true);
									} else {
										spec.addField("Regular Rank", ((long) (totalMessages * 10000D / (double) event.context.gc.regularMessages.get()) / 100D) + "%", true);
									}
								}
							}
						});

						return;
					}
				}

				event.embedResponse(spec -> {
					spec.author(m.getDisplayName(), null, m.getAvatarUrl());
					spec.addField("Rank", "Unranked", true);
					spec.addField("XP", "0", true);
				});

			} catch (Exception ex) {
				ex.printStackTrace();
				event.respond("Failed to connect to API!");
			}
		});
	}
}
