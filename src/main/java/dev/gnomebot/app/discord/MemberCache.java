package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.data.GuildCollections;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;

import java.util.HashMap;
import java.util.Optional;

/**
 * @author LatvianModder
 */
public class MemberCache {
	private final GuildCollections gc;
	private final HashMap<Snowflake, Optional<Member>> map = new HashMap<>();

	public MemberCache(GuildCollections g) {
		gc = g;
	}

	public int getCacheSize() {
		return map.size();
	}

	public Optional<Member> get(final Snowflake id) {
		return map.computeIfAbsent(id, snowflake -> Optional.ofNullable(gc.getMember(snowflake)));
	}

	public Optional<Member> get(final User user, final boolean updateImporting) {
		return map.computeIfAbsent(user.getId(), snowflake -> {
			Member member = gc.getMember(snowflake);

			if (updateImporting) {
				MemberHandler.updateMember(gc, user, member, MemberHandler.ACTION_IMPORT_DATA, gc.members.findFirst(snowflake), null);
			}

			return Optional.ofNullable(member);
		});
	}

	public Optional<Member> getAndUpdate(User user) {
		return get(user, true);
	}

	public String getUsername(final Snowflake id) {
		return get(id).map(User::getUsername).orElse("Deleted User");
	}

	public String getDisplayName(final Snowflake id) {
		DiscordMember member = gc.members.findFirst(id);

		if (member != null) {
			return member.getDisplayName();
		}

		return get(id).map(Member::getDisplayName).orElse("Deleted User");
	}
}