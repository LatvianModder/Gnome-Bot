package dev.gnomebot.app.discord.command;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordMessage;
import discord4j.core.object.entity.Member;

import java.time.ZoneId;
import java.util.Date;

/**
 * @author LatvianModder
 */
public class CheckHourlyActivityCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("check_hourly_activity")
			.description("Check hourly activity of a member")
			.add(user("member"))
			.add(integer("days"))
			.add(zone("timezone"))
			.run(CheckHourlyActivityCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		Member member = event.get("member").asMember().orElse(event.context.sender);

		if (!member.equals(event.context.sender)) {
			event.context.checkSenderAdmin();
		}

		long ms = event.get("days").asDays().orElse(30L) * 1000L * 60L * 60L * 24L;
		ZoneId zoneId = event.get("timezone").asZone();

		long[] activity = new long[24];
		long total = 0L;
		App.info("Starting... " + member.getTag());

		for (DiscordMessage m : event.context.gc.messages.query().eq("user", member.getId().asLong()).filter(ms == 0L ? null : Filters.gt("timestamp", new Date(System.currentTimeMillis() - ms)))) {
			activity[m.getDate().toInstant().atZone(zoneId).getHour()]++;
			total++;
		}

		App.info("Stopped!");

		if (total == 0L) {
			event.respond("No messages found!");
			return;
		}

		StringBuilder sb = new StringBuilder("Activity [" + total + " messages]: (" + zoneId + ")");

		for (int i = 0; i < 24; i++) {
			sb.append('\n');

			if (i < 10) {
				sb.append('0');
			}

			sb.append(i);
			sb.append(":00 - ");
			sb.append(activity[i]);
			sb.append(" [");
			sb.append(activity[i] * 100L / total);
			sb.append("%]");
		}

		event.respond(sb.toString());
	}
}
