package dev.gnomebot.app.discord.command.admin;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.Permission;

import java.util.ArrayList;
import java.util.List;

public class WarnsCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();

		if (event.has("user") && event.get("user").asString().equals("all")) {
			List<String> list = new ArrayList<>();
			MemberCache cache = event.context.gc.createMemberCache();

			for (var entry : event.context.gc.auditLog.query().filter(Filters.bitsAllSet("flags", GnomeAuditLogEntry.Flags.LEVEL_23)).descending("_id").limit(20)) {
				cache.get(Snowflake.of(entry.getUser())).ifPresent(m -> list.add(Utils.formatRelativeDate(entry.getDate().toInstant()) + " " + m.getMention() + ": " + entry.getContent() + " - <@" + entry.getSource() + ">"));
			}

			if (list.isEmpty()) {
				event.respond("None");
			} else {
				event.respond(list);
			}

			return;
		}

		var user = event.get("user").asUser().orElse(event.context.sender);

		if (user.isBot()) {
			throw error("Nice try.");
		} else if (!user.getId().equals(event.context.sender.getId())) {
			event.context.checkGlobalPerms(Permission.VIEW_AUDIT_LOG);
		}

		var list = new ArrayList<String>();
		list.add(user.getMention() + " warnings:");

		for (var entry : event.context.gc.auditLog.query().eq("user", user.getId().asLong()).filter(Filters.bitsAllSet("flags", GnomeAuditLogEntry.Flags.LEVEL_23)).descending("_id").limit(20)) {
			list.add(Utils.formatRelativeDate(entry.getDate().toInstant()) + " " + entry.getContent() + " - <@" + entry.getSource() + ">");
		}

		if (list.size() == 1) {
			event.respond("None");
		} else {
			event.respond(list);
		}
	}
}
