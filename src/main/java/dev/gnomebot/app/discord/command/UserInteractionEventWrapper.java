package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GuildCollections;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import org.jetbrains.annotations.Nullable;

/**
 * @author LatvianModder
 */
public class UserInteractionEventWrapper extends ApplicationCommandInteractionEventWrapper<UserInteractionEvent> {
	public final User user;

	public UserInteractionEventWrapper(GuildCollections gc, UserInteractionEvent e) {
		super(gc, e);
		user = e.getResolvedUser();
	}

	@Override
	public String toString() {
		return event.getCommandName() + " user=" + user.getUsername();
	}

	@Nullable
	public Member getMember() {
		try {
			return user.asMember(context.gc.guildId).block();
		} catch (Exception ex) {
			return null;
		}
	}
}
