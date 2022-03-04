package dev.gnomebot.app.cli;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.UserCache;
import dev.gnomebot.app.discord.command.RegisterCommand;
import dev.gnomebot.app.util.Table;
import discord4j.common.util.Snowflake;
import discord4j.core.object.Ban;

public class CLIBans {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("bans")
			.description("Export all bans")
			.run(CLIBans::run);

	private static String tableString(String s) {
		return s.isEmpty() ? "-" : s;
	}

	private static void run(CLIEvent event) {
		Table table = new Table("User ID", "Tag", "Reason", "Banned By", "Banned At");
		UserCache userCache = event.gc.db.app.discordHandler.createUserCache();

		for (Ban ban : event.gc.getGuild().getBans().toIterable()) {
			Snowflake userId = Snowflake.of(ban.getData().user().id().asLong());

			boolean found = false;

			for (GnomeAuditLogEntry entry : event.gc.auditLog.query().eq("type", GnomeAuditLogEntry.Type.BAN.name).eq("user", userId.asLong())) {
				found = true;
				table.addRow(userId.asString(), userCache.getTag(userId), tableString(entry.getContent()), userCache.getTag(Snowflake.of(entry.getSource())), entry.getDate().toInstant().toString());
			}

			if (!found) {
				table.addRow(userId.asString(), userCache.getTag(userId), ban.getReason().orElse("-"), "-", "-");
			}
		}

		event.respond("Ban list");
		event.response.addFile("banlist-" + event.gc.guildId.asString() + ".csv", table.getCSVBytes(false));
	}
}
