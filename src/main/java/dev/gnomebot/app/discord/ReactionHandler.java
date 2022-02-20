package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.BasicDocument;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Permission;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class ReactionHandler {
	public static abstract class Callback {
		public GuildCollections gc;
		public Message message;

		public Callback(GuildCollections g, Message m) {
			gc = g;
			message = m;
		}

		public abstract boolean onReaction(Member member, ReactionEmoji emoji) throws Exception;

		public void onShutdown() {
		}

		public void onRemoved(boolean shutdown) throws Exception {
		}
	}

	private static final HashMap<Snowflake, Callback> TEMP_REACTION_HANDLERS = new HashMap<>();

	public static void addListener(Callback callback, long ttl) {
		TEMP_REACTION_HANDLERS.put(callback.message.getId(), callback);

		if (ttl > 0L) {
			Thread thread = new Thread(() -> {
				try {
					Thread.sleep(ttl * 1000L);
					removeListener(callback.message.getId());
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

	public static void removeListener(Snowflake id) throws Exception {
		Callback callback = TEMP_REACTION_HANDLERS.remove(id);

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
		Member member = event.getMember().orElse(null);

		if (member == null || member.isBot()) {
			return;
		}

		event.getGuildId().ifPresent(guildId -> {
			App.LOGGER.reactionAdded();

			GuildCollections gc = handler.app.db.guild(guildId);
			ReactionEmoji emoji = event.getEmoji();

			gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.REACTION_ADDED)
					.channel(event.getChannelId())
					.message(event.getMessageId())
					.user(member)
					.content(Utils.reactionToString(emoji))
			);

			Callback callback = TEMP_REACTION_HANDLERS.get(event.getMessageId());

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

			// TODO: Some fine hardcoded bullshit right here
			if (event.getChannelId().asLong() == 255122566438453249L) {
				if (Emojis.VOTEUP.equals(emoji)) {
					BasicDocument repostDoc = gc.db.mmShowcase.query().eq("message", event.getMessageId().asLong()).eq("user", 0L).first();

					if (repostDoc == null) {
						BasicDocument voteDoc = gc.db.mmShowcase.query().eq("message", event.getMessageId().asLong()).eq("user", event.getUserId().asLong()).first();

						if (voteDoc == null) {
							Document document = new Document();
							document.put("message", event.getMessageId().asLong());
							document.put("user", event.getUserId().asLong());
							gc.db.mmShowcase.insert(document);

							Message m = event.getMessage().block();
							Set<Snowflake> users = m.getReactors(Emojis.VOTEUP).map(User::getId).toStream().collect(Collectors.toSet());
							users.remove(m.getAuthor().get().getId());
							users.remove(handler.selfId);

							if (users.size() >= 15) {
								Document rdocument = new Document();
								rdocument.put("message", event.getMessageId().asLong());
								rdocument.put("user", 0L);
								gc.db.mmShowcase.insert(rdocument);

								m.addReaction(Emojis.STAR).subscribe();
								Member a = m.getAuthorAsMember().block();

								a.addRole(Snowflake.of(829500528407871489L)).subscribe();
								RestChannel.create(m.getClient().getRestClient(), m.getId()).createMessage(Emojis.STAR.getRaw()).subscribe();

								gc.db.app.queueBlockingTask(task -> {
									WebHook webHook = (WebHook) Objects.requireNonNull(gc.discordJS.customData.get("showcaseFavoritesWebhook"));

									// message.addReaction(Vote.UP.reaction).subscribe();
									// App.info("Showcase upvoted! " + users);

									List<String> content = new ArrayList<>();

									if (!m.getContent().isEmpty()) {
										content.add(m.getContent());
									}

									for (Attachment attachment : m.getAttachments()) {
										int w = attachment.getWidth().orElse(0);
										int h = attachment.getHeight().orElse(0);

										if (w > 0 && h > 0) {
											content.add(attachment.getProxyUrl().replace("https://media.discordapp.net/", "https://cdn.discordapp.com/"));
										} else {
											content.add(attachment.getUrl().replace("https://media.discordapp.net/", "https://cdn.discordapp.com/"));
										}
									}

									for (String s : content) {
										webHook.execute(MessageBuilder.create()
												.webhookName(a.getDisplayName())
												.webhookAvatarUrl(a.getAvatarUrl())
												.content(s)
										);
									}
								});
							}
						}
					}
				} else if (Emojis.VOTENONE.equals(emoji)) {
					event.getMessage().flatMap(m -> m.removeReaction(emoji, event.getUserId())).subscribe();

					if (event.getMember().isPresent() && event.getMember().get().getBasePermissions().block().contains(Permission.ADMINISTRATOR)) {
						App.info("Resetting all!");
						gc.db.mmShowcase.query().eq("message", event.getMessageId().asLong()).many().delete();
					}
				} else if (Emojis.STAR.equals(emoji)) {
					event.getMessage().flatMap(m -> m.removeReaction(emoji, event.getUserId())).subscribe();
				}
			}
		});
	}

	public static void removed(DiscordHandler handler, ReactionRemoveEvent event) {
		event.getUser().subscribe(u -> {
			if (u.isBot()) {
				return;
			}

			App.LOGGER.reactionRemoved();

			event.getGuildId().ifPresent(guildId -> {
				GuildCollections gc = handler.app.db.guild(guildId);

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.REACTION_REMOVED)
						.channel(event.getChannelId())
						.message(event.getMessageId())
						.user(u)
						.content(Utils.reactionToString(event.getEmoji()))
				);
			});
		});
	}
}
