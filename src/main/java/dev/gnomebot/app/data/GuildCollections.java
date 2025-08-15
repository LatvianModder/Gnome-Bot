package dev.gnomebot.app.data;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.GuildPaths;
import dev.gnomebot.app.data.channel.CachedChannels;
import dev.gnomebot.app.data.channel.TopLevelChannelInfo;
import dev.gnomebot.app.data.config.ChannelConfigType;
import dev.gnomebot.app.data.config.ConfigHolder;
import dev.gnomebot.app.data.config.ConfigKey;
import dev.gnomebot.app.data.config.FontConfigType;
import dev.gnomebot.app.data.config.GuildConfig;
import dev.gnomebot.app.data.config.RoleConfigType;
import dev.gnomebot.app.discord.CachedRoles;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.RecentUser;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.ANSI;
import dev.latvian.apps.ansi.JavaANSI;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.json.JSON;
import dev.latvian.apps.json.JSONObject;
import dev.latvian.apps.webutils.data.HexId32;
import dev.latvian.apps.webutils.data.MutableInt;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.CategorizableChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.object.entity.channel.TopLevelGuildMessageWithThreadsChannel;
import discord4j.core.spec.StartThreadWithoutMessageSpec;
import discord4j.discordjson.json.MemberData;
import discord4j.discordjson.json.UserData;
import discord4j.rest.util.Image;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuildCollections {
	public final Databases db;
	public final long guildId;
	public final GuildPaths paths;
	public final MongoDatabase database;

	public String name;
	public String iconUrl;
	public long ownerId;
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
	public final WrappedCollection<JoinLogEntry> joinLog;
	public final WrappedCollection<GnomeAuditLogEntry> voiceLog;
	public final WrappedCollection<GnomeAuditLogEntry> reactionLog;
	public final WrappedCollection<CommandLogEntry> commandLog;
	public final WrappedCollection<ThreadLocation> memberLogThreads;
	public final WrappedCollection<EchoLogEntry> echoLog;

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
	public final ConfigHolder<Boolean> autoPaste = config(GuildConfig.AUTO_PASTE);
	public final ConfigHolder<Boolean> autoMuteEmbed = config(GuildConfig.AUTO_MUTE_EMBED);

	private CachedChannels channels;
	private CachedRoles roles;
	public final List<RecentUser> recentUsers;
	private List<ChatCommandSuggestion> recentUserSuggestions;
	private final Map<String, Macro> macroMap;
	private Map<Integer, MutableInt> macroUseMap;
	private final Map<Long, Long> memberLogThreadCache;
	private final Map<Long, Long> appealThreadCache;
	private List<RelatedGuild.Group> relatedGuilds;

	public GuildCollections(Databases d, long g) {
		db = d;
		guildId = g;
		paths = AppPaths.getGuildPaths(g);

		var dbid = SnowFlake.str(guildId);
		database = db.mongoClient.getDatabase("gnomebot_" + dbid);

		name = dbid;
		iconUrl = "";
		ownerId = 0L;
		feedbackNumber = 0;
		pollNumber = 0;
		vanityInviteCode = "";

		if (Files.exists(paths.info)) {
			try {
				var json = JSON.DEFAULT.read(paths.info).readObject();
				name = json.asString("name");
				iconUrl = json.asString("icon_url");
				ownerId = SnowFlake.num(json.asString("owner_id"));
				feedbackNumber = json.asInt("feedback_number");
				pollNumber = json.asInt("poll_number");
				vanityInviteCode = json.asString("vanity_invite_code");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		Log.info("Loading guild '" + this + "'...");

		members = create("members", DiscordMember::new);
		// TODO: Move messages to edited when channel is deleted
		messages = create("messages", DiscordMessage::new);
		editedMessages = create("edited_messages", DiscordMessage::new).expiresAfterMonth("timestamp_expire", "timestamp", null);
		feedback = create("feedback", DiscordFeedback::new);
		polls = create("polls", DiscordPoll::new);
		messageCount = create("message_count", DiscordMessageCount::new);
		messageXp = create("message_xp", DiscordMessageXP::new);
		auditLog = create("audit_log", GnomeAuditLogEntry::new).expiresAfterMonth("expires", "expires", Filters.exists("expires"));
		joinLog = create("join_log", JoinLogEntry::new);
		voiceLog = create("voice_log", GnomeAuditLogEntry::new).expiresAfterMonth("expires", "expires", Filters.exists("expires"));
		reactionLog = create("reaction_log", GnomeAuditLogEntry::new).expiresAfterMonth("expires", "expires", Filters.exists("expires"));
		commandLog = create("command_log", CommandLogEntry::new);
		memberLogThreads = create("member_log_threads", ThreadLocation::new);
		echoLog = create("echo_log", EchoLogEntry::new);

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

		macroMap = new HashMap<>();

		getMacroUseMap().clear();

		try {
			if (Files.exists(paths.macrosFile)) {
				for (var entry : JSON.DEFAULT.read(paths.macrosFile).readObject().entrySet()) {
					if (entry.getValue() instanceof JSONObject json) {
						var macro = new Macro(this);

						if (json.containsKey("name")) {
							macro.id = HexId32.of(entry.getKey());
							db.loadMacro(macro);
							macro.name = json.asString("name");
						} else {
							db.findMacroId(macro);
							macro.name = entry.getKey();
						}

						macro.stringId = macro.name.toLowerCase();
						macro.author = json.asLong("author");
						macro.created = json.containsKey("created") ? Instant.parse(json.asString("created")) : null;
						macro.slashCommand = json.asLong("slash_command");
						macro.displayName = json.asString("display_name");
						macro.emoji = Utils.stringToReaction(json.asString("emoji"));
						macroMap.put(macro.stringId, macro);

						if (json.containsKey("content")) {
							macro.setContent(json.asString("content"));
						}

						if (json.containsKey("uses")) {
							getMacroUseMap().computeIfAbsent(macro.id.getAsInt(), MutableInt.MAP_VALUE).add(json.asInt("uses"));
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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
		return Objects.requireNonNull(db.app.discordHandler.client.getGuildById(SnowFlake.convert(guildId)).block());
	}

	public void saveInfo() {
		var json = JSONObject.of();
		json.put("name", name);
		json.put("icon_url", iconUrl);
		json.put("owner_id", SnowFlake.str(ownerId));

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

	public String url() {
		return "/guilds/" + paths.key;
	}

	public String apiUrl() {
		return "/api/guilds/" + paths.key;
	}

	public void saveConfig() {
		var json = JSONObject.of();

		for (var key : GuildConfig.MAP.values()) {
			var holder = configHolders.get(key);

			if (holder != null && holder.save) {
				json.put(key.id(), holder.write());
			}
		}

		Log.info(ANSI.of("Saved config of " + name + ": ").append(JavaANSI.of(json)));

		try {
			Files.writeString(paths.config, json.toPrettyString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Nullable
	public MemberData getMemberData(long id) {
		try {
			return db.app.discordHandler.client.getRestClient().getGuildService().getGuildMember(guildId, id).block();
		} catch (Exception ex) {
			return null;
		}
	}

	@Nullable
	public Member getMember(long id) {
		try {
			return db.app.discordHandler.client.getMemberById(SnowFlake.convert(guildId), SnowFlake.convert(id)).block();
		} catch (Exception ex) {
			return null;
		}
	}

	public PermissionSet getEffectiveGlobalPermissions(long member) {
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
		return guildId == 166630061217153024L;
	}

	public boolean isTest() {
		return guildId == 720671115336220693L;
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
		return inv.isEmpty() ? name : ("[" + name + "](<" + inv + ">)");
	}

	public Optional<Message> findMessage(long id, @Nullable TopLevelChannelInfo priority) {
		if (id == 0L) {
			return Optional.empty();
		}

		var c = channels().list;

		if (priority != null) {
			c.remove(priority);
			c.addFirst(priority);
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

			if (id != 0L) {
				db.app.discordHandler.client.getRestClient().getChannelService().createMessage(id, message.toMultipartMessageCreateRequest()).block();
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

	public void unmute(long user, long seconds, String reason) {
		if (seconds <= 0L) {
			ScheduledTask.unmuteNow(this, user, reason);
		} else if (seconds < Integer.MAX_VALUE) {
			db.app.schedule(Duration.ofSeconds(seconds), ScheduledTask.UNMUTE, guildId, 0L, user, reason);
		}
	}

	public MemberCache createMemberCache() {
		return new MemberCache(this);
	}

	public AuthLevel getAuthLevel(@Nullable Member member) {
		if (member == null) {
			return AuthLevel.NO_AUTH;
		} else if (member.getId().asLong() == db.app.discordHandler.selfId) {
			return AuthLevel.OWNER;
		} else if (member.getId().asLong() == getGuild().getOwnerId().asLong()) {
			return AuthLevel.OWNER;
		}

		var roleIds = member.getRoleIds();

		for (var id : roleIds) {
			var r = roles().get(id.asLong());

			if (r != null && r.ownerRole) {
				return AuthLevel.OWNER;
			}
		}

		for (var id : roleIds) {
			var r = roles().get(id.asLong());

			if (r != null && r.adminRole) {
				return AuthLevel.ADMIN;
			}
		}

		return AuthLevel.MEMBER;
	}

	public AuthLevel getAuthLevel(long memberId) {
		if (memberId == 0L) {
			return AuthLevel.NO_AUTH;
		} else if (memberId == db.app.discordHandler.selfId) {
			return AuthLevel.OWNER;
		} else if (memberId == ownerId) {
			return AuthLevel.OWNER;
		}

		var data = getMemberData(memberId);

		if (data == null) {
			return AuthLevel.NO_AUTH;
		}

		var roleIds = data.roles();

		for (var id : roleIds) {
			var r = roles().get(id.asLong());

			if (r != null && r.ownerRole) {
				return AuthLevel.OWNER;
			}
		}

		for (var id : roleIds) {
			var r = roles().get(id.asLong());

			if (r != null && r.adminRole) {
				return AuthLevel.ADMIN;
			}
		}

		return AuthLevel.MEMBER;
	}

	private void refreshCache() {
		channels = null;
		roles = null;
		macroUseMap = null;
	}

	public void guildUpdated(@Nullable Guild g) {
		refreshCache();
		var saveInfo = false;

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

			var o = g.getOwnerId().asLong();

			if (ownerId != o) {
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

	public void channelUpdated(@Nullable CategorizableChannel old, CategorizableChannel channel, boolean deleted) {
		refreshCache();
	}

	public void roleUpdated(long roleId, boolean deleted) {
		refreshCache();
	}

	public void refreshPermissions() {
		var c = channels;

		if (c != null) {
			for (var ch : c) {
				ch.refreshPermissions();
			}
		}
	}

	// 0 update | 1 join | 2 leave
	public void memberUpdated(long userId, int type) {
		if (type == 0) {
			// App.LOGGER.refreshedMemberCache();
		}

		refreshPermissions();
	}

	public CachedChannels channels() {
		if (channels == null) {
			channels = new CachedChannels(this);
		}

		return channels;
	}

	public CachedRoles roles() {
		if (roles == null) {
			roles = new CachedRoles(this);
		}

		return roles;
	}

	public void auditLog(GnomeAuditLogEntry.Builder builder) {
		try {
			builder.type.collection.apply(this).insert(builder.build());
		} catch (Exception ex) {
			Log.info(ANSI.orange("Failed to write to audit log [" + builder.type.name + "]: ").append(ANSI.darkRed(ex)));
		}
	}

	public Stream<Member> getMemberStream() {
		return getGuild().getMembers().toStream();
	}

	public List<Member> getMembers() {
		return getMemberStream().toList();
	}

	public void usernameSuggestions(ChatCommandSuggestionEvent event) {
		if (recentUserSuggestions == null) {
			recentUserSuggestions = new ArrayList<>();

			for (var i = 0; i < recentUsers.size(); i++) {
				var user = recentUsers.get(i);
				recentUserSuggestions.add(new ChatCommandSuggestion(user.tag(), SnowFlake.str(user.id()), user.tag().toLowerCase(), recentUsers.size() - i));
			}

			var set = recentUsers.stream().map(RecentUser::id).collect(Collectors.toSet());

			for (var member : getMembers()) {
				if (!set.contains(member.getId().asLong())) {
					recentUserSuggestions.add(new ChatCommandSuggestion(member.getTag(), member.getId().asString(), member.getTag().toLowerCase(), 0));
				}
			}
		}

		event.transformSearch = s -> s.toLowerCase().replace('@', ' ').trim();
		event.suggestions.addAll(recentUserSuggestions);
	}

	public void pushRecentUser(long userId, String tag) {
		if (!recentUsers.isEmpty() && recentUsers.getFirst().id() == userId) {
			return;
		}

		if (tag.endsWith("#0")) {
			tag = tag.substring(0, tag.length() - 2);
		} else if (tag.endsWith("#null")) {
			tag = tag.substring(0, tag.length() - 5);
		}

		recentUserSuggestions = null;
		var user = new RecentUser(userId, tag);
		recentUsers.remove(user);
		recentUsers.addFirst(user);

		if (recentUsers.size() > 1000) {
			recentUsers.removeLast();
		}
	}

	public Map<String, Macro> getMacroMap() {
		return macroMap;
	}

	@Nullable
	public Macro getMacro(String name) {
		var m = getMacroMap().get(name.toLowerCase());

		if (m == null) {
			if (name.startsWith("moddedmc:")) {
				return db.guild(166630061217153024L).getMacro(name.substring(9));
			} else if (name.startsWith("lat:")) {
				return db.guild(303440391124942858L).getMacro(name.substring(4));
			}
		}

		return m;
	}

	public Macro getMacroFromCommand(String name) {
		if (name.isEmpty()) {
			throw new GnomeException("Macro name can't be empty!");
		}

		try {
			var m = db.allMacros.get(HexId32.of(name).getAsInt());

			if (m != null) {
				return m;
			}
		} catch (Exception ignored) {
		}

		var m = getMacro(name);

		if (m == null) {
			throw new GnomeException("Macro '" + name + "' not found!");
		}

		return m;
	}

	public boolean macroExists(String name) {
		return getMacro(name) != null || HexId32.isValid(name) && db.allMacros.containsKey(HexId32.of(name).getAsInt());
	}

	public void saveMacroMap() {
		var json = JSONObject.of();

		for (var macro : getMacroMap().values()) {
			var obj = JSONObject.of();
			obj.put("name", macro.name);
			obj.put("author", macro.author);

			if (macro.created != null) {
				obj.put("created", macro.created.toString());
			}

			if (macro.slashCommand != 0L) {
				obj.put("slash_command", macro.slashCommand);
			}

			if (!macro.displayName.isEmpty()) {
				obj.put("display_name", macro.displayName);
			}

			if (macro.emoji != null) {
				obj.put("emoji", Utils.reactionToString(macro.emoji));
			}

			json.put(macro.id.toString(), obj);
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

	public Map<Integer, MutableInt> getMacroUseMap() {
		if (macroUseMap == null) {
			macroUseMap = new HashMap<>();
			var ported = false;

			try {
				if (Files.exists(paths.macroUseFile)) {
					for (var line : Files.readAllLines(paths.macroUseFile)) {
						if (line.isBlank()) {
							continue;
						}

						var split = line.lastIndexOf(':');

						if (split == -1) {
							continue;
						}

						var key = line.substring(0, split);
						var value = new MutableInt(Integer.parseInt(line.substring(split + 1)));

						macroUseMap.put(HexId32.of(key).getAsInt(), value);
						ported = true;
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				ported = false;
			}

			if (ported) {
				saveMacroUseMap();
			}
		}

		return macroUseMap;
	}

	public int getMacroUses(int id) {
		var m = getMacroUseMap().get(id);
		return m == null ? 0 : m.value;
	}

	public void addMacroUse(int id) {
		getMacroUseMap().computeIfAbsent(id, MutableInt.MAP_VALUE).add(1);
		saveMacroUseMap();
	}

	private static int compareMacroEntries(Map.Entry<Integer, MutableInt> o1, Map.Entry<Integer, MutableInt> o2) {
		int i = Integer.compare(o2.getValue().value, o1.getValue().value);
		return i == 0 ? o1.getKey().compareTo(o2.getKey()) : i;
	}

	public void saveMacroUseMap() {
		var lines = new ArrayList<String>();

		for (var entry : getMacroUseMap().entrySet().stream().sorted(GuildCollections::compareMacroEntries).toList()) {
			lines.add(HexId32.of(entry.getKey()).toString() + ":" + entry.getValue().value);
		}

		try {
			Files.write(paths.macroUseFile, lines);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private long memberLogThread(int type, Map<Long, Long> cache, @Nullable UserData user, ChannelConfigType.Holder config) {
		if (user == null) {
			return 0L;
		}

		var id = cache.get(user.id().asLong());

		exit:
		if (id == null) {
			var ci = config.messageChannel().orElse(null);
			var topLevelChannel = ci == null ? null : ci.getTopLevelChannel();

			if (topLevelChannel instanceof TopLevelGuildMessageWithThreadsChannel msgc) {
				try {
					var doc = memberLogThreads.query(user.id().asLong()).eq("type", type).eq("channel", ci.id).projectionFields("thread").first();

					if (doc != null) {
						var thread = db.app.discordHandler.client.getChannelById(SnowFlake.convert(doc.thread())).cast(ThreadChannel.class).block();

						if (thread != null) {
							id = thread.getId().asLong();
							break exit;
						}
					}
				} catch (Exception ignore) {
				}

				var thread = msgc.startPublicThreadWithoutMessage(StartThreadWithoutMessageSpec.builder()
						.type(type == 0 ? ThreadChannel.Type.GUILD_PUBLIC_THREAD : ThreadChannel.Type.GUILD_PRIVATE_THREAD)
						.invitable(false)
						.reason(user.username() + " Member Channel")
						.name(user.username())
						.autoArchiveDuration(type == 0 ? ThreadChannel.AutoArchiveDuration.DURATION1 : ThreadChannel.AutoArchiveDuration.DURATION2)
						.build()
				).block();

				memberLogThreads.query(user.id().asLong()).eq("type", type).eq("channel", ci.id).upsert(List.of(Updates.set("thread", thread.getId().asLong())));

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
					list.add(Utils.formatRelativeDate(SnowFlake.convert(user.id().asLong()).getTimestamp()));

					try {
						var member = getMember(user.id().asLong());

						if (member != null && member.getJoinTime().isPresent()) {
							list.add("### Joined");
							list.add(Utils.formatRelativeDate(member.getJoinTime().get()));
						}
					} catch (Exception ignored) {
					}

					thread.createMessage(MessageBuilder.create().content(list).allowUserMentions(user.id().asLong()).toMessageCreateSpec()).subscribe();
				}

				var adminRole = this.adminRole.get();

				if (adminRole != 0L) {
					thread.createMessage(MessageBuilder.create("...")
							.allowRoleMentions(adminRole)
							.toMessageCreateSpec()
					).flatMap(m -> m.edit(MessageBuilder.create()
							.allowRoleMentions(adminRole)
							.content("Adding <@&" + adminRole + ">...")
							.toMessageEditSpec()
					)).flatMap(Message::delete).subscribe();
				}

				id = thread.getId().asLong();
			}
		}

		if (id == null) {
			id = 0L;
		}

		cache.put(user.id().asLong(), id);
		return id;
	}

	public long memberAuditLogThread(@Nullable UserData user) {
		return memberLogThread(0, memberLogThreadCache, user, adminLogChannel);
	}

	public long memberAppealThread(@Nullable UserData user) {
		return memberLogThread(1, appealThreadCache, user, appealChannel);
	}

	public void closeThread(long threadId, Duration duration) {
		var task = db.app.findScheduledGuildTask(guildId, t -> t.type.equals(ScheduledTask.CLOSE_THREAD) && t.channelId == threadId);

		if (task != null) {
			task.changeEnd(Math.min(task.end, System.currentTimeMillis() + duration.toMillis()));
		} else {
			db.app.schedule(duration, ScheduledTask.CLOSE_THREAD, guildId, threadId, 0L, "");
		}
	}

	public ConfigHolder<?> getConfigHolder(String key) {
		var k = GuildConfig.get(key);
		return k == null ? null : configHolders.get(k);
	}

	public List<RelatedGuild.Group> getRelatedGuilds() {
		if (relatedGuilds == null) {
			relatedGuilds = new ArrayList<>();

			if (Files.exists(paths.relatedGuilds)) {
				try {
					var json = JSON.DEFAULT.read(paths.relatedGuilds).readArray();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		return relatedGuilds;
	}

	public void logCommand(long user, long channel, long message, String command, String fullCommand) {
		var doc = new Document();
		doc.put("user", user);
		doc.put("channel", channel);
		doc.put("message", message);
		doc.put("command", command);
		doc.put("full_command", fullCommand);
		commandLog.insert(doc);
	}
}