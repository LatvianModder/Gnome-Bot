package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GuildCollections;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MemberCache {
	private final GuildCollections gc;
	private final Map<Long, Optional<Member>> map = new HashMap<>();

	public MemberCache(GuildCollections g) {
		gc = g;
	}

	public int getCacheSize() {
		return map.size();
	}

	public Optional<Member> get(long id) {
		return map.computeIfAbsent(id, snowflake -> Optional.ofNullable(gc.getMember(snowflake)));
	}

	public Optional<Member> get(final User user, final boolean updateImporting) {
		return map.computeIfAbsent(user.getId().asLong(), snowflake -> {
			var member = gc.getMember(snowflake);

			if (updateImporting) {
				MemberHandler.updateMember(gc, user, member, MemberHandler.ACTION_IMPORT_DATA, gc.members.findFirst(snowflake), null);
			}

			return Optional.ofNullable(member);
		});
	}

	public Optional<Member> getAndUpdate(User user) {
		return get(user, true);
	}

	public String getUsername(long id) {
		return get(id).map(User::getUsername).orElse("Deleted User");
	}

	public String getDisplayName(long id) {
		var member = gc.members.findFirst(id);

		if (member != null) {
			return member.displayName();
		}

		return get(id).map(Member::getDisplayName).orElse("Deleted User");
	}
}