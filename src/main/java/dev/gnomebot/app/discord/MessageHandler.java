package dev.gnomebot.app.discord;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.Assets;
import dev.gnomebot.app.BrainEventType;
import dev.gnomebot.app.WatchdogThread;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.ScheduledTask;
import dev.gnomebot.app.discord.command.admin.MuteCommand;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.discord.legacycommand.LegacyCommands;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.handler.PasteHandlers;
import dev.gnomebot.app.util.AttachmentType;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.MessageId;
import dev.latvian.apps.ansi.ANSI;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.net.IPUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.StartThreadFromMessageSpec;
import discord4j.rest.RestClient;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class MessageHandler {
	public static final Pattern MESSAGE_URL_PATTERN = Pattern.compile("<?https?://(?:(?:canary|ptb)\\.)?(?:discordapp|discord)\\.(?:com|net)/channels/(\\d+)/(\\d+)/(\\d+)>?", Pattern.MULTILINE);
	public static final Pattern INVITE_PATTERN = Pattern.compile("(?:discord\\.com/invite|discord\\.gg)/(\\w+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern IP_PATTERN = Pattern.compile("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b(.*\\.jar)?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	public static final Pattern REMOVE_FORMATTING_PATTERN = Pattern.compile("(\\*\\*|\\*|__|_|`)(.+?)\\1");
	public static final Pattern URL_PATTERN = Pattern.compile("(?:https?://)?((?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{2,64})\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*");
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

	public static final HashMap<Long, List<MessageId>> AUTO_DELETE = new HashMap<>();

	public static void addAutoDelete(long original, MessageId m) {
		AUTO_DELETE.computeIfAbsent(original, s -> new ArrayList<>()).add(m);
	}

	public static void autoDelete(RestClient client, long original) {
		var ids = AUTO_DELETE.remove(original);

		if (ids != null && !ids.isEmpty()) {
			for (var m : ids) {
				client.getChannelService().deleteMessage(m.channel, m.id, null).subscribe();
			}
		}
	}

	// FIXME: Remove this hardcoded shit and move it to automatic role system
	public static final int MM_MEMBER = 300;

	public static void created(DiscordHandler handler, MessageCreateEvent event) {
		WatchdogThread.update();
		var m = event.getMessage();
		var channel = Objects.requireNonNull(m.getChannel().block());

		if (event.getGuildId().isPresent() && event.getGuildId().get().asLong() == 720671115336220693L) {
			Log.info(ANSI.of("Message " + m.getId().asString() + " in " + channel.getClass().getName() + "/" + channel.getId().asString() + " - " + m.getType() + ", '" + m.getContent() + "' by " + m.getUserData().username()).navyBG());
		}

		var author = m.getData().author();

		if (VALID_MESSAGE_TYPES.contains(m.getData().type())) {
			var member = event.getMember().orElse(null);

			if (channel instanceof PrivateChannel privateChannel && !author.bot().toOptional().orElse(false)) {
				var content = Emojis.stripEmojis(m.getContent());
				DM.log(handler, privateChannel, author, m);

				if (FAST_READ_PATTERN.matcher(content).find()) {
					DM.reply(handler, privateChannel, author, channel, "I don't believe it.");
				} else if (NO_U_PATTERN.matcher(content).find()) {
					DM.reply(handler, privateChannel, author, channel, "no u");
				} else if (HI_PATTERN.matcher(content).find()) {
					DM.reply(handler, privateChannel, author, channel, "Hi");
				} else if (OK_PATTERN.matcher(content).find()) {
					DM.reply(handler, privateChannel, author, channel, "ok");
				} else if (SORRY_PATTERN.matcher(content).find()) {
					DM.reply(handler, privateChannel, author, channel, "It's ok");
				} else if (content.contains("m a bot")) {
					DM.reply(handler, privateChannel, author, channel, "No I don't think so. I'm a bot!");
				} else {
					DM.reply(handler, privateChannel, author, channel, "Why are you talking to me? I'm a bot");
				}
			} else if (member != null && channel instanceof ThreadChannel threadChannel) {
				var gc = handler.app.db.guild(event.getGuildId().get());
				var channelInfo = gc.getChannelMap().get(threadChannel.getParentId().get().asLong());

				if (channelInfo != null) {
					messageCreated(handler, threadChannel, channelInfo.thread(threadChannel.getId().asLong(), threadChannel.getName()), m, member, member, false);
				} else {
					Log.error("Thread parent not found: " + threadChannel.getParentId().get().asLong());
				}
			} else if (member != null && channel instanceof GuildMessageChannel guildChannel) {
				var gc = handler.app.db.guild(event.getGuildId().get());
				var channelInfo = gc.getChannelMap().get(guildChannel.getId().asLong());

				if (channelInfo != null) {
					messageCreated(handler, guildChannel, channelInfo, m, member, member, false);
				} else {
					Log.error("Channel not found: " + guildChannel.getId().asLong());
				}
			}
		} else {
			BrainEventType.UNKNOWN_MESSAGE.build(event.getGuildId().map(Snowflake::asLong).orElse(0L)).content(m.getContent()).post();
		}
	}

	public static void deleted(DiscordHandler handler, MessageDeleteEvent event) {
		if (event.getGuildId().isPresent()) {
			App.instance.queueBlockingTask(cancelled -> {
				var gc = handler.app.db.guild(event.getGuildId().get());

				var message = gc.messages.findFirst(event.getMessageId());

				if (message != null) {
					message.delete(gc, !message.is(DiscordMessage.FLAG_BOT));
					//App.info("Deleted " + gc + "/#" + gc.getChannelName(event.getChannelId()) + "/" + event.getMessageId().asString() + " / by " + Snowflake.of(message.getUserID()).asString());
				}

				autoDelete(event.getClient().getRestClient(), event.getMessageId().asLong());
			});
		}
	}

	public static void bulkDeleted(DiscordHandler handler, MessageBulkDeleteEvent event) {
		var messageIds = event.getMessageIds();

		if (!messageIds.isEmpty()) {
			App.instance.queueBlockingTask(cancelled -> {
				var gc = handler.app.db.guild(event.getGuildId());

				for (var m : messageIds) {
					var message = gc.messages.findFirst(m);

					if (message != null) {
						message.delete(gc, !message.is(DiscordMessage.FLAG_BOT));
						//App.info("Bulk deleted " + gc + "/#" + gc.getChannelName(event.getChannelId()) + "/" + m.asString() + " - " + message.getContent());
					}
				}
			});
		}
	}

	public static void updated(DiscordHandler handler, MessageUpdateEvent event) {
		if (event.isContentChanged() && event.getCurrentContent().isPresent() && event.getGuildId().isPresent()) {
			var gc = handler.app.db.guild(event.getGuildId().get());
			var message = gc.messages.findFirst(event.getMessageId());

			if (message != null) {
				message.edit(gc, event.getCurrentContent().get(), !message.is(DiscordMessage.FLAG_BOT));
			}

			Hardcoded.messageEdited(gc, event);
		}
	}

	@SuppressWarnings("deprecation")
	public static void messageCreated(DiscordHandler handler, MessageChannel messageChannel, ChannelInfo channelInfo, Message message, User user, @Nullable Member member, boolean importing) {
		var dmId = DM.getChannelFromMessage(channelInfo.id);

		if (dmId != null && !message.getContent().isEmpty()) {
			DM.send(handler, handler.getUserData(dmId.userId()), message.getContent(), true);
			return;
		}

		var gc = channelInfo.gc;
		var content = message.getContent();
		var contentNoEmojis = Emojis.stripEmojis(content);

		var updates = new ArrayList<Bson>();
		updates.add(Updates.set("timestamp", message.getTimestamp()));
		updates.add(Updates.set("channel", channelInfo.id));
		updates.add(Updates.set("user", user.getId().asLong()));
		updates.add(Updates.set("content", content));

		var flags = 0L;

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
		var files = new BasicDBObject(0);

		for (var embed : message.getEmbeds()) {
			embed.getImage().ifPresent(img -> images.add(img.getProxyUrl()));
			embed.getVideo().ifPresent(vid -> videos.add(vid.getUrl()));
		}

		for (var a : message.getAttachments()) {
			switch (AttachmentType.get(a)) {
				case IMAGE -> images.add(a.getProxyUrl());
				case VIDEO -> videos.add(a.getProxyUrl());
				default -> files.put(a.getId().asString(), a.getFilename());
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

		for (var userId : message.getUserMentionIds()) {
			userMentions.add(userId.asLong());

			if (userId.asLong() == handler.selfId) {
				flags |= DiscordMessage.FLAG_MENTIONS_BOT;
			}
		}

		for (var roleId : message.getRoleMentionIds()) {
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

		if (contentNoEmojis.indexOf('\n') != -1) {
			flags |= DiscordMessage.FLAG_MULTILINE;
		}

		var referenceMessage = message.getReferencedMessage().orElse(null);

		if (referenceMessage != null) {
			updates.add(Updates.set("reply", referenceMessage.getId().asLong()));
			flags |= DiscordMessage.FLAG_REPLY;
		}

		var ipMatcher = IP_PATTERN.matcher(contentNoEmojis);

		while (ipMatcher.find()) {
			var jarPart = ipMatcher.group(5);

			if (jarPart == null || jarPart.isEmpty()) {
				try {
					var a = Integer.parseInt(ipMatcher.group(1));
					var b = Integer.parseInt(ipMatcher.group(2));
					var c = Integer.parseInt(ipMatcher.group(3));
					var d = Integer.parseInt(ipMatcher.group(4));

					if (IPUtils.isIP(a, b, c, d) && !(a == b && c == d && a == c)) {
						flags |= DiscordMessage.FLAG_IP;
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
			Log.error("Failed to save message " + message.getId().asString() + ": " + ex);
		}

		var discordMessage = gc.messages.findFirst(message);

		if (discordMessage == null) {
			Log.error("Failed to retrieve message " + message.getId().asString());
			return;
		}

		if (!importing) {
			try {
				MemberHandler.updateMember(gc, user, member, MemberHandler.ACTION_MESSAGE, gc.members.findFirst(user), discordMessage);
			} catch (Exception ex) {
				Log.error("Failed to save member from message " + message.getId().asString() + ": " + ex);
			}

			var date = Date.from(message.getTimestamp().truncatedTo(ChronoUnit.DAYS));
			var d = new Date(date.getYear(), date.getMonth(), date.getDate());

			try {
				var xpDoc = gc.messageCount.query()
						.eq("date", d)
						.eq("channel", channelInfo.id)
						.eq("user", user.getId().asLong())
						.firstDocument();

				if (xpDoc != null) {
					gc.messageCount.query(xpDoc.getObjectId("_id")).update(Updates.inc("count", 1L));
				} else {
					xpDoc = new Document();
					xpDoc.put("date", d);
					xpDoc.put("channel", channelInfo.id);
					xpDoc.put("user", user.getId().asLong());
					xpDoc.put("count", 1L);
					gc.messageCount.insert(xpDoc);
				}

				gc.members.query(user.getId().asLong()).update(Updates.inc("total_messages", 1L));
			} catch (Exception ex) {
				Log.error("Failed to save message stats from message " + message.getId().asString() + ": " + ex);
			}

			var xp = channelInfo.getXp();

			if (xp > 0) {
				try {
					var xpDoc = gc.messageXp.query()
							.eq("date", d)
							.eq("channel", channelInfo.id)
							.eq("user", user.getId().asLong())
							.firstDocument();

					if (xpDoc != null) {
						gc.messageXp.query(xpDoc.getObjectId("_id")).update(Updates.inc("xp", xp));
					} else {
						xpDoc = new Document();
						xpDoc.put("date", d);
						xpDoc.put("channel", channelInfo.id);
						xpDoc.put("user", user.getId().asLong());
						xpDoc.put("xp", xp);
						gc.messageXp.insert(xpDoc);
					}

					gc.members.query(user.getId().asLong()).update(Updates.inc("total_xp", xp));
				} catch (Exception ex) {
					Log.error("Failed to save message stats from message " + message.getId().asString() + ": " + ex);
				}
			}
		}

		var wrappedDiscordMember = importing ? null : gc.members.findFirst(user);
		var totalMessages = wrappedDiscordMember == null ? 0L : wrappedDiscordMember.getTotalMessages();
		var totalXp = wrappedDiscordMember == null ? 0L : wrappedDiscordMember.getTotalXp();
		var authLevel = member == null ? AuthLevel.LOGGED_IN : gc.getAuthLevel(member);

		if (user.isBot()) {
			BrainEventType.MESSAGE_CREATED_BOT.build(gc).post();
		} else if (authLevel.is(AuthLevel.ADMIN)) {
			BrainEventType.MESSAGE_CREATED_ADMIN.build(gc).post();
		} else if (member != null && member.getRoleIds().size() > 0) {
			BrainEventType.MESSAGE_CREATED_ANY_ROLE.build(gc).post();
		} else {
			BrainEventType.MESSAGE_CREATED_NO_ROLE.build(gc).post();
		}

		if (importing || member == null) {
			return;
		}

		if (!user.isBot()) {
			gc.pushRecentUser(member.getId().asLong(), member.getDisplayName() + "#" + member.getDiscriminator());

			for (var mention : message.getUserMentionIds()) {
				handler.getUserTag(mention.asLong()).ifPresent(tag -> gc.pushRecentUser(mention.asLong(), tag));
			}
		}

		var context = new CommandContext();
		context.handler = handler;
		context.gc = gc;
		context.channelInfo = channelInfo;
		context.message = message;
		context.sender = member;

		if (gc.regularRole.isSet() && gc.regularMessages.get() > 0 && totalMessages >= gc.regularMessages.get()) {
			gc.regularRole.role().ifPresent(r -> r.add(member, "Reached Regular"));
		}

		if (!member.isBot() && !authLevel.is(AuthLevel.ADMIN)) {
			var scam = ScamHandler.checkScam(contentNoEmojis);

			if (ScamHandler.URL_SHORTENER_PATTERN.matcher(contentNoEmojis).find()) {
				if (gc.autoMuteUrlShortener.get() > 0 && context.gc.mutedRole.isSet()) {
					var seconds = gc.autoMuteUrlShortener.get() * 60L;

					context.referenceMessage = false;

					try {
						MuteCommand.mute(context, member, seconds, "URL Shortener Link", "Muted " + member.getMention());
					} catch (GnomeException e) {
						e.printStackTrace();
					}

					context.referenceMessage = true;
					message.delete().subscribe();
					return;
				} else {
					handler.suspiciousMessageModLog(gc, gc.adminLogChannel, discordMessage, member, "URL Shortener Link", null);
				}

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.URL_SHORTENER)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}

			if (member.getRoleIds().isEmpty() && INVITE_PATTERN.matcher(contentNoEmojis).find()) {
				handler.suspiciousMessageModLog(gc, gc.adminLogChannel, discordMessage, member, "Suspicious Invite", null);

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.DISCORD_INVITE)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}

			if ((flags & DiscordMessage.FLAG_IP) != 0L) {
				handler.suspiciousMessageModLog(gc, gc.logIpAddressesChannel, discordMessage, member, "IP Address", null);

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.IP_ADDRESS)
						.channel(channelInfo.id)
						.message(message)
						.user(member)
						.content(content)
				);
			}
		}

		var handleQuotes = 0;

		try {
			handleQuotes = QuoteHandler.handle(handler.app, gc, message, channelInfo, member);
		} catch (Exception ex) {
			Log.error("Failed to read message: " + ex);
		}

		var adminRoleMentioned = gc.adminRole.isMentioned(message);

		if (handleQuotes == -1) {
			message.delete().subscribe();
			return;
		} else if (handleQuotes == 0 && adminRoleMentioned && gc.adminLogChannel.isSet() && !member.isBot()) {
			var builder = new StringBuilder("[Admin ping:](");
			QuoteHandler.getMessageURL(builder, gc.guildId, channelInfo.id, message.getId().asLong());
			builder.append(")\n\n");
			builder.append(content);

			if (referenceMessage != null) {
				builder.append("\n<@");
				builder.append(referenceMessage.getUserData().id().asString());
				builder.append(">: ");
				builder.append(referenceMessage.getContent());

				gc.adminLogChannelEmbed(referenceMessage.getUserData(), gc.adminLogChannel, spec -> {
					spec.description(builder.toString());
					spec.timestamp(message.getTimestamp());
					spec.author(member.getTag(), member.getAvatarUrl());

					if (!images.isEmpty()) {
						spec.thumbnail(images.getFirst().toString());
					}
				});
			} else {
				gc.adminLogChannelEmbed(null, gc.adminLogChannel, spec -> {
					spec.description(builder.toString());
					spec.timestamp(message.getTimestamp());
					spec.author(member.getTag(), member.getAvatarUrl());

					if (!images.isEmpty()) {
						spec.thumbnail(images.getFirst().toString());
					}
				});
			}
		}

		if (adminRoleMentioned) {
			var c = new StringBuilder(content);

			if (referenceMessage != null) {
				c.append("\nReferencing: ").append(referenceMessage.getContent());
			}

			gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.ADMIN_PING)
					.channel(channelInfo.id)
					.message(message)
					.user(referenceMessage != null ? referenceMessage.getUserData().id().asLong() : 0L)
					.source(member)
					.content(c.toString().trim())
			);
		}

		var macroPrefix = context.gc.macroPrefix.get();

		if (Hardcoded.message(gc, message, channelInfo, member, contentNoEmojis)) {
			return;
		}

		if (channelInfo.settings.autoUpvote && channelInfo.threadParent == null) {
			message.addReaction(Emojis.VOTEUP).subscribe();
		}

		var outputMessageChannel = messageChannel;

		if (channelInfo.settings.autoThread && channelInfo.threadParent == null && member.getId().asLong() != gc.db.app.discordHandler.selfId) {
			try {
				var u = member.getDisplayName();
				var c = contentNoEmojis.replace('\n', ' ');

				var ci = c.indexOf("```");

				if (ci != -1) {
					c = c.substring(0, ci);
				}

				c = EXTRA_SPACE_PATTERN.matcher(REMOVE_FORMATTING_PATTERN.matcher(URL_PATTERN.matcher(c).replaceAll("[$1 link]").replace("[www.", "[").replace('"', '’').replace('\'', '’')).replaceAll("$2")).replaceAll(" ").trim();
				c = STRIP_URL_PATTERN.matcher(c).replaceAll("").trim();

				String n;

				if (c.isEmpty()) {
					n = FormattingUtils.trim("Post by " + u, 100);
				} else {
					var u1 = FormattingUtils.trim(u, 94);
					n = FormattingUtils.trim(c, 95 - u1.length()) + " - " + u1;
				}

				// (u.endsWith("s") ? (u + "’") : (u + "’s")) + " Post Discussion"

				outputMessageChannel = Objects.requireNonNull(message.startThread(StartThreadFromMessageSpec.builder()
						.name(n)
						// .autoArchiveDuration(ThreadChannel.AutoArchiveDuration.DURATION2)
						.build()
				).block());
			} catch (Exception ex) {
				Log.error("Failed to create a thread!");
				Log.warn(ex);
			}
		}

		if (gc.autoPaste.get()) {
			var attachments = new ArrayList<Attachment>();

			for (var attachment : message.getAttachments()) {
				var type = AttachmentType.get(attachment);
				if (type == AttachmentType.TEXT || type == AttachmentType.ZIP) {
					attachments.add(attachment);
				}
			}

			if (!attachments.isEmpty() || PasteHandlers.MCLOGS_PATTERN.matcher(contentNoEmojis).find()) {
				PasteHandlers.pasteMessage(gc.db, outputMessageChannel, message, contentNoEmojis, attachments);
			}
		}

		if (member.isBot()) {
			return;
		}

		if (!contentNoEmojis.isEmpty()) {
			var thankGnome = THANK_GNOME_PATTERN.matcher(contentNoEmojis).find();

			if (handleLegacyCommand(context, content)) {
				//App.info("Gnome command: " + content);
			} else if (handleMacro(context, content, macroPrefix)) {
				//App.info("Custom command: " + content);
			} else if (thankGnome || (flags & DiscordMessage.FLAG_MENTIONS_BOT) != 0L) {
				if (thankGnome) {
					message.addReaction(Emojis.GNOME_HAHA_YES).subscribe();
				} else if (NO_U_PATTERN.matcher(contentNoEmojis).find()) {
					outputMessageChannel.createMessage("no u").subscribe();
				} else if (contentNoEmojis.contains("help")) {
					outputMessageChannel.createMessage("Try `" + gc.legacyPrefix.get() + "help`").subscribe();
				} else if (contentNoEmojis.contains("prefix")) {
					outputMessageChannel.createMessage("Current command prefix is `" + gc.legacyPrefix.get() + "`").subscribe();
				} else if (HI_PATTERN.matcher(contentNoEmojis).find()) {
					outputMessageChannel.createMessage("Hi").subscribe();
				} else if (OK_PATTERN.matcher(contentNoEmojis).find()) {
					outputMessageChannel.createMessage("ok").subscribe();
				} else if (referenceMessage != null && referenceMessage.getAuthor().isPresent() && referenceMessage.getAuthor().get().getId().asLong() == gc.db.app.discordHandler.selfId) {
					outputMessageChannel.createMessage(Assets.REPLY_PING.getPath(handler.app)).subscribe();
				} else {
					outputMessageChannel.createMessage(Emojis.GNOME_PING.asFormat()).subscribe();
				}
			}

			gc.db.app.pingHandler.handle(gc, channelInfo, user, contentNoEmojis, content, discordMessage.getURL(gc));
		}

		var task = gc.db.app.findScheduledGuildTask(gc.guildId, t -> t.type.equals(ScheduledTask.CLOSE_THREAD) && t.channelId == channelInfo.id);

		if (task != null) {
			task.changeEnd(System.currentTimeMillis() + Duration.ofHours(1L).toMillis());
			Log.info("Extended " + channelInfo.getName() + " expiration");
		}

		Hardcoded.afterMessage(gc, message, member, totalMessages, totalXp, contentNoEmojis);
	}

	private static boolean handleLegacyCommand(CommandContext context, String content) {
		var prefix = context.gc.legacyPrefix.get();

		if (content.startsWith(prefix) && content.length() > prefix.length()) {
			var reader = new CommandReader(context.gc, content.substring(prefix.length()));

			try {
				LegacyCommands.run(context, reader, content, false);
				BrainEventType.COMMAND_SUCCESS.build(context.gc).content(content).post();
				return true;
			} catch (GnomeException ex) {
				if (ex.type == GnomeException.Type.NOT_FOUND) {
					return false;
				}

				BrainEventType.COMMAND_FAIL.build(context.gc).content(content).post();
				context.message.addReaction(ex.reaction).subscribe();

				if (ex.deleteMessage) {
					context.message.delete(ex.getMessage()).subscribe();
				}

				var error = EmbedBuilder.create().title("Failed to reply!").redColor();

				if (ex.clientException != null && ex.clientException.getErrorResponse().isPresent()) {
					var err = new HashMap<>(ex.clientException.getErrorResponse().get().getFields());
					error.field("Status", ex.clientException.getStatus().toString(), true);
					error.field("Code", String.valueOf(err.remove("code")), true);
					error.field("Message", String.valueOf(err.remove("message")), true);
					System.out.println(err.get("errors").getClass().getName());
					error.description("```\n" + err.get("errors") + "\n```");
				} else {
					error.description("```\n" + ex.getMessage() + "\n```");
				}

				if (!ex.ephemeral || DM.send(context.handler, context.sender.getUserData(), MessageBuilder.create(error), false).isEmpty()) {
					try {
						context.reply(error);
					} catch (GnomeException e) {
						e.printStackTrace();
					}
				}

				return true;
			} catch (Exception ex) {
				context.message.addReaction(Emojis.NO).subscribe();
				Log.warn("Failed to run command: " + ex + " / " + context);
				ex.printStackTrace();
				return true;
			}
		}

		return false;
	}

	private static boolean handleMacro(CommandContext context, String content, String prefix) {
		if (content.length() > prefix.length() && content.startsWith(prefix)) {
			var reader = new CommandReader(context.gc, content.substring(prefix.length()));

			try {
				var macroName = reader.readString().orElse("").trim();
				var macro = context.gc.getMacro(macroName);

				if (macro != null) {
					macro.addUse();
					macro.createMessageOrTimeout(context.gc, reader, context.sender.getId().asLong()).thenAccept(context::reply);
					return true;
				}
			} catch (GnomeException ex) {
				BrainEventType.COMMAND_FAIL.build(context.gc).post();
				context.message.addReaction(ex.reaction).subscribe();

				if (ex.deleteMessage) {
					context.message.delete(ex.getMessage()).subscribe();
				}

				var error = EmbedBuilder.create().title("Failed to reply!").redColor();

				if (ex.clientException != null && ex.clientException.getErrorResponse().isPresent()) {
					var err = new HashMap<>(ex.clientException.getErrorResponse().get().getFields());
					error.field("Status", ex.clientException.getStatus().toString(), true);
					error.field("Code", String.valueOf(err.remove("code")), true);
					error.field("Message", String.valueOf(err.remove("message")), true);
					System.out.println(err.get("errors").getClass().getName());
					error.description("```\n" + err.get("errors") + "\n```");
				} else {
					error.description("```\n" + ex.getMessage() + "\n```");
				}

				if (!ex.ephemeral || DM.send(context.handler, context.sender.getUserData(), MessageBuilder.create(error), false).isEmpty()) {
					try {
						context.reply(error);
					} catch (GnomeException e) {
						e.printStackTrace();
					}
				}

				return true;
			} catch (Exception ex) {
				context.message.addReaction(Emojis.NO).subscribe();
				Log.warn("Failed to run command: " + ex + " / " + context);
				ex.printStackTrace();
				return true;
			}
		}

		return false;
	}
}