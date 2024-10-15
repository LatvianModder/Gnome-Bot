package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.BrainEventType;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.cli.CLICommands;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.config.ChannelConfigType;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.interaction.CustomInteractionTypes;
import dev.gnomebot.app.discord.legacycommand.LegacyCommands;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.webutils.data.MutableLong;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.dispatch.DispatchHandler;
import discord4j.core.event.dispatch.DispatchHandlers;
import discord4j.core.event.domain.AuditLogEntryCreateEvent;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.automod.AutoModActionExecutedEvent;
import discord4j.core.event.domain.channel.NewsChannelCreateEvent;
import discord4j.core.event.domain.channel.NewsChannelDeleteEvent;
import discord4j.core.event.domain.channel.NewsChannelUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.TextChannelUpdateEvent;
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.channel.VoiceChannelUpdateEvent;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildUpdateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.guild.UnbanEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ReconnectFailEvent;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.audit.ChangeKey;
import discord4j.core.object.automod.AutoModRuleAction;
import discord4j.core.object.entity.Entity;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.UserData;
import discord4j.discordjson.json.gateway.Dispatch;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.gateway.json.jackson.PayloadDeserializer;
import discord4j.rest.util.AllowedMentions;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class DiscordHandler {
	public final App app;
	public final GatewayDiscordClient client;
	public final long selfId;

	@SuppressWarnings("unchecked")
	public static void addDispatcherType(String event, Class<? extends Dispatch> type) throws Exception {
		var field = PayloadDeserializer.class.getDeclaredField("dispatchTypes");
		field.setAccessible(true);
		((Map<String, Class<? extends Dispatch>>) field.get(null)).put(event, type);
	}

	public static <D, S, E extends Event> void addDispatcherHandler(Class<D> dispatchType, DispatchHandler<D, S, E> dispatchHandler) throws Exception {
		var method = DispatchHandlers.class.getDeclaredMethod("addHandler", Class.class, DispatchHandler.class);
		method.setAccessible(true);
		method.invoke(null, dispatchType, dispatchHandler);
	}

	public DiscordHandler(App a) {
		app = a;

		try {
			// addDispatcherType(EventNames.PRESENCE_UPDATE, null);
			addDispatcherType("GUILD_JOIN_REQUEST_UPDATE", null);
			addDispatcherType("APPLICATION_COMMAND_PERMISSIONS_UPDATE", null);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// PayloadDeserializer.dispatchTypes.put(EventNames.THREAD_MEMBERS_UPDATE, ThreadMembersUpdate.class);
		// DispatchHandlers.addHandler(ThreadMembersUpdate.class, ThreadDispatchHandlers::threadMemberUpdate);

		client = Objects.requireNonNull(DiscordClientBuilder.create(Config.get().discord_bot_token)
				.setDefaultAllowedMentions(AllowedMentions.builder().build())
				.build()
				.gateway()
				.setInitialPresence(shardInfo -> ClientPresence.online(ClientActivity.watching("all of you")))
				.setEnabledIntents(IntentSet.of(
						Intent.GUILDS,
						Intent.GUILD_MEMBERS,
						Intent.GUILD_MODERATION,
						Intent.GUILD_EMOJIS_AND_STICKERS,
						Intent.GUILD_MESSAGES,
						Intent.GUILD_MESSAGE_REACTIONS,
						Intent.DIRECT_MESSAGES,
						Intent.DIRECT_MESSAGE_REACTIONS,
						Intent.GUILD_PRESENCES,
						Intent.GUILD_VOICE_STATES,
						Intent.AUTO_MODERATION_EXECUTION,
						Intent.MESSAGE_CONTENT
				))
				.setMemberRequestFilter(MemberRequestFilter.none())
				.login()
				.timeout(Duration.ofMinutes(5L))
				.block()
		);

		selfId = client.getSelfId().asLong();
	}

	public void load() {
		if (client == null) {
			throw new RuntimeException("Discord client is null!");
		}

		Log.info("Looking for commands");
		LegacyCommands.find();
		ApplicationCommands.findCommands();
		CLICommands.find();
		CustomInteractionTypes.init();

		Log.info("Loading discord handler events...");
		handle(ReadyEvent.class, this::ready);
		handle(GuildCreateEvent.class, this::guildCreated);
		handle(ReconnectFailEvent.class, this::disconnected);
		handle(GuildUpdateEvent.class, this::guildUpdated);
		handle(TextChannelCreateEvent.class, this::channelCreated);
		handle(TextChannelDeleteEvent.class, this::channelDeleted);
		handle(TextChannelUpdateEvent.class, this::channelUpdated);
		handle(NewsChannelCreateEvent.class, this::channelCreated);
		handle(NewsChannelDeleteEvent.class, this::channelDeleted);
		handle(NewsChannelUpdateEvent.class, this::channelUpdated);
		handle(VoiceChannelCreateEvent.class, this::channelCreated);
		handle(VoiceChannelDeleteEvent.class, this::channelDeleted);
		handle(VoiceChannelUpdateEvent.class, this::channelUpdated);
		handle(RoleCreateEvent.class, this::roleCreated);
		handle(RoleDeleteEvent.class, this::roleDeleted);
		handle(RoleUpdateEvent.class, this::roleUpdated);
		handle(MemberJoinEvent.class, this::memberJoined);
		handle(MemberLeaveEvent.class, this::memberLeft);
		handle(MemberUpdateEvent.class, this::memberUpdated);
		// handle(MemberChunkEvent.class, this::memberChunkUpdated);
		handle(PresenceUpdateEvent.class, this::memberPresenceUpdated);
		handle(ReactionAddEvent.class, this::reactionAdded);
		handle(ReactionRemoveEvent.class, this::reactionRemoved);
		handle(MessageCreateEvent.class, this::messageCreated);
		handle(MessageDeleteEvent.class, this::messageDeleted);
		handle(MessageBulkDeleteEvent.class, this::messageBulkDeleted);
		handle(MessageUpdateEvent.class, this::messageUpdated);
		handle(BanEvent.class, this::banned);
		handle(UnbanEvent.class, this::unbanned);
		handle(VoiceStateUpdateEvent.class, this::stateUpdate);
		handle(ChatInputInteractionEvent.class, this::chatInputInteraction);
		handle(UserInteractionEvent.class, this::userInteraction);
		handle(MessageInteractionEvent.class, this::messageInteraction);
		handle(ButtonInteractionEvent.class, this::button);
		handle(SelectMenuInteractionEvent.class, this::selectMenu);
		handle(ModalSubmitInteractionEvent.class, this::modalSubmitInteraction);
		handle(ChatInputAutoCompleteEvent.class, this::chatInputAutoComplete);
		handle(AuditLogEntryCreateEvent.class, this::auditLogEntryCreated);
		handle(AutoModActionExecutedEvent.class, this::autoModActionExecuted);

		Log.info("Connecting to discord servers...");
		client.onDisconnect().subscribe(this::disconnected);
	}

	private <T extends Event> void handle(Class<T> c, Consumer<T> handler) {
		client.on(c).onErrorResume(this::defaultErrorHandler).doOnError(this::defaultErrorHandler).subscribe(handler);
	}

	private <T extends Event> Mono<T> defaultErrorHandler(Throwable exception) {
		Log.error("Failed to handle event: " + exception);
		exception.printStackTrace();
		return Mono.empty();
	}

	private void ready(ReadyEvent event) {
	}

	private void disconnected(Object ignored) {
		Log.error("Disconnected!");
		app.restart();
	}

	private void guildCreated(GuildCreateEvent event) {
		var gc = app.db.guild(event.getGuild().getId());
		gc.guildUpdated(event.getGuild());

		for (var channel : event.getGuild().getChannels().toIterable()) {
			if (channel instanceof TopLevelGuildMessageChannel c) {
				app.db.channelSettings(channel.getId().asLong()).updateFrom(c);
			}
		}

		// App.info("Guild created: " + event.getGuild().getName());
	}

	private void guildUpdated(GuildUpdateEvent event) {
		var gc = app.db.guild(event.getCurrent().getId());
		gc.guildUpdated(event.getCurrent());

		// App.info("Guild updated: " + event.getCurrent().getName());
	}

	private void channelCreated(TextChannelCreateEvent event) {
		app.db.guild(event.getChannel().getGuildId()).channelUpdated(null, event.getChannel(), false);
	}

	private void channelDeleted(TextChannelDeleteEvent event) {
		app.db.guild(event.getChannel().getGuildId()).channelUpdated(null, event.getChannel(), true);
	}

	private void channelUpdated(TextChannelUpdateEvent event) {
		if (event.getCurrent() instanceof TopLevelGuildMessageChannel) {
			app.db.guild(event.getCurrent().getGuildId()).channelUpdated(event.getOld().orElse(null), (TopLevelGuildMessageChannel) event.getCurrent(), false);
		}
	}

	private void channelCreated(NewsChannelCreateEvent event) {
		app.db.guild(event.getChannel().getGuildId()).channelUpdated(null, event.getChannel(), false);
	}

	private void channelDeleted(NewsChannelDeleteEvent event) {
		app.db.guild(event.getChannel().getGuildId()).channelUpdated(null, event.getChannel(), true);
	}

	private void channelUpdated(NewsChannelUpdateEvent event) {
		if (event.getCurrent() instanceof TopLevelGuildMessageChannel) {
			app.db.guild(event.getCurrent().getGuildId()).channelUpdated(event.getOld().orElse(null), (TopLevelGuildMessageChannel) event.getCurrent(), false);
		}
	}

	private void channelCreated(VoiceChannelCreateEvent event) {
		app.db.guild(event.getChannel().getGuildId()).channelUpdated(null, event.getChannel(), false);
	}

	private void channelDeleted(VoiceChannelDeleteEvent event) {
		app.db.guild(event.getChannel().getGuildId()).channelUpdated(null, event.getChannel(), true);
	}

	private void channelUpdated(VoiceChannelUpdateEvent event) {
		app.db.guild(event.getCurrent().getGuildId()).channelUpdated(event.getOld().orElse(null), event.getCurrent(), false);
	}

	private void roleCreated(RoleCreateEvent event) {
		app.db.guild(event.getGuildId()).roleUpdated(event.getRole().getId().asLong(), false);
	}

	private void roleDeleted(RoleDeleteEvent event) {
		app.db.guild(event.getGuildId()).roleUpdated(event.getRoleId().asLong(), true);
	}

	private void roleUpdated(RoleUpdateEvent event) {
		app.db.guild(event.getCurrent().getGuildId()).roleUpdated(event.getCurrent().getId().asLong(), false);
	}

	private void memberJoined(MemberJoinEvent event) {
		var gc = app.db.guild(event.getGuildId());
		gc.memberUpdated(event.getMember().getId().asLong(), 1);
		MemberHandler.joined(this, gc, event);
	}

	private void memberLeft(MemberLeaveEvent event) {
		var gc = app.db.guild(event.getGuildId());
		gc.memberUpdated(event.getUser().getId().asLong(), 2);
		MemberHandler.left(this, gc, event);
	}

	private void memberUpdated(MemberUpdateEvent event) {
		try {
			var member = event.getMember().block();

			if (event.getOld().isPresent() && member != null) {
				var old = event.getOld().get();
				var gc = app.db.guild(event.getGuildId());

				var b = new MutableLong(0L);
				//checkDifference(gc, b, old, member, Member::getDiscriminator, "Discriminator");
				checkDifference(gc, b, old, member, Member::getUsername, "Username");
				//checkDifference(gc, b, old, member, Member::getAvatarUrl, "Avatar");
				//checkDifference(gc, b, old, member, Member::getPublicFlags, "Public Flags");
				checkDifference(gc, b, old, member, Member::getRoleIds, "Roles");
				checkDifference(gc, b, old, member, Member::getNickname, "Nickname");
				checkDifference(gc, b, old, member, Member::getPremiumTime, "Boost");
				checkDifference(gc, b, old, member, Member::isPending, "Pending");

				if (b.value > 0L) {
					gc.memberUpdated(member.getId().asLong(), 0);
					gc.pushRecentUser(member.getId().asLong(), member.getDisplayName() + "#" + member.getDiscriminator());
				}
			}
		} catch (Exception ex) {
			// ex.printStackTrace();
			Log.error("Failed to update member " + event.getMemberId().asString() + ": " + ex);
		}
	}

	private String getEntityName(Entity entity) {
		if (entity instanceof User) {
			return ((User) entity).getTag();
		}

		return entity.getId().asString();
	}

	private <T extends Entity> void checkDifference(GuildCollections gc, MutableLong b, T oldM, T newM, Function<T, ?> value, String name) {
		if (!gc.advancedLogging && b.value > 0L) {
			return;
		}

		var o = value.apply(oldM);
		var n = value.apply(newM);

		if (!Objects.equals(o, n)) {
			b.value++;

			if (gc.advancedLogging) {
				if (b.value == 1L) {
					Log.info(newM.getClass().getSimpleName() + " updated: " + this + "/" + getEntityName(newM));
				}

				Log.info("> " + name + " " + o + " -> " + n);
			}
		}
	}

	private void memberPresenceUpdated(PresenceUpdateEvent event) {
		if (event.getNewAvatar().isPresent() || event.getNewDiscriminator().isPresent() || event.getNewUsername().isPresent()) {
			var gc = app.db.guild(event.getGuildId());
			gc.memberUpdated(event.getUserId().asLong(), 0);

			if (gc.advancedLogging) {
				Log.info("Member updated: " + this + "/" + event.getUserId());

				if (event.getNewUsername().isPresent()) {
					Log.info("> Username " + event.getNewUsername().get());
				}

				if (event.getNewAvatar().isPresent()) {
					Log.info("> Avatar " + event.getNewAvatar().get());
				}

				if (event.getNewDiscriminator().isPresent()) {
					Log.info("> Discriminator " + event.getNewDiscriminator().get());
				}
			}
		}
	}

	private void reactionAdded(ReactionAddEvent event) {
		ReactionHandler.added(this, event);
	}

	private void reactionRemoved(ReactionRemoveEvent event) {
		ReactionHandler.removed(this, event);
	}

	private void messageCreated(MessageCreateEvent event) {
		try {
			MessageHandler.created(this, event);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void messageDeleted(MessageDeleteEvent event) {
		MessageHandler.deleted(this, event);
	}

	private void messageBulkDeleted(MessageBulkDeleteEvent event) {
		MessageHandler.bulkDeleted(this, event);
	}

	private void messageUpdated(MessageUpdateEvent event) {
		MessageHandler.updated(this, event);
	}

	private void banned(BanEvent event) {
		MemberHandler.banned(this, event);
	}

	private void unbanned(UnbanEvent event) {
		MemberHandler.unbanned(this, event);
	}

	private void stateUpdate(VoiceStateUpdateEvent event) {
		VoiceHandler.stateUpdate(this, event);
	}

	private void chatInputInteraction(ChatInputInteractionEvent event) {
		InteractionHandler.chatInputInteraction(this, event);
	}

	private void userInteraction(UserInteractionEvent event) {
		InteractionHandler.userInteraction(this, event);
	}

	private void messageInteraction(MessageInteractionEvent event) {
		InteractionHandler.messageInteraction(this, event);
	}

	private void button(ButtonInteractionEvent event) {
		InteractionHandler.button(this, event);
	}

	private void selectMenu(SelectMenuInteractionEvent event) {
		InteractionHandler.selectMenu(this, event);
	}

	private void modalSubmitInteraction(ModalSubmitInteractionEvent event) {
		InteractionHandler.modalSubmitInteraction(this, event);
	}

	private void chatInputAutoComplete(ChatInputAutoCompleteEvent event) {
		InteractionHandler.chatInputAutoComplete(this, event);
	}

	private void auditLogEntryCreated(AuditLogEntryCreateEvent event) {
		try {
			auditLogEntryCreated0(event);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void auditLogEntryCreated0(AuditLogEntryCreateEvent event) {
		var gc = app.db.guild(event.getGuildId());
		// App.warn(event.toString());
		BrainEventType.AUDIT_LOG.build(gc.guildId).post();
		var e = event.getAuditLogEntry();

		var target = e.getTargetId().isEmpty() ? null : app.discordHandler.getUser(e.getTargetId().get().asLong());

		if (target == null) {
			return;
		}

		var targetName = target.getGlobalName().orElse(target.getUsername());
		var responsible = e.getResponsibleUserId().isEmpty() ? null : gc.getMember(e.getResponsibleUserId().get().asLong());
		var responsibleName = responsible == null ? "AutoMod" : responsible.getDisplayName();

		var reason = e.getReason().orElse("No reason");

		switch (e.getActionType()) {
			case MEMBER_UPDATE -> {
				if (!e.getTargetId().equals(e.getResponsibleUserId())) {
					// App.warn("Audit Log: " + e.getResponsibleUserId().orElse(Utils.NO_SNOWFLAKE).asString() + " > " + e.getTargetId().orElse(Utils.NO_SNOWFLAKE).asString() + ": " + e.getReason().orElse("No reason"));

					var timeout = e.getChange(ChangeKey.COMMUNICATION_DISABLED_UNTIL).orElse(null);

					if (timeout != null) {
						if (timeout.getCurrentValue().isEmpty()) {
							gc.adminLogChannelEmbed(target.getUserData(), gc.adminLogChannel, spec -> {
								spec.description((responsible == null ? "AutoMod" : responsible.getMention()) + " removed timeout of " + target.getMention());
								spec.author(targetName + " timeout", target.getAvatarUrl());
								spec.color(EmbedColor.GREEN);

								if (responsible != null) {
									spec.footer(responsibleName, responsible.getEffectiveAvatarUrl());
								}
							});
						} else {
							gc.adminLogChannelEmbed(target.getUserData(), gc.adminLogChannel, spec -> {
								spec.description((responsible == null ? "AutoMod" : responsible.getMention()) + " timed out " + target.getMention());
								spec.author(targetName + " timeout", target.getAvatarUrl());
								spec.inlineField("Expires", Utils.formatRelativeDate(timeout.getCurrentValue().get()));
								spec.inlineField("Reason", reason);

								if (responsible != null) {
									spec.footer(responsibleName, responsible.getEffectiveAvatarUrl());
								}
							});
						}
					}
				}
			}
			case MEMBER_BAN_ADD -> Log.warn(targetName + " banned for '" + reason + "' by " + responsibleName);
			case MEMBER_BAN_REMOVE -> Log.warn(targetName + " unbanned by " + responsibleName);
			case AUTO_MODERATION_USER_COMMUNICATION_DISABLED -> Log.warn(responsibleName + " timed out " + targetName);
		}
	}

	private void autoModActionExecuted(AutoModActionExecutedEvent event) {
		if (event.getAction().getType() != AutoModRuleAction.Type.SEND_ALERT_MESSAGE) {
			return;
		}

		var pattern = Pattern.compile("\\[gnome:(kick|warn|mute|ban)( [^]]+)?]");
		var rule = event.getAutoModRule().block();

		Log.error("AutoMod: " + rule.getName() + " " + event.getContent());
		Log.info(event.getAction().getData());

		var matcher = pattern.matcher(rule.getName());

		Consumer<MessageBuilder> messageChannel = null;

		while (matcher.find()) {
			var reason = Optional.ofNullable(matcher.group(2)).map(String::trim).orElse("AutoMod");
			var guild = event.getGuild().block();
			var gc = app.db.guild(guild);
			var user = event.getUser().block();

			if (messageChannel == null) {
				long msgChannel = event.getAction().getData().metadata().toOptional().flatMap(d -> d.channelId().toOptional()).map(Id::asLong).orElse(0L);

				if (msgChannel != 0L) {
					messageChannel = m -> guild.getChannelById(SnowFlake.convert(msgChannel)).ofType(MessageChannel.class).flatMap(c -> c.createMessage(m.toMessageCreateSpec())).block();
				} else if (gc.adminLogChannel.isSet()) {
					messageChannel = m -> gc.adminLogChannel.getMessageChannel().createMessage(m).block();
				} else {
					messageChannel = m -> {
					};
				}
			}

			switch (matcher.group(1).trim()) {
				case "kick" -> {
					var dm = DM.send(this, user.getUserData(), "You've been kicked from " + gc.getClickableName() + ": **" + reason + "**.\nIf you wish to appeal it, [click here](<" + App.url("/guild/" + guild.getId().asString() + "/appeal") + ">).", true).isPresent();
					guild.kick(user.getId(), reason).block();
					var msg = "<@" + selfId + "> kicked " + user.getMention() + ": " + reason + " [DM " + Emojis.yesNo(dm).asFormat() + "]";

					messageChannel.accept(MessageBuilder.create(msg));

					gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.KICK)
							.user(user)
							.source(selfId)
							.content("AutoMod: " + reason)
							.flags(GnomeAuditLogEntry.Flags.DM, dm)
					);
				}
				case "warn" -> {
					messageChannel.accept(MessageBuilder.create("Gnome AutoMod Warn not implemented yet :("));
					// Warn
				}
				case "mute" -> {
					messageChannel.accept(MessageBuilder.create("Gnome AutoMod Mute not implemented yet :("));
					// Mute
				}
				case "ban" -> {
					messageChannel.accept(MessageBuilder.create("Gnome AutoMod Ban not implemented yet :("));
					// Ban
				}
			}
		}
	}

	public void suspiciousMessageModLog(GuildCollections gc, ChannelConfigType.Holder channelConfig, DiscordMessage message, @Nullable User user, String reason, @Nullable Function<String, String> content) {
		if (channelConfig.isSet()) {
			return;
		}

		if (user == null) {
			user = getUser(message.getUserID());
		}

		if (user == null) {
			return;
		}

		final var u = user;

		//App.error("Suspicious message by " + u.getUsername() + " detected: " + content.apply(message.getContent()) + " [" + reason + "]" + Ansi.RESET);

		BrainEventType.SUSPICIOUS_MESSAGE.build(gc.guildId).post();

		var sb1 = new StringBuilder("[Suspicious message detected in](");
		message.appendMessageURL(sb1);
		sb1.append(") <#");
		sb1.append(SnowFlake.str(message.getChannelID()));
		sb1.append("> from ").append(u.getMention()).append("\n\n");
		sb1.append(content == null ? message.getContent() : content.apply(message.getContent()));

		gc.adminLogChannelEmbed(u.getUserData(), channelConfig, spec -> {
			spec.description(sb1.toString());
			spec.timestamp(message.getDate().toInstant());
			spec.author(u.getTag(), u.getAvatarUrl());
			spec.footer(reason, null);
		});
	}

	public UserCache createUserCache() {
		return new UserCache(this);
	}

	public User getSelfUser() {
		return Objects.requireNonNull(client.getSelf().block());
	}

	@Nullable
	public User getUser(long id) {
		try {
			return id == 0L ? null : client.getUserById(SnowFlake.convert(id)).block();
		} catch (Exception ex) {
			return null;
		}
	}

	@Nullable
	public UserData getUserData(long id) {
		var user = getUser(id);
		return user == null ? null : user.getUserData();
	}

	public Optional<String> getUserName(long id) {
		var user = getUser(id);
		return user == null ? Optional.empty() : Optional.of(user.getGlobalName().orElse(user.getUsername()));
	}

	public Optional<String> getUserTag(long id) {
		var user = getUser(id);
		return user == null ? Optional.empty() : Optional.of(user.getTag());
	}

	public List<Guild> getSelfGuilds() {
		return client.getGuilds().toStream().toList();
	}
}