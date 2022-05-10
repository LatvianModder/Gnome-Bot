package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.Ansi;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class UpdateRegularRolesCommand {
	@LegacyDiscordCommand(name = "update_regular_roles", help = "Adds or removes regular role. Use in case requirements are changed", permissionLevel = AuthLevel.OWNER)
	public static final CommandCallback COMMAND = (context, reader) -> {
		boolean confirm = reader.readString().orElse("").equalsIgnoreCase("confirm");

		if (!context.gc.regularRole.isSet() || context.gc.regularMessages.get() <= 0) {
			throw new GnomeException("Regular role not set!");
		}

		final Snowflake role = context.gc.regularRole.get();
		final Snowflake roleMember = Snowflake.of(748075791790637078L);

		context.handler.app.queueBlockingTask(task -> {
			int added = 0;
			int removed = 0;
			int addedMember = 0;
			int removedMember = 0;

			for (Member member : context.gc.getGuild().getMembers().filter(m -> !m.isBot()).toIterable()) {
				if (task.cancelled) {
					return;
				}

				DiscordMember m = context.gc.members.findFirst(member);

				if (m != null) {
					long totalMessages = m.getTotalMessages();

					String n = member.getTag();

					if (member.getRoleIds().size() > 1) {
						n = Ansi.CYAN + n + Ansi.RESET;
					}

					if (totalMessages >= context.gc.regularMessages.get()) {
						if (!member.getRoleIds().contains(role)) {
							added++;
							App.info("Regular + " + n + ": " + totalMessages);

							if (confirm) {
								member.addRole(role).block();
							}
						}
					} else if (member.getRoleIds().contains(role)) {
						removed++;
						App.info("Regular - " + n + ": " + totalMessages);

						if (confirm) {
							member.removeRole(role).block();
						}
					}

					if (context.gc.isMM()) {
						if (totalMessages >= 150) {
							if (!member.getRoleIds().contains(roleMember)) {
								addedMember++;
								App.info("Member + " + n + ": " + totalMessages);

								if (confirm) {
									member.addRole(roleMember).block();
								}
							}
						} else if (member.getRoleIds().contains(roleMember)) {
							removedMember++;
							App.info("Member - " + n + ": " + totalMessages);

							if (confirm) {
								member.removeRole(roleMember).block();
							}
						}
					}
				}
			}

			List<String> reply = new ArrayList<>();
			reply.add("<@&" + role.asString() + ">: added " + added + ", removed " + removed);

			if (context.gc.isMM()) {
				reply.add("<@&" + roleMember.asString() + ">: added " + addedMember + ", removed " + removedMember);
			}

			if (!confirm) {
				reply.add("");
				reply.add("Type `" + context.gc.legacyPrefix + "update_regular_roles confirm` to change roles");
			}

			context.reply(String.join("\n", reply));
		});
	};
}
