package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class WarnsCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("warns")
			.description("Lists warnings")
			.add(user("user"))
			.run(WarnsCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledgeEphemeral();

		if (event.has("user") && event.get("user").asString().equals("all")) {
			List<String> list = new ArrayList<>();
			MemberCache cache = event.context.gc.createMemberCache();

			for (GnomeAuditLogEntry entry : event.context.gc.auditLog.query().eqOr("type", GnomeAuditLogEntry.Type.WARN.name, GnomeAuditLogEntry.Type.MUTE.name).limit(40).descending("timestamp")) {
				cache.get(Snowflake.of(entry.getUser())).ifPresent(m -> list.add(Utils.formatRelativeDate(entry.getDate().toInstant()) + " " + m.getMention() + ": " + entry.getContent() + " - <@" + entry.getSource() + ">"));
			}

			if (list.isEmpty()) {
				event.respond("None");
			} else {
				event.respond(list);
			}

			return;
		}

		User user = event.get("user").asUser().orElse(event.context.sender);

		if (user.isBot()) {
			throw error("Nice try.");
		} else if (!user.getId().equals(event.context.sender.getId())) {
			event.context.checkSenderPerms(Permission.VIEW_AUDIT_LOG);
		}

		List<String> list = new ArrayList<>();
		list.add(user.getMention() + " warnings:");

		for (GnomeAuditLogEntry entry : event.context.gc.auditLog.query().eqOr("type", GnomeAuditLogEntry.Type.WARN.name, GnomeAuditLogEntry.Type.MUTE.name).eq("user", user.getId().asLong())) {
			list.add(Utils.formatRelativeDate(entry.getDate().toInstant()) + " " + entry.getContent() + " - <@" + entry.getSource() + ">");
		}

		if (list.size() == 1) {
			event.respond("None");
		} else {
			event.respond(list);
		}
	}
}
