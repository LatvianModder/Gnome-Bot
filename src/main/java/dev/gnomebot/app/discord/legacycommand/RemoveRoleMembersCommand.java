package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.Ansi;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;

/**
 * @author LatvianModder
 */
public class RemoveRoleMembersCommand {
	@LegacyDiscordCommand(name = "remove_role_members", help = "Removes members from role", arguments = "<role>", permissionLevel = AuthLevel.ADMIN)
	public static final CommandCallback COMMAND = (context, reader) -> {
		boolean confirm = reader.readString().orElse("").equalsIgnoreCase("confirm");

		final Snowflake role = Snowflake.of(reader.readLong().orElse(0L));

		context.handler.app.queueBlockingTask(task -> {
			int removed = 0;

			for (Member member : context.gc.getGuild().getMembers().toIterable()) {
				if (task.cancelled) {
					return;
				}

				DiscordMember m = context.gc.members.findFirst(member);

				if (m != null) {
					String n = member.getTag();

					if (member.getRoleIds().size() > 1) {
						n = Ansi.CYAN + n + Ansi.RESET;
					}

					if (member.getRoleIds().contains(role)) {
						removed++;
						App.info("- " + n);

						if (confirm) {
							member.removeRole(role).block();
						}
					}
				}
			}

			App.info("Removed " + removed);

			if (confirm) {
				context.reply("<@&" + role.asString() + ">: removed " + removed);
			} else {
				context.reply("<@&" + role.asString() + ">: removed " + removed + "\n\nType `" + context.gc.prefix + "remove_role_members <role> confirm` to change roles");
			}
		});
	};
}
