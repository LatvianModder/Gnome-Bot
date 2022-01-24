package dev.gnomebot.app.discord;

import com.mongodb.BasicDBList;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.WatchdogThread;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.data.Paste;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandImpl;
import dev.gnomebot.app.discord.legacycommand.MuteCommand;
import dev.gnomebot.app.script.event.MessageEventJS;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.AttachmentType;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageId;
import dev.gnomebot.app.util.ThreadMessageRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class MessageHandler {
	public static final Pattern MESSAGE_URL_PATTERN = Pattern.compile("<?https?://(?:(?:canary|ptb)\\.)?(?:discordapp|discord)\\.(?:com|net)/channels/(\\d+)/(\\d+)/(\\d+)>?", Pattern.MULTILINE);
	public static final Pattern INVITE_PATTERN = Pattern.compile("(?:discord\\.com/invite|discord\\.gg)/(\\w+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern GNOME_MENTION_PATTERN = Pattern.compile("gnom|712800443566260316", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern IP_PATTERN = Pattern.compile("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b(.*\\.jar)?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern EVERYONE_MENTION_PATTERN = Pattern.compile("(`?)\\\\?@(?:everyone|here)\\1");
	public static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```\\w*\\n.*```", Pattern.MULTILINE | Pattern.DOTALL);
	public static final Pattern REMOVE_FORMATTING_PATTERN = Pattern.compile("(\\*\\*|\\*|__|_|`)(.+?)\\1");
	public static final Pattern URL_PATTERN = Pattern.compile("https?://((?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6})\\b[-a-zA-Z0-9()@:%_+.~#?&//=]*");
	public static final Pattern EXTRA_SPACE_PATTERN = Pattern.compile("\\s{2,}", Pattern.MULTILINE);
	public static final Pattern STRIP_URL_PATTERN = Pattern.compile("\\[(?:youtube.com|youtu.be|imgur.com|streamable.com|cdn.discordapp.com|media.discordapp.net) link]");
	public static final Pattern NO_U_PATTERN = Pattern.compile("\\b(?:no|fuck|suc|gay|bad|worse|shit|die|dumb|stupid|idiot|trash|garbage|loser|looser|garbo|good|nice|neat|best|shut)\\b", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern FAST_READ_PATTERN = Pattern.compile("(?:fast|quick|before|early|earlier).*read|read.*(?:fast|quick|before|early|earlier)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern HI_PATTERN = Pattern.compile("\\b(?:hi|hello|hey)\\b", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern OK_PATTERN = Pattern.compile("\\b(?:ok|alright|fine)\\b", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern SORRY_PATTERN = Pattern.compile("\\bsorry\\b", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern THANK_GNOME_PATTERN = Pattern.compile("th[ae]nk.*gnom", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final HashSet<Integer> VALID_MESSAGE_TYPES = new HashSet<>();

	static {
		VALID_MESSAGE_TYPES.add(Message.Type.DEFAULT.getValue());
		VALID_MESSAGE_TYPES.add(Message.Type.REPLY.getValue());
		VALID_MESSAGE_TYPES.add(Message.Type.APPLICATION_COMMAND.getValue());
		VALID_MESSAGE_TYPES.add(Message.Type.THREAD_STARTER_MESSAGE.getValue());
		VALID_MESSAGE_TYPES.add(Message.Type.CONTEXT_MENU_COMMAND.getValue());
	}

	public static final HashMap<Snowflake, List<MessageId>> AUTO_DELETE = new HashMap<>();

	public static void addAutoDelete(Snowflake original, MessageId m) {
		AUTO_DELETE.computeIfAbsent(original, s -> new ArrayList<>()).add(m);
	}

	public static void autoDelete(RestClient client, Snowflake original) {
		List<MessageId> ids = AUTO_DELETE.remove(original);

		if (ids != null && !ids.isEmpty()) {
			for (MessageId m : ids) {
				client.getChannelService().deleteMessage(m.channel, m.id, null).subscribe();
			}
		}
	}

	public static boolean mentionsEveryone(String s) {
		if (s.length() < 5 || s.indexOf('@') == -1) {
			return false;
		}

		String s1 = CODE_BLOCK_PATTERN.matcher(s).replaceAll("").trim();

		if (s1.length() < 5 || s1.indexOf('@') == -1) {
			return false;
		}

		Matcher m = EVERYONE_MENTION_PATTERN.matcher(s1);

		while (m.find()) {
			String s2 = m.group(0);

			if (s2.equals("@everyone") || s2.equals("@here") || s2.equals("\\@everyone") || s2.equals("\\@here")) {
				return true;
			}
		}

		return false;
	}

	// FIXME: Remove this hardcoded shit and move it to automatic role system
	public static final int MM_MEMBER = 300;

	public static void created(DiscordHandler handler, MessageCreateEvent event) {
		WatchdogThread.update();
		Message m = event.getMessage();

		User author = m.getAuthor().orElse(null);

		if (author != null && VALID_MESSAGE_TYPES.contains(m.getData().type())) {
			Member member = event.getMember().orElse(null);
			MessageChannel channel = m.getChannel().block();

			if (channel instanceof PrivateChannel && !author.isBot()) {
				String content = Emojis.stripEmojis(m.getContent());
				DM.log(handler, author, m);

				if (FAST_READ_PATTERN.matcher(content).find()) {
					DM.reply(handler, author, channel, "I don't believe it.");
				} else if (NO_U_PATTERN.matcher(content).find()) {
					DM.reply(handler, author, channel, "no u");
				} else if (HI_PATTERN.matcher(content).find()) {
					DM.reply(handler, author, channel, "Hi");
				} else if (OK_PATTERN.matcher(content).find()) {
					DM.reply(handler, author, channel, "ok");
				} else if (SORRY_PATTERN.matcher(content).find()) {
					DM.reply(handler, author, channel, "It's ok");
				} else if (content.contains("m a bot")) {
					DM.reply(handler, author, channel, "No I don't think so. I'm a bot!");
				} else {
					DM.reply(handler, author, channel, "Why are you talking to me? I'm a bot");
				}
			} else if (member != null && event.getGuildId().isPresent()) {
				var gc = handler.app.db.guildOrNull(event.getGuildId().orElse(null));
				ChannelInfo channelInfo = gc == null ? null : gc.getChannelMap().get(event.getMessage().getChannelId());

				if (channelInfo == null && gc != null && channel instanceof ThreadChannel) {
					channelInfo = new ChannelInfo(gc, gc.channelInfo, MapWrapper.EMPTY, event.getMessage().getChannelId());

					channelInfo.thread = true;
					channelInfo.name = ((ThreadChannel) channel).getName();
					channelInfo.xp = 0L;
					channelInfo.totalMessages = 0L;
					channelInfo.totalXp = 0L;
					channelInfo.autoThread = false;
					channelInfo.autoUpvote = false;
				}

				if (channelInfo != null) {
					messageCreated(handler, channelInfo, m, member, member, false);
				}
			}
		} else {
			App.LOGGER.unknownMessage();
			//App.info("Message type: " + m.getData().type() + " / " + (author == null ? "-" : author.getUsername()));
		}
	}

	public static void deleted(DiscordHandler handler, MessageDeleteEvent event) {
		if (event.getGuildId().isPresent()) {
			GuildCollections gc = handler.app.db.guild(event.getGuildId().get());

			App.instance.queueBlockingTask(cancelled -> {
				DiscordMessage message = gc.messages.findFirst(event.getMessageId());

				if (message != null) {
					message.delete(gc);
					//App.info("Deleted " + gc + "/#" + gc.getChannelName(event.getChannelId()) + "/" + event.getMessageId().asString() + " / by " + Snowflake.of(message.getUserID()).asString());
					App.LOGGER.messageDeleted();
				}

				autoDelete(event.getClient().getRestClient(), event.getMessageId());
			});
		}
	}

	public static void bulkDeleted(DiscordHandler handler, MessageBulkDeleteEvent event) {
		if (!event.getMessageIds().isEmpty()) {
			GuildCollections gc = handler.app.db.guild(event.getGuildId());

			for (Snowflake m : event.getMessageIds()) {
				App.instance.queueBlockingTask(cancelled -> {
					DiscordMessage message = gc.messages.findFirst(m);

					if (message != null) {
						message.delete(gc);
						//App.info("Bulk deleted " + gc + "/#" + gc.getChannelName(event.getChannelId()) + "/" + m.asString() + " - " + message.getContent());
						App.LOGGER.messageDeleted();
					}
				});
			}
		}
	}

	public static void updated(DiscordHandler handler, MessageUpdateEvent event) {
		if (event.isContentChanged() && event.getCurrentContent().isPresent() && event.getGuildId().isPresent()) {
			GuildCollections gc = handler.app.db.guild(event.getGuildId().get());
			DiscordMessage message = gc.messages.findFirst(event.getMessageId());

			if (message != null) {
				message.edit(gc, event.getCurrentContent().get());

				if (!message.is(DiscordMessage.FLAG_BOT)) {
					//App.info("Edited " + gc + "/#" + gc.getChannelName(event.getChannelId()) + "/" + event.getMessageId().asString());
					App.LOGGER.messageEdited();
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void messageCreated(DiscordHandler handler, ChannelInfo channelInfo, Message message, User user, @Nullable Member member, boolean importing) {
		GuildCollections gc = channelInfo.gc;
		String content = message.getContent();
		String contentNoEmojis = Emojis.stripEmojis(content);

		List<Bson> updates = new ArrayList<>();
		updates.add(Updates.set("timestamp", message.getTimestamp()));
		updates.add(Updates.set("channel", channelInfo.id.asLong()));
		updates.add(Updates.set("user", user.getId().asLong()));
		updates.add(Updates.set("content", content));

		long flags = 0L;

		if (user.isBot()) {
			flags |= DiscordMessage.FLAG_BOT;
		}

		if (!message.getEmbeds().isEmpty()) {
			flags |= DiscordMessage.FLAG_EMBEDS;
		}

		if (!message.getAttachments().isEmpty()) {
			flags |= DiscordMessage.FLAG_ATTACHMENTS;
		}

		var images = new BasicDBList();
		var videos = new BasicDBList();
		var files = new BasicDBList();

		for (Embed embed : message.getEmbeds()) {
			embed.getImage().ifPresent(img -> images.add(img.getProxyUrl()));
			embed.getVideo().ifPresent(vid -> videos.add(vid.getUrl()));
		}

		for (Attachment a : message.getAttachments()) {
			switch (AttachmentType.get(a)) {
				case IMAGE -> images.add(a.getProxyUrl());
				case VIDEO -> videos.add(a.getProxyUrl());
				default -> files.add(a.getUrl());
			}
		}

		if (!images.isEmpty()) {
			updates.add(Updates.set("images", images));
			flags |= DiscordMessage.FLAG_IMAGES;
		}

		if (!videos.isEmpty()) {
			updates.add(Updates.set("videos", videos));
			flags |= DiscordMessage.FLAG_VIDEOS;
		}

		if (!files.isEmpty()) {
			updates.add(Updates.set("files", files));
			flags |= DiscordMessage.FLAG_FILES;
		}

		if (message.mentionsEveryone()) {
			flags |= DiscordMessage.FLAG_MENTIONS_ANYONE;
			flags |= DiscordMessage.FLAG_MENTIONS_EVERYONE;
		}

		var userMentions = new BasicDBList();
		var roleMentions = new BasicDBList();

		for (Snowflake userId : message.getUserMentionIds()) {
			userMentions.add(userId.asLong());

			if (userId.equals(handler.selfId)) {
				flags |= DiscordMessage.FLAG_MENTIONS_BOT;
			}
		}

		for (Snowflake roleId : message.getRoleMentionIds()) {
			roleMentions.add(roleId.asLong());
		}

		if (!userMentions.isEmpty()) {
			updates.add(Updates.set("user_mentions", userMentions));
			flags |= DiscordMessage.FLAG_MENTIONS_ANYONE;
			flags |= DiscordMessage.FLAG_MENTIONS_USERS;
		}

		if (!roleMentions.isEmpty()) {
			updates.add(Updates.set("role_mentions", roleMentions));
			flags |= DiscordMessage.FLAG_MENTIONS_ANYONE;
			flags |= DiscordMessage.FLAG_MENTIONS_ROLES;
		}

		if (message.isTts()) {
			flags |= DiscordMessage.FLAG_TTS;
		}

		if (gc.badWordRegex != null && gc.badWordRegex.matcher(contentNoEmojis).find()) {
			flags |= DiscordMessage.FLAG_BAD_WORD;
		}

		if (contentNoEmojis.indexOf('\n') != -1) {
			flags |= DiscordMessage.FLAG_MULTILINE;
		}

		Message referenceMessage = message.getReferencedMessage().orElse(null);

		if (referenceMessage != null) {
			updates.add(Updates.set("reply", referenceMessage.getId().asLong()));
			flags |= DiscordMessage.FLAG_REPLY;
		}

		Matcher ipMatcher = IP_PATTERN.matcher(contentNoEmojis);

		while (ipMatcher.find()) {
			String jarPart = ipMatcher.group(5);

			if (jarPart == null || jarPart.isEmpty()) {
				try {
					int a = Integer.parseInt(ipMatcher.group(1));
					int b = Integer.parseInt(ipMatcher.group(2));
					int c = Integer.parseInt(ipMatcher.group(3));
					int d = Integer.parseInt(ipMatcher.group(4));

					if (Utils.isIP(a, b, c, d)) {
						flags |= DiscordMessage.FLAG_SUSPICIOUS;
						break;
					}
				} catch (Exception ex) {
				}
			}
		}

		updates.add(Updates.set("flags", flags));

		try {
			gc.messages.query(message.getId().asLong()).upsert(updates);
		} catch (Exception ex) {
			App.error("Failed to save message " + message.getId().asString() + ": " + ex);
		}

		DiscordMessage discordMessage = gc.messages.findFirst(message);

		if (discordMessage == null) {
			App.error("Failed to retrieve message " + message.getId().asString());
			return;
		}

		if (!importing) {
			try {
				MemberHandler.updateMember(gc, user, member, MemberHandler.ACTION_MESSAGE, gc.members.findFirst(user), discordMessage);
			} catch (Exception ex) {
				App.info("Failed to save member from message " + message.getId().asString() + ": " + ex);
			}

			Date date = Date.from(message.getTimestamp().truncatedTo(ChronoUnit.DAYS));
			Date d = new Date(date.getYear(), date.getMonth(), date.getDate());

			try {
				Document xpDoc = gc.messageCount.query()
						.eq("date", d)
						.eq("channel", channelInfo.id.asLong())
						.eq("user", user.getId().asLong())
						.firstDocument();

				if (xpDoc != null) {
					gc.messageCount.query(xpDoc.getObjectId("_id")).update(Updates.inc("count", 1L));
				} else {
					xpDoc = new Document();
					xpDoc.put("date", d);
					xpDoc.put("channel", channelInfo.id.asLong());
					xpDoc.put("user", user.getId().asLong());
					xpDoc.put("count", 1L);
					gc.messageCount.insert(xpDoc);
				}

				gc.members.query(user.getId().asLong()).update(Updates.inc("total_messages", 1L));
			} catch (Exception ex) {
				App.error("Failed to save message stats from message " + message.getId().asString() + ": " + ex);
			}

			if (channelInfo.xp > 0L) {
				try {
					Document xpDoc = gc.messageXp.query()
							.eq("date", d)
							.eq("channel", channelInfo.id.asLong())
							.eq("user", user.getId().asLong())
							.firstDocument();

					if (xpDoc != null) {
						gc.messageXp.query(xpDoc.getObjectId("_id")).update(Updates.inc("xp", channelInfo.xp));
					} else {
						xpDoc = new Document();
						xpDoc.put("date", d);
						xpDoc.put("channel", channelInfo.id.asLong());
						xpDoc.put("user", user.getId().asLong());
						xpDoc.put("xp", channelInfo.xp);
						gc.messageXp.insert(xpDoc);
					}

					gc.members.query(user.getId().asLong()).update(Updates.inc("total_xp", channelInfo.xp));
				} catch (Exception ex) {
					App.error("Failed to save message stats from message " + message.getId().asString() + ": " + ex);
				}
			}
		}

		DiscordMember wrappedDiscordMember = importing ? null : gc.members.findFirst(user);
		long totalMessages = wrappedDiscordMember == null ? 0L : wrappedDiscordMember.getTotalMessages();
		long totalXp = wrappedDiscordMember == null ? 0L : wrappedDiscordMember.getTotalXp();
		AuthLevel authLevel = member == null ? AuthLevel.LOGGED_IN : gc.getAuthLevel(member);

		if (user.isBot()) {
			App.LOGGER.messageCreatedBot();
		} else if (authLevel.is(AuthLevel.ADMIN)) {
			App.LOGGER.messageCreatedAdmin();
		} else if (member != null && member.getRoleIds().size() > 0) {
			App.LOGGER.messageCreatedAnyRole();
		} else {
			App.LOGGER.messageCreatedNoRole();
		}

		if (importing || member == null) {
			return;
		}

		gc.pushRecentUser(member.getId(), member.getTag());

		for (Snowflake mention : message.getUserMentionIds()) {
			handler.getUserTag(mention).ifPresent(tag -> gc.pushRecentUser(mention, tag));
		}

		CommandContext context = new CommandContext();
		context.handler = handler;
		context.gc = gc;
		context.channelInfo = channelInfo;
		context.message = message;
		context.sender = member;

		if (gc.regularMessages.get() > 0 && totalMessages >= gc.regularMessages.get() && !gc.regularRole.is(member)) {
			gc.regularRole.add(member.getId(), "Reached Regular");
		}

		if (!member.isBot() && !authLevel.is(AuthLevel.ADMIN)) {
			if (gc.badWordRegex != null && (flags & DiscordMessage.FLAG_BAD_WORD) != 0L) {
				handler.suspiciousMessageModLog(gc, discordMessage, member, "Bad Word", s -> gc.badWordRegex.matcher(s).replaceAll(" **__ $0 __** "));

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.BAD_WORD)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}

			Matcher steamScamMatcher = ScamHandler.STEAM_PATTERN.matcher(contentNoEmojis);

			while (steamScamMatcher.find()) {
				String domain = steamScamMatcher.group(1);

				if (domain.equals("store.steampowered.com") || domain.equals("steamcommunity.com") || domain.equals("steamcharts.com")) {
					continue;
				}

				if (gc.autoMuteSteamLink.get() > 0 && context.gc.mutedRole.isSet()) {
					long seconds = gc.autoMuteSteamLink.get() * 60L;

					context.referenceMessage = false;

					try {
						MuteCommand.mute(context, member, seconds, "Potential Steam Scam", "Muted " + member.getMention());
					} catch (DiscordCommandException e) {
						e.printStackTrace();
					}

					context.referenceMessage = true;
					message.delete().subscribe();
					return;
				} else {
					handler.suspiciousMessageModLog(gc, discordMessage, member, "Potential Steam Scam", s -> s);
				}

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.SCAM)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}

			Matcher nitroScamMatcher = ScamHandler.NITRO_PATTERN.matcher(contentNoEmojis);

			while (nitroScamMatcher.find()) {
				if (gc.autoMuteNitroLink.get() > 0 && context.gc.mutedRole.isSet()) {
					long seconds = gc.autoMuteNitroLink.get() * 60L;

					context.referenceMessage = false;

					try {
						MuteCommand.mute(context, member, seconds, "Potential Nitro Scam", "Muted " + member.getMention());
					} catch (DiscordCommandException e) {
						e.printStackTrace();
					}

					context.referenceMessage = true;
					message.delete().subscribe();
					return;
				} else {
					handler.suspiciousMessageModLog(gc, discordMessage, member, "Potential Nitro Scam", s -> s);
				}

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.SCAM)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}

			if ((flags & DiscordMessage.FLAG_MENTIONS_EVERYONE) == 0L && mentionsEveryone(contentNoEmojis)) {
				if (gc.autoMuteEveryone.get() > 0 && context.gc.mutedRole.isSet()) {
					long seconds = gc.autoMuteEveryone.get() * 60L;

					context.referenceMessage = false;

					try {
						MuteCommand.mute(context, member, seconds, "Mentioning @everyone or @here", "Muted " + member.getMention());
					} catch (DiscordCommandException e) {
						e.printStackTrace();
					}

					context.referenceMessage = true;
					message.delete().subscribe();
					return;
				} else {
					handler.suspiciousMessageModLog(gc, discordMessage, member, "Mentions @everyone / @here", s -> s);
				}

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.EVERYONE_PING)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}

			if (ScamHandler.URL_SHORTENER_PATTERN.matcher(contentNoEmojis).find()) {
				if (gc.autoMuteUrlShortener.get() > 0 && context.gc.mutedRole.isSet()) {
					long seconds = gc.autoMuteUrlShortener.get() * 60L;

					context.referenceMessage = false;

					try {
						MuteCommand.mute(context, member, seconds, "URL Shortener Link", "Muted " + member.getMention());
					} catch (DiscordCommandException e) {
						e.printStackTrace();
					}

					context.referenceMessage = true;
					message.delete().subscribe();
					return;
				} else {
					handler.suspiciousMessageModLog(gc, discordMessage, member, "URL Shortener Link", s -> s);
				}

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.URL_SHORTENER)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}

			if (member.getRoleIds().isEmpty() && INVITE_PATTERN.matcher(contentNoEmojis).find()) {
				handler.suspiciousMessageModLog(gc, discordMessage, member, "Suspicious Invite", s -> s);

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.DISCORD_INVITE)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}

			if ((flags & DiscordMessage.FLAG_SUSPICIOUS) != 0L) {
				handler.suspiciousMessageModLog(gc, discordMessage, member, "IP Address", s -> s);

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.IP_ADDRESS)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}
		}

		int handleQuotes = 0;

		try {
			handleQuotes = QuoteHandler.handle(gc, message, channelInfo, member);
		} catch (Exception ex) {
			App.info("Failed to read message: " + ex);
		}

		if (handleQuotes == -1) {
			message.delete().subscribe();
			return;
		} else if (handleQuotes == 0 && gc.adminLogChannel.isSet() && !member.isBot() && gc.adminRole.isMentioned(message)) {
			gc.adminLogChannelEmbed(spec -> {
				StringBuilder builder = new StringBuilder("[Admin ping:](");
				QuoteHandler.getMessageURL(builder, gc.guildId, channelInfo.id, message.getId());
				builder.append(")\n\n");
				builder.append(content);

				if (referenceMessage != null && referenceMessage.getAuthor().isPresent()) {
					builder.append('\n');
					builder.append(referenceMessage.getAuthor().get().getMention());
					builder.append(": ");
					builder.append(referenceMessage.getContent());
				}

				spec.description(builder.toString());
				spec.timestamp(message.getTimestamp());
				spec.author(member.getTag(), null, member.getAvatarUrl());

				if (!images.isEmpty()) {
					spec.thumbnail(images.get(0).toString());
				}

			});
		}

		if (gc.adminRole.isMentioned(message)) {
			StringBuilder c = new StringBuilder(content);

			if (message.getReferencedMessage().isPresent()) {
				c.append("\nReferencing: ").append(message.getReferencedMessage().get().getContent());
			}

			gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.ADMIN_PING)
					.channel(channelInfo.id)
					.message(message)
					.user(member)
					.content(c.toString().trim())
			);
		}

		if (channelInfo.autoUpvote) {
			message.addReaction(Emojis.VOTEUP).subscribe();
		}

		if (channelInfo.autoThread && !member.getId().equals(gc.db.app.discordHandler.selfId)) {
			try {
				String u = member.getDisplayName();
				String c = contentNoEmojis.replace('\n', ' ');

				int ci = c.indexOf("```");

				if (ci != -1) {
					c = c.substring(0, ci);
				}

				c = EXTRA_SPACE_PATTERN.matcher(REMOVE_FORMATTING_PATTERN.matcher(URL_PATTERN.matcher(c).replaceAll("[$1 link]").replace("[www.", "[").replace('"', '’').replace('\'', '’')).replaceAll("$2")).replaceAll(" ").trim();

				String n;

				if (STRIP_URL_PATTERN.matcher(c).replaceAll("").trim().isEmpty()) {
					n = Utils.trim("Post by " + u, 100);
				} else {
					String u1 = Utils.trim(u, 94);
					n = Utils.trim(c, 95 - u1.length()) + " - " + u1;
				}

				// (u.endsWith("s") ? (u + "’") : (u + "’s")) + " Post Discussion"

				Utils.THREAD_ROUTE.newRequest(channelInfo.id.asLong(), message.getId().asLong())
						.body(new ThreadMessageRequest(n))
						.exchange(context.handler.client.getCoreResources().getRouter())
						.skipBody()
						.block();
			} catch (Exception ex) {
				App.error("Failed to create a thread!");
				App.warn(ex);
			}
		}

		if (gc.guildScripts != null && gc.guildScripts.onMessage.hasListeners()) {
			if (gc.guildScripts.onMessage.post(new MessageEventJS(gc.getWrappedGuild().channels.get(channelInfo.id.asString()).getMessage(message), totalMessages, totalXp), true)) {
				return;
			}
		}

		if (gc.autoPaste.get()) {
			List<Attachment> attachments = new ArrayList<>();

			for (Attachment attachment : message.getAttachments()) {
				if (AttachmentType.get(attachment) == AttachmentType.TEXT) {
					attachments.add(attachment);
				}
			}

			if (!attachments.isEmpty()) {
				if (channelInfo.autoThread) {
					Paste.pasteMessage(gc.db, RestChannel.create(gc.getClient().getRestClient(), message.getId()), message, attachments);
				} else {
					Paste.pasteMessage(gc.db, channelInfo.getRest(), message, attachments);
				}
			}
		}

		if (member.isBot()) {
			return;
		}

		boolean thankGnome = THANK_GNOME_PATTERN.matcher(contentNoEmojis).find();

		if (handleRealCommand(context, content)) {
			//App.info("Gnome command: " + content);
		} else if (handleMacro(context, content)) {
			//App.info("Custom command: " + content);
		} else if (thankGnome || (flags & DiscordMessage.FLAG_MENTIONS_BOT) != 0L) {
			if (thankGnome) {
				message.addReaction(Emojis.GNOME_HAHA_YES).subscribe();
			} else if (NO_U_PATTERN.matcher(contentNoEmojis).find()) {
				channelInfo.createMessage("no u").subscribe();
			} else if (contentNoEmojis.contains("help")) {
				channelInfo.createMessage("Try `" + gc.prefix + "help`").subscribe();
			} else if (contentNoEmojis.contains("prefix")) {
				channelInfo.createMessage("Current command prefix is `" + gc.prefix + "`").subscribe();
			} else if (HI_PATTERN.matcher(contentNoEmojis).find()) {
				channelInfo.createMessage("Hi").subscribe();
			} else if (OK_PATTERN.matcher(contentNoEmojis).find()) {
				channelInfo.createMessage("ok").subscribe();
			} else if (referenceMessage != null && referenceMessage.getAuthor().isPresent() && referenceMessage.getAuthor().get().getId().equals(gc.db.app.discordHandler.selfId)) {
				// hardcoded
				if (channelInfo.id.asLong() != 802238108242018304L) {
					channelInfo.createMessage(Assets.REPLY_PING.getPath()).subscribe();
				}
			} else {
				channelInfo.createMessage(Emojis.GNOME_PING.asFormat()).subscribe();
			}
		}

		if (GNOME_MENTION_PATTERN.matcher(contentNoEmojis).find()) {
			handler.app.config.gnome_mention_webhook.execute(WebhookExecuteRequest.builder()
					.content(discordMessage.getURLAsArrow(context.gc) + " " + content)
					.avatarUrl(user.getAvatarUrl())
					.username(user.getUsername() + " [" + gc + "]")
					.allowedMentions(DiscordMessage.noMentions().toData())
					.build()
			);
		}

		if (gc.guildScripts != null && gc.guildScripts.onAfterMessage.hasListeners()) {
			gc.guildScripts.onAfterMessage.post(new MessageEventJS(gc.getWrappedGuild().channels.get(channelInfo.id.asString()).getMessage(message), totalMessages, totalXp), false);
		}
	}

	private static boolean handleRealCommand(CommandContext context, String content) {
		String prefix = context.gc.prefix.get();

		if (content.startsWith(prefix) && content.length() > prefix.length()) {
			CommandReader reader = new CommandReader(context.gc, content.substring(prefix.length()));

			try {
				DiscordCommandImpl.run(context, reader, content, false);
				App.LOGGER.commandSuccess();
				return true;
			} catch (DiscordCommandException ex) {
				if (ex.type == DiscordCommandException.Type.NOT_FOUND) {
					return false;
				}

				App.LOGGER.commandFail();
				context.message.addReaction(ex.reaction).subscribe();

				if (ex.deleteMessage) {
					context.message.delete(ex.getMessage()).subscribe();
				}

				if (ex.ephemeral) {
					if (DM.send(context.handler, context.sender, ex.getMessage(), false).isEmpty()) {
						try {
							context.reply(spec -> {
								spec.color(EmbedColors.RED);
								spec.title(ex.getMessage());
							});
						} catch (DiscordCommandException e) {
							e.printStackTrace();
						}
					}
				} else {
					try {
						context.reply(spec -> {
							spec.color(EmbedColors.RED);
							spec.title(ex.getMessage());
						});
					} catch (DiscordCommandException e) {
						e.printStackTrace();
					}
				}

				return true;
			} catch (Exception ex) {
				context.message.addReaction(Emojis.NO_ENTRY).subscribe();
				App.info("Failed to run command: " + ex + " / " + context);
				ex.printStackTrace();
				return true;
			}
		}

		return false;
	}

	private static boolean handleMacro(CommandContext context, String content) {
		String prefix = context.gc.macroPrefix.get();

		if (content.startsWith(prefix) && content.length() > prefix.length()) {
			CommandReader reader = new CommandReader(context.gc, content.substring(prefix.length()));

			try {
				String macroName = reader.readString().orElse("").trim();
				Macro macro = context.gc.macros.query().eq("command_name", macroName.toLowerCase()).first();

				if (macro != null) {
					macro.update(Updates.inc("uses", 1));
					context.replyRaw(macro::createMessage);
					App.LOGGER.commandSuccess();
					return true;
				}
			} catch (DiscordCommandException ex) {
				App.LOGGER.commandFail();
				context.message.addReaction(ex.reaction).subscribe();

				if (ex.deleteMessage) {
					context.message.delete(ex.getMessage()).subscribe();
				}

				if (ex.ephemeral) {
					if (DM.send(context.handler, context.sender, ex.getMessage(), false).isEmpty()) {
						try {
							context.reply(spec -> {
								spec.color(EmbedColors.RED);
								spec.title(ex.getMessage());
							});
						} catch (DiscordCommandException e) {
							e.printStackTrace();
						}
					}
				} else {
					try {
						context.reply(spec -> {
							spec.color(EmbedColors.RED);
							spec.title(ex.getMessage());
						});
					} catch (DiscordCommandException e) {
						e.printStackTrace();
					}
				}

				return true;
			} catch (Exception ex) {
				context.message.addReaction(Emojis.NO_ENTRY).subscribe();
				App.info("Failed to run command: " + ex + " / " + context);
				ex.printStackTrace();
				return true;
			}
		}

		return false;
	}
}