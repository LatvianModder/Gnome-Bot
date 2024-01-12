package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.webutils.ansi.Ansi;

import java.util.ArrayList;
import java.util.List;

public class UpdateRegularRolesCommand {
	@LegacyDiscordCommand(name = "update_regular_roles", help = "Adds or removes regular role. Use in case requirements are changed", permissionLevel = AuthLevel.OWNER)
	public static final CommandCallback COMMAND = (context, reader) -> {
		var confirm = reader.readString().orElse("").equalsIgnoreCase("confirm");

		if (!context.gc.regularRole.isSet() || context.gc.regularMessages.get() <= 0) {
			throw new GnomeException("Regular role not set!");
		}

		final var role = SnowFlake.convert(context.gc.regularRole.get());
		final var roleMember = SnowFlake.convert(748075791790637078L);

		context.handler.app.queueBlockingTask(task -> {
			var added = 0;
			var removed = 0;
			var addedMember = 0;
			var removedMember = 0;

			for (var member : context.gc.getGuild().getMembers().filter(m -> !m.isBot()).toIterable()) {
				if (task.cancelled) {
					return;
				}

				var m = context.gc.members.findFirst(member);

				if (m != null) {
					var totalMessages = m.getTotalMessages();

					var n = Ansi.of(member.getTag());

					if (member.getRoleIds().size() > 1) {
						n.cyan();
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
								App.info(Ansi.of("Member + ").append(n).append(": " + totalMessages));

								if (confirm) {
									member.addRole(roleMember).block();
								}
							}
						} else if (member.getRoleIds().contains(roleMember)) {
							removedMember++;
							App.info(Ansi.of("Member - ").append(n).append(": " + totalMessages));

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
