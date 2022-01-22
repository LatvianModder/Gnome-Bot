package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import discord4j.core.object.entity.Member;
import org.jetbrains.annotations.Nullable;

public class MemberConfig extends SnowflakeConfig {
	public MemberConfig(GuildCollections gc, String name) {
		super(gc, name);
	}

	@Override
	public String getType() {
		return "member";
	}

	@Override
	public String toString() {
		return "<@" + get().asString() + '>';
	}

	@Nullable
	public Member getMember() {
		try {
			return isSet() ? gc.getGuild().getMemberById(get()).block() : null;
		} catch (Exception ex) {
			return null;
		}
	}
}
