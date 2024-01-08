package dev.gnomebot.app.data;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.BrainEvents;
import dev.gnomebot.app.GuildPaths;
import dev.gnomebot.app.data.config.ChannelConfigType;
import dev.gnomebot.app.data.config.ConfigHolder;
import dev.gnomebot.app.data.config.ConfigKey;
import dev.gnomebot.app.data.config.FontConfigType;
import dev.gnomebot.app.data.config.GuildConfig;
import dev.gnomebot.app.data.config.RoleConfigType;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.script.DiscordJS;
import dev.gnomebot.app.script.WrappedGuild;
import dev.gnomebot.app.script.WrappedId;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.RecentUser;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.AnsiJava;
import dev.latvian.apps.webutils.json.JSON;
import dev.latvian.apps.webutils.json.JSONObject;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.CategorizableChannel;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.StartThreadWithoutMessageSpec;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ChannelData;
import discord4j.discordjson.json.MemberData;
import discord4j.discordjson.json.UserData;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Image;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuildCollections {
	public final Databases db;
	public final Snowflake guildId;
	public final WrappedId wrappedId;
	public final GuildPaths paths;
	public final MongoDatabase database;
	public DiscordJS discordJS;

	public String name;
	public String iconUrl;
	public Snowflake ownerId;
	public int feedbackNumber;
	public int pollNumber;
	public String vanityInviteCode;

	public final WrappedCollection<DiscordMember> members;
	public final WrappedCollection<DiscordMessage> messages;
	public final WrappedCollection<DiscordMessage> editedMessages;
	public final WrappedCollection<DiscordFeedback> feedback;
	public final WrappedCollection<DiscordPoll> polls;
	public final WrappedCollection<DiscordMessageCount> messageCount;
	public final WrappedCollection<DiscordMessageXP> messageXp;
	public final WrappedCollection<GnomeAuditLogEntry> auditLog;
	public final WrappedCollection<ThreadLocation> memberLogThreads;

	public final Map<ConfigKey<?, ?>, ConfigHolder<?>> configHolders = new IdentityHashMap<>();
	public final ConfigHolder<Integer> globalXp = config(GuildConfig.GLOBAL_XP);
	public final ConfigHolder<Integer> regularMessages = config(GuildConfig.REGULAR_MESSAGES);
	public final ConfigHolder<Integer> regularXP = config(GuildConfig.REGULAR_XP);
	public final RoleConfigType.Holder regularRole = config(GuildConfig.REGULAR_ROLE);
	public final RoleConfigType.Holder adminRole = config(GuildConfig.ADMIN_ROLE);
	public final RoleConfigType.Holder mutedRole = config(GuildConfig.MUTED_ROLE);
	public final RoleConfigType.Holder feedbackSuggestRole = config(GuildConfig.FEEDBACK_SUGGEST_ROLE);
	public final RoleConfigType.Holder feedbackVoteRole = config(GuildConfig.FEEDBACK_VOTE_ROLE);
	public final RoleConfigType.Holder reportMentionRole = config(GuildConfig.REPORT_MENTION_ROLE);
	public final ChannelConfigType.Holder feedbackChannel = config(GuildConfig.FEEDBACK_CHANNEL);
	public final ChannelConfigType.Holder adminLogChannel = config(GuildConfig.ADMIN_LOG_CHANNEL);
	public final ChannelConfigType.Holder adminMessagesChannel = config(GuildConfig.ADMIN_MESSAGES_CHANNEL);
	public final ChannelConfigType.Holder muteAppealChannel = config(GuildConfig.MUTE_APPEAL_CHANNEL);
	public final ChannelConfigType.Holder logNewAccountsChannel = config(GuildConfig.LOG_NEW_ACCOUNTS_CHANNEL);
	public final ChannelConfigType.Holder logLeavingChannel = config(GuildConfig.LOG_LEAVING_CHANNEL);
	public final ChannelConfigType.Holder reportChannel = config(GuildConfig.REPORT_CHANNEL);
	public final ChannelConfigType.Holder logIpAddressesChannel = config(GuildConfig.LOG_IP_ADDRESSES_CHANNEL);
	public final ChannelConfigType.Holder appealChannel = config(GuildConfig.APPEAL_CHANNEL);
	public final ConfigHolder<String> legacyPrefix = config(GuildConfig.LEGACY_PREFIX);
	public final ConfigHolder<String> macroPrefix = config(GuildConfig.MACRO_PREFIX);
	public final ConfigHolder<String> inviteCode = config(GuildConfig.INVITE_CODE);
	public final ConfigHolder<Boolean> lockdownMode = config(GuildConfig.LOCKDOWN_MODE);
	public final ConfigHolder<Integer> kickNewAccounts = config(GuildConfig.KICK_NEW_ACCOUNTS);
	public final ConfigHolder<Boolean> anonymousFeedback = config(GuildConfig.ANONYMOUS_FEEDBACK);
	public final ConfigHolder<Boolean> adminsBypassAnonFeedback = config(GuildConfig.ADMINS_BYPASS_ANON_FEEDBACK);
	public final FontConfigType.Holder font = config(GuildConfig.FONT);
	public final ConfigHolder<Integer> autoMuteUrlShortener = config(GuildConfig.AUTO_MUTE_URL_SHORTENER);
	public final ConfigHolder<Integer> autoMuteScamUrl = config(GuildConfig.AUTO_MUTE_SCAM_URL);
	public final ConfigHolder<Boolean> autoPaste = config(GuildConfig.AUTO_PASTE);
	public final ConfigHolder<List<String>> reportOptions = config(GuildConfig.REPORT_OPTIONS);
	public final ConfigHolder<Boolean> autoMuteEmbed = config(GuildConfig.AUTO_MUTE_EMBED);

	private WrappedGuild wrappedGuild;
	private Map<Snowflake, ChannelInfo> channelMap;
	private List<ChannelInfo> channelList;
	private Map<String, ChannelInfo> uniqueChannelNameMap;
	private Map<Snowflake, CachedRole> roleMap;
	private List<CachedRole> roleList;
	private Map<String, CachedRole> uniqueRoleNameMap;
	public final List<RecentUser> recentUsers;
	private List<ChatCommandSuggestion> recentUserSuggestions;
	private Map<String, Macro> macroMap;
	private final Map<Long, Snowflake> memberLogThreadCache;
	private final Map<Long, Snowflake> appealThreadCache;

	public boolean advancedLogging = false;

	public GuildCollections(Databases d, Snowflake g) {
		db = d;
		guildId = g;
		wrappedId = new WrappedId(guildId);
		paths = AppPaths.getGuildPaths(guildId);
		database = db.mongoClient.getDatabase("gnomebot_" + g.asString());

		name = guildId.asString();
		iconUrl = "";
		ownerId = Utils.NO_SNOWFLAKE;
		feedbackNumber = 0;
		pollNumber = 0;
		vanityInviteCode = "";

		if (Files.exists(paths.info)) {
			try {
				var json = JSON.DEFAULT.read(paths.info).readObject();
				name = json.asString("name");
				iconUrl = json.asString("icon_url");
				ownerId = Utils.snowflake(json.asString("owner_id"));
				feedbackNumber = json.asInt("feedback_number");
				pollNumber = json.asInt("poll_number");
				vanityInviteCode = json.asString("vanity_invite_code");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		var dbid = guildId.asString();

		members = create("members", DiscordMember::new);
		// TODO: Move messages to edited when channel is deleted
		messages = create("messages", DiscordMessage::new);
		editedMessages = create("edited_messages", DiscordMessage::new).expiresAfterMonth("timestamp_expire_" + dbid, "timestamp", null); // GDPR
		feedback = create("feedback", DiscordFeedback::new);
		polls = create("polls", DiscordPoll::new);
		messageCount = create("message_count", DiscordMessageCount::new);
		messageXp = create("message_xp", DiscordMessageXP::new);
		auditLog = create("audit_log", GnomeAuditLogEntry::new).expiresAfterMonth("timestamp_expire_" + dbid, "expires", Filters.exists("expires")); // GDPR
		memberLogThreads = create("member_log_threads", ThreadLocation::new);

		if (Files.exists(paths.config)) {
			try {
				var json = JSON.DEFAULT.read(paths.config).readObject();

				for (var entry : json.entrySet()) {
					var key = GuildConfig.get(entry.getKey());

					if (key != null && entry.getValue() != null && entry.getValue() != JSON.NULL) {
						configHolders.get(key).read(entry.getValue());
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		recentUsers = new ArrayList<>();
		memberLogThreadCache = new HashMap<>();
		appealThreadCache = new HashMap<>();

		postReadSettings();
		discordJS = new DiscordJS(this, false);
	}

	public <T extends WrappedDocument<T>> WrappedCollection<T> create(String ci, BiFunction<WrappedCollection<T>, MapWrapper, T> w) {
		return db.create(database, ci, w).gc(this);
	}

	private <T, H extends ConfigHolder<T>> H config(ConfigKey<T, H> key) {
		var holder = key.type().createHolder(this, key);
		configHolders.put(key, holder);
		return holder;
	}

	public Guild getGuild() {
		return Objects.requireNonNull(db.app.discordHandler.client.getGuildById(guildId).block());
	}

	public void saveInfo() {
		var json = JSONObject.of();
		json.put("name", name);
		json.put("icon_url", iconUrl);
		json.put("owner_id", ownerId.asString());

		if (feedbackNumber > 0) {
			json.put("feedback_number", feedbackNumber);
		}

		if (pollNumber > 0) {
			json.put("poll_number", pollNumber);
		}

		if (!vanityInviteCode.isEmpty()) {
			json.put("vanity_invite_code", vanityInviteCode);
		}

		try {
			Files.writeString(paths.info, json.toPrettyString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void saveConfig() {
		var json = JSONObject.of();

		for (var key : GuildConfig.MAP.values()) {
			var holder = configHolders.get(key);

			if (holder != null && holder.save) {
				json.put(key.id(), holder.write());
			}
		}

		App.info(Ansi.of("Saved config of " + name + ": ").append(AnsiJava.of(json)));

		try {
			Files.writeString(paths.config, json.toPrettyString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Nullable
	public MemberData getMemberData(Snowflake id) {
		try {
			return db.app.discordHandler.client.getRestClient().getGuildService().getGuildMember(guildId.asLong(), id.asLong()).block();
		} catch (Exception ex) {
			return null;
		}
	}

	@Nullable
	public Member getMember(Snowflake id) {
		try {
			return db.app.discordHandler.client.getMemberById(guildId, id).block();
		} catch (Exception ex) {
			return null;
		}
	}

	public PermissionSet getEffectiveGlobalPermissions(Snowflake member) {
		try {
			var set = getMember(member).getBasePermissions().block();

			if (set == null) {
				return PermissionSet.none();
			} else if (set.contains(Permission.ADMINISTRATOR)) {
				return PermissionSet.all();
			} else {
				return set;
			}
		} catch (Exception ignored) {
			return PermissionSet.none();
		}
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean isMM() {
		return guildId.asLong() == 166630061217153024L;
	}

	public boolean isTest() {
		return guildId.asLong() == 720671115336220693L;
	}

	public GatewayDiscordClient getClient() {
		return db.app.discordHandler.client;
	}

	public void postReadSettings() {
		// App.info("Loading settings for " + this + "...");

		//Table settingsTable = new Table("Setting", "Value");
		//
		//for (DBSetting<?, ?> setting : settings.map.values())
		//{
		//	settingsTable.addRow(setting.key, setting);
		//}
		//
		//settingsTable.print();
	}

	public String getInviteUrl() {
		if (!vanityInviteCode.isEmpty()) {
			return "https://discord.gg/" + vanityInviteCode;
		}

		var inv = inviteCode.get();

		if (!inv.isEmpty()) {
			return "https://discord.gg/" + inv;
		}

		return "";
	}

	public String getClickableName() {
		var inv = getInviteUrl();
		return inv.isEmpty() ? name : ("[" + name + "](" + inv + ")");
	}

	public Optional<Message> findMessage(@Nullable Snowflake id, @Nullable ChannelInfo priority) {
		if (id == null) {
			return Optional.empty();
		}

		var c = getChannelList();

		if (priority != null) {
			c.remove(priority);
			c.add(0, priority);
		}

		for (var channel : c) {
			var m = channel.getMessage(id);

			if (m != null) {
				return Optional.of(m);
			}
		}

		return Optional.empty();
	}

	public void adminLogChannelEmbed(@Nullable UserData user, ChannelConfigType.Holder channelConfig, Consumer<EmbedBuilder> embed) {
		var builder = EmbedBuilder.create();
		builder.color(EmbedColor.RED);
		builder.timestamp(Instant.now());
		embed.accept(builder);
		var message = MessageBuilder.create(builder);

		if (user != null) {
			var id = memberAuditLogThread(user);

			if (id.asLong() != 0L) {
				db.app.discordHandler.client.getRestClient().getChannelService().createMessage(id.asLong(), message.toMultipartMessageCreateRequest()).block();
				closeThread(id, Duration.ofMinutes(5L));
				return;
			}
		}

		channelConfig.messageChannel().ifPresent(c -> c.createMessage(message).subscribe());
	}

	public long getUserID(String tag) {
		var member = getGuild().getMembers().cache().filter(m1 -> m1.getTag().equals(tag)).blockFirst();
		return member == null ? 0L : member.getId().asLong();
	}

	public void unmute(Snowflake user, long seconds, String reason) {
		if (seconds <= 0L) {
			ScheduledTask.unmuteNow(this, user, reason);
		} else if (seconds < Integer.MAX_VALUE) {
			db.app.schedule(Duration.ofSeconds(seconds), ScheduledTask.UNMUTE, guildId.asLong(), 0L, user.asLong(), reason);
		}
	}

	public MemberCache createMemberCache() {
		return new MemberCache(this);
	}

	public ChannelInfo getChannelInfo(Snowflake id) {
		return Objects.requireNonNull(getChannelMap().get(id));
	}

	public ChannelInfo getChannelInfo(Channel channel) {
		return getChannelInfo(channel.getId());
	}

	public String getChannelName(Snowflake channel) {
		ChannelInfo c = getChannelMap().get(channel);
		return c == null ? channel.asString() : c.getName();
	}

	public JSONObject getChannelJson(Snowflake channel) {
		var json = JSONObject.of();
		json.put("id", channel.asString());
		json.put("name", getChannelName(channel));
		return json;
	}

	public AuthLevel getAuthLevel(@Nullable Member member) {
		if (member == null) {
			return AuthLevel.NO_AUTH;
		} else if (member.getId().equals(db.app.discordHandler.selfId)) {
			return AuthLevel.OWNER;
		} else if (member.getId().equals(getGuild().getOwnerId())) {
			return AuthLevel.OWNER;
		}

		Set<Snowflake> roleIds = member.getRoleIds();

		for (Snowflake id : roleIds) {
			CachedRole r = getRoleMap().get(id);

			if (r != null && r.ownerRole) {
				return AuthLevel.OWNER;
			}
		}

		for (Snowflake id : roleIds) {
			CachedRole r = getRoleMap().get(id);

			if (r != null && r.adminRole) {
				return AuthLevel.ADMIN;
			}
		}

		return AuthLevel.MEMBER;
	}

	public AuthLevel getAuthLevel(@Nullable Snowflake memberId) {
		if (memberId == null) {
			return AuthLevel.NO_AUTH;
		} else if (memberId.equals(db.app.discordHandler.selfId)) {
			return AuthLevel.OWNER;
		} else if (memberId.equals(getGuild().getOwnerId())) {
			return AuthLevel.OWNER;
		}

		MemberData data = getMemberData(memberId);

		if (data == null) {
			return AuthLevel.NO_AUTH;
		}

		List<Id> roleIds = data.roles();

		for (Id id : roleIds) {
			CachedRole r = getRoleMap().get(Snowflake.of(id));

			if (r != null && r.ownerRole) {
				return AuthLevel.OWNER;
			}
		}

		for (Id id : roleIds) {
			CachedRole r = getRoleMap().get(Snowflake.of(id));

			if (r != null && r.adminRole) {
				return AuthLevel.ADMIN;
			}
		}

		return AuthLevel.MEMBER;
	}

	private void refreshCache() {
		refreshChannelCache();
		wrappedGuild = null;
		roleMap = null;
		roleList = null;
		uniqueRoleNameMap = null;
		macroMap = null;
	}

	public void guildUpdated(@Nullable Guild g) {
		App.LOGGER.event(BrainEvents.REFRESHED_GUILD_CACHE);
		refreshCache();
		boolean saveInfo = false;

		if (g != null) {
			var n = g.getName();

			if (!name.equals(n)) {
				name = n;
				saveInfo = true;
			}

			var i = g.getIconUrl(Image.Format.PNG).orElse("");

			if (!iconUrl.equals(i)) {
				iconUrl = i;
				saveInfo = true;
			}

			var o = g.getOwnerId();

			if (!ownerId.equals(o)) {
				ownerId = o;
				saveInfo = true;
			}

			var v = g.getVanityUrlCode().orElse("");

			if (!vanityInviteCode.equals(v)) {
				vanityInviteCode = v;
				saveInfo = true;
			}
		}

		if (saveInfo) {
			saveInfo();
		}
	}

	public void channelUpdated(@Nullable CategorizableChannel old, TopLevelGuildMessageChannel channel, boolean deleted) {
		App.LOGGER.event(BrainEvents.REFRESHED_CHANNEL_CACHE);
		refreshCache();

		if (advancedLogging) {
			App.info("Channel updated: " + this + "/#" + channel.getName());

			if (old != null) {
				if (!old.getName().equals(channel.getName())) {
					App.info("> Name " + old.getName() + " -> " + channel.getName());
				}

				if (old.getRawPosition() != channel.getRawPosition()) {
					App.info("> Position " + old.getRawPosition() + " -> " + channel.getRawPosition());
				}
			}
		}

		if (!deleted) {
			ChannelInfo ci = getChannelMap().get(channel.getId());

			if (ci != null) {
				ci.settings.updateFrom(channel);
			} else {
				App.error("Unknown channel " + channel.getId().asString() + "/" + channel.getName() + " updated!");
			}
		}
	}

	public void roleUpdated(Snowflake roleId, boolean deleted) {
		App.LOGGER.event(BrainEvents.REFRESHED_ROLE_CACHE);
		refreshCache();
	}

	// 0 update | 1 join | 2 leave
	public void memberUpdated(Snowflake userId, int type) {
		if (type == 0) {
			// App.LOGGER.refreshedMemberCache();
		}
		// refreshCache();
	}

	public synchronized void refreshChannelCache() {
		channelMap = null;
		channelList = null;
		uniqueChannelNameMap = null;
	}

	public synchronized Map<Snowflake, ChannelInfo> getChannelMap() {
		if (channelMap == null) {
			channelMap = new LinkedHashMap<>();

			try {
				for (var ch : getGuild().getChannels()
						.filter(c -> c instanceof TopLevelGuildMessageChannel)
						.cast(TopLevelGuildMessageChannel.class)
						.sort(Comparator.comparing(TopLevelGuildMessageChannel::getRawPosition).thenComparing(TopLevelGuildMessageChannel::getId))
						.toIterable()) {
					var settings = getChannelSettings(ch.getId());
					settings.updateFrom(ch);
					var c = new ChannelInfo(this, ch.getId(), settings);
					channelMap.put(ch.getId(), c);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return channelMap;
	}

	public List<ChannelInfo> getChannelList() {
		if (channelList == null) {
			channelList = new ArrayList<>(getChannelMap().values());
		}

		return channelList;
	}

	public Map<String, ChannelInfo> getUniqueChannelNameMap() {
		if (uniqueChannelNameMap == null) {
			uniqueChannelNameMap = new LinkedHashMap<>();

			for (var c : getChannelMap().values()) {
				var name = c.getName().toLowerCase().replace(' ', '-');
				var key = name;

				for (int i = 2; uniqueChannelNameMap.containsKey(key); i++) {
					key = name + '-' + i;
				}

				uniqueChannelNameMap.put(key, c);
			}
		}

		return uniqueChannelNameMap;
	}

	public List<CachedRole> getRoleList() {
		if (roleList == null) {
			roleList = new ArrayList<>();
			var adminRoleId = adminRole.get().asLong();
			CachedRole adminRoleW = null;

			try {
				for (var r : getGuild().getRoles()
						.filter(r -> !r.isEveryone())
						.sort(Comparator.comparing(Role::getRawPosition).thenComparing(Role::getId).reversed())
						.toStream()
						.toList()) {
					var role = new CachedRole(this, r, roleList.size());

					if (role.id.asLong() == adminRoleId) {
						adminRoleW = role;
					}

					roleList.add(role);
				}
			} catch (Exception ex) {
			}

			if (adminRoleW != null) {
				for (var role : roleList) {
					if (role.index <= adminRoleW.index) {
						role.adminRole = true;
					}
				}
			}
		}

		return roleList;
	}

	public Map<Snowflake, CachedRole> getRoleMap() {
		if (roleMap == null) {
			roleMap = new LinkedHashMap<>();

			for (var r : getRoleList()) {
				roleMap.put(r.id, r);
			}
		}

		return roleMap;
	}

	public Map<String, CachedRole> getUniqueRoleNameMap() {
		if (uniqueRoleNameMap == null) {
			uniqueRoleNameMap = new LinkedHashMap<>();

			for (var c : getRoleList()) {
				var name = c.name.toLowerCase().replace(' ', '-');
				var key = name;

				for (int i = 2; uniqueRoleNameMap.containsKey(key); i++) {
					key = name + '-' + i;
				}

				uniqueRoleNameMap.put(key, c);
			}
		}

		return uniqueRoleNameMap;
	}

	public void auditLog(GnomeAuditLogEntry.Builder builder) {
		try {
			auditLog.insert(builder.build());
		} catch (Exception ex) {
			Ansi.log(Ansi.orange("Failed to write to audit log [" + builder.type.name + "]: ").append(Ansi.darkRed(ex)));
		}
	}

	public WrappedGuild getWrappedGuild() {
		if (wrappedGuild == null) {
			wrappedGuild = new WrappedGuild(discordJS, this);
		}

		return wrappedGuild;
	}

	public Stream<Member> getMemberStream() {
		return getGuild().getMembers().toStream();
	}

	public List<Member> getMembers() {
		return getMemberStream().toList();
	}

	private ChannelSettings getChannelSettings(Snowflake id) {
		var settings = db.channelSettings.findFirst(id);

		if (settings == null) {
			settings = new ChannelSettings(db.channelSettings, MapWrapper.wrap(new Document("_id", id.asLong())));
			db.channelSettings.insert(settings.document.toDocument());
		}

		return settings;
	}

	public ChannelInfo getOrMakeChannelInfo(Snowflake id) {
		ChannelInfo ci = getChannelMap().get(id);

		if (ci == null) {
			ci = new ChannelInfo(this, id, getChannelSettings(id));
			ChannelData data = ci.getChannelData();
			Id parentId = data == null ? null : data.parentId().toOptional().orElse(Optional.empty()).orElse(null);

			if (parentId != null) {
				ci = getOrMakeChannelInfo(Snowflake.of(parentId)).thread(id, "-");
			}
		}

		return ci;
	}

	public void usernameSuggestions(ChatCommandSuggestionEvent event) {
		if (recentUserSuggestions == null) {
			recentUserSuggestions = new ArrayList<>();

			for (int i = 0; i < recentUsers.size(); i++) {
				RecentUser user = recentUsers.get(i);
				recentUserSuggestions.add(new ChatCommandSuggestion(user.tag(), user.id().asString(), user.tag().toLowerCase(), recentUsers.size() - i));
			}

			Set<Snowflake> set = recentUsers.stream().map(RecentUser::id).collect(Collectors.toSet());

			for (Member member : getMembers()) {
				if (!set.contains(member.getId())) {
					recentUserSuggestions.add(new ChatCommandSuggestion(member.getTag(), member.getId().asString(), member.getTag().toLowerCase(), 0));
				}
			}
		}

		event.transformSearch = s -> s.toLowerCase().replace('@', ' ').trim();
		event.suggestions.addAll(recentUserSuggestions);
	}

	public void pushRecentUser(Snowflake userId, String tag) {
		if (!recentUsers.isEmpty() && recentUsers.get(0).id().equals(userId)) {
			return;
		}

		if (tag.endsWith("#0")) {
			tag = tag.substring(0, tag.length() - 2);
		} else if (tag.endsWith("#null")) {
			tag = tag.substring(0, tag.length() - 5);
		}

		recentUserSuggestions = null;
		RecentUser user = new RecentUser(userId, tag);
		recentUsers.remove(user);
		recentUsers.add(0, user);

		if (recentUsers.size() > 1000) {
			recentUsers.remove(recentUsers.size() - 1);
		}
	}

	public Map<String, Macro> getMacroMap() {
		if (macroMap == null) {
			macroMap = new LinkedHashMap<>();

			try {
				if (Files.exists(paths.macrosFile)) {
					for (var entry : JSON.DEFAULT.read(paths.macrosFile).readObject().entrySet()) {
						if (entry.getValue() instanceof JSONObject json) {
							var macro = new Macro(this);
							macro.id = entry.getKey().toLowerCase();
							macro.name = entry.getKey();
							macro.content = json.asString("content");
							macro.author = json.asLong("author");
							macro.created = json.containsKey("created") ? Instant.parse(json.asString("created")) : null;
							macro.uses = json.asInt("uses");
							macro.slashCommand = json.asLong("slash_command");
							macroMap.put(macro.id, macro);
						}
					}
				}
			} catch (Exception ex) {
				macroMap = null;
				ex.printStackTrace();
			}
		}

		return macroMap;
	}

	@Nullable
	public Macro getMacro(String name) {
		var m = getMacroMap().get(name.toLowerCase());

		if (m == null) {
			if (name.startsWith("moddedmc:")) {
				return db.guild(Snowflake.of(166630061217153024L)).getMacro(name.substring(9));
			} else if (name.startsWith("lat:")) {
				return db.guild(Snowflake.of(303440391124942858L)).getMacro(name.substring(4));
			}
		}

		return m;
	}

	public void saveMacroMap() {
		var json = JSONObject.of();

		for (var macro : getMacroMap().values()) {
			var obj = JSONObject.of();
			obj.put("content", macro.content);
			obj.put("author", macro.author);

			if (macro.created != null) {
				obj.put("created", macro.created.toString());
			}

			if (macro.uses > 0) {
				obj.put("uses", macro.uses);
			}

			if (macro.slashCommand != 0L) {
				obj.put("slash_command", macro.slashCommand);
			}

			json.put(macro.name, obj);
		}

		try {
			if (Files.notExists(paths.macrosFile)) {
				Files.createFile(paths.macrosFile);
			}

			Files.writeString(paths.macrosFile, json.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private Snowflake memberLogThread(int type, Map<Long, Snowflake> cache, @Nullable UserData user, ChannelConfigType.Holder config) {
		if (user == null) {
			return Utils.NO_SNOWFLAKE;
		}

		var id = cache.get(user.id().asLong());

		exit:
		if (id == null) {
			var ci = config.messageChannel().orElse(null);
			var topLevelChannel = ci == null ? null : ci.getTopLevelChannel();

			if (topLevelChannel != null) {
				try {
					var doc = memberLogThreads.query(user.id().asLong()).eq("type", type).eq("channel", ci.id.asLong()).projectionFields("thread").first();

					if (doc != null) {
						var thread = db.app.discordHandler.client.getChannelById(doc.thread).cast(ThreadChannel.class).block();

						if (thread != null) {
							id = thread.getId();
							break exit;
						}
					}
				} catch (Exception ignore) {
				}

				var thread = topLevelChannel.startThread(StartThreadWithoutMessageSpec.builder()
						.type(type == 0 ? ThreadChannel.Type.GUILD_PUBLIC_THREAD : ThreadChannel.Type.GUILD_PRIVATE_THREAD)
						.invitable(false)
						.reason(user.username() + " Member Channel")
						.name(user.username())
						.autoArchiveDuration(type == 0 ? ThreadChannel.AutoArchiveDuration.DURATION1 : ThreadChannel.AutoArchiveDuration.DURATION2)
						.build()
				).block();

				memberLogThreads.query(user.id().asLong()).eq("type", type).eq("channel", ci.id.asLong()).upsert(List.of(Updates.set("thread", thread.getId().asLong())));

				if (type == 0) {
					var list = new ArrayList<String>();
					list.add("# <@" + user.id().asString() + ">");
					list.add("### ID");
					list.add(user.id().asString());
					list.add("### Username");
					list.add(user.username());
					list.add("### Global Name");
					list.add(user.globalName().orElse(user.username()));
					list.add("### Account Created");
					list.add(Utils.formatRelativeDate(Snowflake.of(user.id().asLong()).getTimestamp()));

					try {
						var member = getMember(Snowflake.of(user.id().asLong()));

						if (member != null && member.getJoinTime().isPresent()) {
							list.add("### Joined");
							list.add(Utils.formatRelativeDate(member.getJoinTime().get()));
						}
					} catch (Exception ignored) {
					}

					thread.createMessage(MessageCreateSpec.builder()
							.content(String.join("\n", list))
							.allowedMentions(AllowedMentions.builder()
									.allowUser(Snowflake.of(user.id().asLong()))
									.build()
							)
							.build()
					).subscribe();
				}

				var adminRole = this.adminRole.get();

				if (adminRole.asLong() != 0L) {
					thread.createMessage("...").withAllowedMentions(AllowedMentions.builder()
							.allowRole(adminRole)
							.build()
					).flatMap(m -> m.edit(MessageEditSpec.builder()
							.allowedMentionsOrNull(AllowedMentions.builder()
									.allowRole(adminRole)
									.build()
							)
							.contentOrNull("Adding <@&" + adminRole.asString() + ">...")
							.build()
					)).flatMap(Message::delete).subscribe();
				}

				id = thread.getId();
			}
		}

		if (id == null) {
			id = Utils.NO_SNOWFLAKE;
		}

		cache.put(user.id().asLong(), id);
		return id;
	}

	public Snowflake memberAuditLogThread(@Nullable UserData user) {
		return memberLogThread(0, memberLogThreadCache, user, adminLogChannel);
	}

	public Snowflake memberAppealThread(@Nullable UserData user) {
		return memberLogThread(1, appealThreadCache, user, appealChannel);
	}

	public void closeThread(Snowflake threadId, Duration duration) {
		var task = db.app.findScheduledGuildTask(guildId, t -> t.type.equals(ScheduledTask.CLOSE_THREAD) && t.channelId.asLong() == threadId.asLong());

		if (task != null) {
			task.changeEnd(Math.min(task.end, System.currentTimeMillis() + duration.toMillis()));
		} else {
			db.app.schedule(duration, ScheduledTask.CLOSE_THREAD, guildId.asLong(), threadId.asLong(), 0L, "");
		}
	}

	public ConfigHolder<?> getConfigHolder(String key) {
		var k = GuildConfig.get(key);
		return k == null ? null : configHolders.get(k);
	}
}