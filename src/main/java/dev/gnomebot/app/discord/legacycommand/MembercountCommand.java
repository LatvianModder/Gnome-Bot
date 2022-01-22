package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.entity.Member;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class MembercountCommand {
	@LegacyDiscordCommand(name = "membercount", help = "Display member count", arguments = "['quiet_people' | role id]")
	public static final CommandCallback COMMAND = (context, reader) -> {
		String peek = reader.peekString().orElse("");

		if (peek.equals("quiet_people")) {
			int count = 0;
			int total = context.gc.getGuild().getMembers().count().block().intValue();

			for (Member m : context.gc.getMemberStream()
					.filter(m -> {
						DiscordMember dm = context.gc.members.findFirst(m);
						return dm != null && dm.getTotalMessages() <= 0;
					})
					.sorted((o1, o2) -> o1.getUsername().compareToIgnoreCase(o2.getUsername()))
					.collect(Collectors.toList())) {
				App.info("- " + m.getTag());
				count++;
			}

			context.reply(count + " / " + total + " quiet people [" + (int) (count * 100D / (double) total) + "%]");
			return;
		}

		Optional<CachedRole> role = reader.readRole();

		if (role.isPresent()) {
			CachedRole wr = role.get();
			List<Member> members = context.gc.getGuild().getMembers()
					.filter(member -> member.getRoleIds().contains(wr.id))
					.sort((o1, o2) -> o1.getUsername().compareToIgnoreCase(o2.getUsername()))
					.toStream()
					.collect(Collectors.toList());

			App.info("=== Members with role " + wr.name + " ===");

			for (Member m : members) {
				App.info("- " + m.getTag());
			}

			context.reply(Utils.format(members.size()) + " members with role " + wr);
		} else {
			context.reply(Utils.format(context.gc.getGuild().getMembers().count().block()) + " members");
		}
	};
}
