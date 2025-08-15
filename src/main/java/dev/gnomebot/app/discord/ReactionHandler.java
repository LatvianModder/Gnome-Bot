package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.Utils;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.emoji.Emoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;

import java.util.HashMap;

public class ReactionHandler {
	public static abstract class Callback {
		public GuildCollections gc;
		public Message message;

		public Callback(GuildCollections g, Message m) {
			gc = g;
			message = m;
		}

		public abstract boolean onReaction(Member member, Emoji emoji) throws Exception;

		public void onShutdown() {
		}

		public void onRemoved(boolean shutdown) throws Exception {
		}
	}

	private static final HashMap<Long, Callback> TEMP_REACTION_HANDLERS = new HashMap<>();

	public static void addListener(Callback callback, long ttl) {
		TEMP_REACTION_HANDLERS.put(callback.message.getId().asLong(), callback);

		if (ttl > 0L) {
			var thread = new Thread(() -> {
				try {
					Thread.sleep(ttl * 1000L);
					removeListener(callback.message.getId().asLong());
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}, "ReactionListenerTTL-" + callback.message.getId().asString());
			thread.setDaemon(true);
			thread.start();
		}
	}

	public static void addListener(Callback callback) {
		addListener(callback, 3600L);
	}

	public static void removeListener(long id) throws Exception {
		var callback = TEMP_REACTION_HANDLERS.remove(id);

		if (callback != null) {
			callback.onRemoved(false);
		}
	}

	public static void shutdown() {
		TEMP_REACTION_HANDLERS.values().forEach(Callback::onShutdown);

		TEMP_REACTION_HANDLERS.values().forEach(c -> {
			try {
				c.onRemoved(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		TEMP_REACTION_HANDLERS.clear();
	}

	public static void added(DiscordHandler handler, ReactionAddEvent event) {
		var member = event.getMember().orElse(null);

		if (member == null || member.isBot() || event.getGuildId().isEmpty()) {
			return;
		}

		var guildId = event.getGuildId().get();
		var gc = handler.app.db.guild(guildId);
		var emoji = event.getEmoji();

		gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.REACTION_ADDED)
				.channel(event.getChannelId().asLong())
				.message(event.getMessageId().asLong())
				.user(member)
				.content(Utils.reactionToString(emoji))
		);

		var callback = TEMP_REACTION_HANDLERS.get(event.getMessageId().asLong());

		if (callback != null) {
			event.getMessage().subscribe(m -> {
				callback.gc = gc;
				callback.message = m;

				try {
					if (callback.onReaction(member, emoji)) {
						m.removeReaction(emoji, event.getUserId()).subscribe();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		}
	}

	public static void removed(DiscordHandler handler, ReactionRemoveEvent event) {
		event.getUser().subscribe(u -> {
			if (u.isBot()) {
				return;
			}

			event.getGuildId().ifPresent(guildId -> {
				var gc = handler.app.db.guild(guildId);

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.REACTION_REMOVED)
						.channel(event.getChannelId().asLong())
						.message(event.getMessageId().asLong())
						.user(u)
						.content(Utils.reactionToString(event.getEmoji()))
				);
			});
		});
	}
}
