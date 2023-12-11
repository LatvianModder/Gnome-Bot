package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.ReactionHandler;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.MessageEditSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CreateLobbyCommand {
	public static class Lobby extends ReactionHandler.Callback {
		public final Snowflake owner;
		public final int max;
		public final HashSet<Snowflake> joined;

		public Lobby(GuildCollections gc, Message m, Snowflake ow, int mx) {
			super(gc, m);
			owner = ow;
			max = mx;
			joined = new HashSet<>();
		}

		@Override
		public boolean onReaction(Member member, ReactionEmoji emoji) throws Exception {
			if (Emojis.VOTEUP.equals(emoji.asCustomEmoji().orElse(null))) {
				if (joined.add(member.getId())) {
					// TODO: Replace with buttons
					message.removeReaction(emoji, member.getId()).block();

					message.edit(MessageEditSpec.builder()
							.contentOrNull("Lobby created - click " + Emojis.VOTEUP.asFormat() + " to join!\n" + joined.size() + "/" + max + " joined")
							.allowedMentionsOrNull(DiscordMessage.noMentions())
							.build()
					).block();

					if (joined.size() >= max) {
						ReactionHandler.removeListener(message.getId());
					}
				}
			} else if (member.getId().equals(owner) && Emojis.STOP.equals(emoji.asUnicodeEmoji().orElse(null))) {
				ReactionHandler.removeListener(message.getId());
			}

			return true;
		}

		@Override
		public void onRemoved(boolean shutdown) {
			List<Snowflake> list = new ArrayList<>(joined);
			Collections.shuffle(list);
			message.edit(MessageEditSpec.builder()
					.contentOrNull(list.size() + "/" + max + " joined:\n" + list.stream().map(i -> "<@" + i.asString() + ">").collect(Collectors.joining("\n")))
					.allowedMentionsOrNull(DiscordMessage.noMentions())
					.build()
			).block();
			message.removeAllReactions().subscribe();
		}
	}

	@LegacyDiscordCommand(name = "create_lobby", help = "Creates a lobby others can join. Used for games", aliases = {"lobby", "create_game"}, arguments = "[max people]")
	public static final CommandCallback COMMAND = (context, reader) -> {
		int max = Math.min(100, Math.max(1, reader.readLong().orElse(8L).intValue()));
		Message m = context.reply("Creating a lobby...");
		m.addReaction(Emojis.VOTEUP).block();
		m.addReaction(Emojis.STOP).block();
		m.edit(MessageEditSpec.builder()
				.contentOrNull("Lobby created - click " + Emojis.VOTEUP.asFormat() + " to join!\n0/" + max + " joined")
				.allowedMentionsOrNull(DiscordMessage.noMentions())
				.build()
		).block();

		ReactionHandler.addListener(new Lobby(context.gc, m, context.sender.getId(), max));
	};
}
