package dev.gnomebot.app.discord.command.admin;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.QuoteHandler;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.rest.util.Permission;

import java.util.ArrayList;

public class UserAuditLogCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();

		var user = event.get("user").asUser().orElse(event.context.sender);

		if (user.isBot()) {
			throw error("Nice try.");
		} else {
			event.context.checkSenderPerms(Permission.VIEW_AUDIT_LOG);
		}

		var list = new ArrayList<String>();
		list.add("## Audit Log of " + user.getMention());

		for (var entry : event.context.gc.auditLog.query().eq("user", user.getId().asLong()).filter(Filters.in("type", GnomeAuditLogEntry.Type.USER_AUDIT_LOG_TYPES)).descending("_id").limit(20)) {
			var sb = new StringBuilder();
			sb.append("- ");
			sb.append(Utils.formatRelativeDate(entry.getDate().toInstant()));
			sb.append(' ');

			if (entry.getMessage() == 0L || entry.getChannel() == 0L) {
				sb.append('`');
				sb.append(entry.getType().displayName);
				sb.append('`');
			} else {
				sb.append('[');
				sb.append(entry.getType().displayName);
				sb.append("](<");
				QuoteHandler.getMessageURL(sb, event.context.gc.guildId, entry.getChannel(), entry.getMessage());
				sb.append(">)");
			}

			if (!entry.getContent().isEmpty()) {
				sb.append(' ');
				sb.append(FormattingUtils.trim(entry.getContent(), 100).replace("`", "\\`"));
			}

			if (entry.getSource() != 0L) {
				sb.append(" - <@");
				sb.append(entry.getSource());
				sb.append('>');
			}

			list.add(sb.toString());
		}

		if (list.size() == 1) {
			event.respond("- None");
		} else {
			event.respond(list);
		}
	}
}
