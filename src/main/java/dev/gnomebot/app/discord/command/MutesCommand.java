package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ScheduledTask;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author LatvianModder
 */
public class MutesCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("mutes")
			.description("Prints all mutes")
			.add(user("user"))
			.run(MutesCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		var list = new ArrayList<String>();
		var i = new AtomicInteger();
		var set = new ConcurrentHashMap<Snowflake, Snowflake>();

		for (var task : event.context.gc.db.app.scheduledTasks) {
			if (task.type.equals(ScheduledTask.UNMUTE)) {
				var m = event.context.gc.getMember(task.userId);

				if (m != null) {
					list.add(i.addAndGet(1) + ". <@" + task.userId.asString() + "> - " + Utils.formatRelativeDate(Instant.ofEpochMilli(task.end)) + " - " + task.content);
				} else {
					var user = event.context.gc.db.app.discordHandler.getUserData(task.userId);
					list.add(i.addAndGet(1) + ". " + user.username() + "/" + task.userId.asString() + " - " + Utils.formatRelativeDate(Instant.ofEpochMilli(task.end)) + " - " + task.content);
				}

				set.put(task.userId, task.userId);
			}
		}

		var muteRoleId = event.context.gc.mutedRole.get();

		if (muteRoleId.asLong() != 0L) {
			event.context.gc.getMemberStream().filter(m -> m.getRoleIds().contains(muteRoleId)).forEach(m -> {
				if (!set.contains(m.getId())) {
					list.add(i.addAndGet(1) + ". <@" + m.getId().asString() + "> - Doesn't Expire");
				}
			});
		}

		if (list.isEmpty()) {
			event.respond("None!");
		} else {
			event.respond(String.join("\n", list));
		}
	}
}
