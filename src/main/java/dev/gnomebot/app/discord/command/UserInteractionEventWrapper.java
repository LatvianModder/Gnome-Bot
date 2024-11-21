package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import org.jetbrains.annotations.Nullable;

public class UserInteractionEventWrapper extends ApplicationCommandInteractionEventWrapper<UserInteractionEvent> {
	public final User user;

	public UserInteractionEventWrapper(App app, GuildCollections gc, UserInteractionEvent e) {
		super(app, gc, e);
		user = e.getResolvedUser();
	}

	@Override
	public String toString() {
		return event.getCommandName() + " user=" + user.getUsername();
	}

	@Nullable
	public Member getMember() {
		try {
			return user.asMember(SnowFlake.convert(context.gc.guildId)).block();
		} catch (Exception ex) {
			return null;
		}
	}
}
