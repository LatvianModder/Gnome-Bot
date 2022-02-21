package dev.gnomebot.app.data;

import com.google.gson.JsonObject;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.GuildPaths;
import dev.gnomebot.app.data.config.BaseConfig;
import dev.gnomebot.app.data.config.BooleanConfig;
import dev.gnomebot.app.data.config.ChannelConfig;
import dev.gnomebot.app.data.config.DBConfig;
import dev.gnomebot.app.data.config.IntConfig;
import dev.gnomebot.app.data.config.MemberConfig;
import dev.gnomebot.app.data.config.RoleConfig;
import dev.gnomebot.app.data.config.StringConfig;
import dev.gnomebot.app.data.config.StringListConfig;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.MessageFilter;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.ChatCommandSuggestionEvent;
import dev.gnomebot.app.script.DiscordJS;
import dev.gnomebot.app.script.WrappedGuild;
import dev.gnomebot.app.script.WrappedId;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.Ansi;
import dev.gnomebot.app.util.ConfigFile;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.RecentUser;
import dev.gnomebot.app.util.UnmuteTask;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.CategorizableChannel;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.MemberData;
import discord4j.rest.util.Image;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author LatvianModder
 */
public class GuildCollections {
	public final Databases db;
	public final Snowflake guildId;
	public final WrappedId wrappedId;
	public final GuildPaths paths;
	public DiscordJS discordJS;

	public final WrappedCollection<DiscordMember> members;
	public final WrappedCollection<DiscordMessage> messages;
	public final WrappedCollection<DiscordMessage> editedMessages;
	public final WrappedCollection<DiscordFeedback> feedback;
	public final WrappedCollection<DiscordPoll> polls;
	public final WrappedCollection<DiscordMessageCount> messageCount;
	public final WrappedCollection<DiscordMessageXP> messageXp;
	public final WrappedCollection<ChannelInfo> channelInfo;
	public final WrappedCollection<GnomeAuditLogEntry> auditLog;
	public final WrappedCollection<Macro> macros;
	public final WrappedCollection<ScheduledTask> scheduledTasks;

	public final DBConfig config;
	public final StringConfig name;
	public final StringConfig iconUrl;
	public final MemberConfig ownerId;
	public final StringListConfig badWords;
	public final IntConfig regularMessages;
	public final IntConfig regularXP;
	public final RoleConfig regularRole;
	public final RoleConfig adminRole;
	public final RoleConfig mutedRole;
	public final RoleConfig feedbackSuggestRole;
	public final RoleConfig feedbackVoteRole;
	public final RoleConfig reportMentionRole;
	public final ChannelConfig feedbackChannel;
	public final ChannelConfig adminLogChannel;
	public final ChannelConfig adminMessagesChannel;
	public final ChannelConfig muteAppealChannel;
	public final ChannelConfig logNewAccountsChannel;
	public final ChannelConfig logLeavingChannel;
	public final ChannelConfig reportChannel;
	public final StringConfig prefix;
	public final StringConfig macroPrefix;
	public final StringConfig inviteCode;
	public List<MessageFilter> messageFilters = new ArrayList<>(); // TODO: Implement
	public final BooleanConfig lockdownMode;
	public final IntConfig kickNewAccounts;
	public final StringConfig lockdownModeText;
	public final BooleanConfig anonymousFeedback;
	public final BooleanConfig adminsBypassAnonFeedback;
	public final StringConfig font;
	public final BooleanConfig forcePingableName;
	public final IntConfig autoMuteEveryone;
	public final IntConfig autoMuteUrlShortener;
	public final IntConfig autoMuteScamUrl;
	public final IntConfig feedbackNumber;
	public final IntConfig pollNumber;
	public final BooleanConfig autoPaste;
	public final StringConfig reportOptions;
	public final BooleanConfig autoMuteEmbed;

	public Pattern badWordRegex;
	private WrappedGuild wrappedGuild;
	private Map<Snowflake, ChannelInfo> channelMap;
	private List<ChannelInfo> channelList;
	private Map<Snowflake, CachedRole> roleMap;
	private List<CachedRole> roleList;
	public final Map<Snowflake, UnmuteTask> unmuteMap;
	public final List<RecentUser> recentUsers;
	private List<ChatCommandSuggestion> recentUserSuggestions;
	private Map<String, Macro> macroMap;

	public boolean advancedLogging = false;

	public GuildCollections(Databases d, Snowflake g) {
		db = d;
		guildId = g;
		wrappedId = new WrappedId(guildId);
		paths = AppPaths.getGuildPaths(guildId);

		String dbid = guildId.asString();

		members = create("members_" + dbid, DiscordMember::new);
		// TODO: Move messages to edited when channel is deleted
		messages = create("messages_" + dbid, DiscordMessage::new);
		editedMessages = create("edited_messages_" + dbid, DiscordMessage::new).expiresAfterMonth("timestamp_expire_" + dbid, "timestamp"); // GDPR
		feedback = create("feedback_" + dbid, DiscordFeedback::new);
		polls = create("polls_" + dbid, DiscordPoll::new);
		messageCount = create("message_count_" + dbid, DiscordMessageCount::new);
		messageXp = create("message_xp_" + dbid, DiscordMessageXP::new);
		channelInfo = create("channel_settings_" + dbid, (c, doc) -> new ChannelInfo(this, c, doc, null));
		auditLog = create("audit_log_" + dbid, GnomeAuditLogEntry::new).expiresAfterMonth("timestamp_expire_" + dbid, "expires"); // GDPR
		macros = create("macros_" + dbid, (c, doc) -> new Macro(this, c, doc));
		scheduledTasks = create("scheduled_tasks_" + dbid, (c, doc) -> new ScheduledTask(this, c, doc));

		config = new DBConfig();

		name = config.add(new StringConfig(this, "name", guildId.asString())).internal();
		iconUrl = config.add(new StringConfig(this, "icon_url", "")).internal();
		ownerId = config.add(new MemberConfig(this, "owner_id")).internal();
		badWords = config.add(new StringListConfig(this, "bad_words", Arrays.asList("reallybadword", "reallybadword2"))).title("Bad Words (separated by ' | ')");
		regularMessages = config.add(new IntConfig(this, "regular_messages", 0)).title("Regular Messages");
		regularXP = config.add(new IntConfig(this, "regular_xp", 3000)).title("Regular XP");
		regularRole = config.add(new RoleConfig(this, "regular_role")).title("Regular Role");
		adminRole = config.add(new RoleConfig(this, "admin_role")).title("Admin Role");
		mutedRole = config.add(new RoleConfig(this, "muted_role")).title("Muted Role");
		feedbackSuggestRole = config.add(new RoleConfig(this, "feedback_suggest_role")).title("Feedback Role for suggest command");
		feedbackVoteRole = config.add(new RoleConfig(this, "feedback_vote_role")).title("Feedback Role for voting");
		reportMentionRole = config.add(new RoleConfig(this, "report_mention_role")).title("Message Report mention role");
		feedbackChannel = config.add(new ChannelConfig(this, "feedback_channel")).title("Feedback Channel");
		adminLogChannel = config.add(new ChannelConfig(this, "admin_log_channel")).title("Admin Log Channel");
		adminMessagesChannel = config.add(new ChannelConfig(this, "admin_messages_channel")).title("Admin Messages Channel");
		muteAppealChannel = config.add(new ChannelConfig(this, "mute_appeal_channel")).title("Mute Appeal Channel");
		logNewAccountsChannel = config.add(new ChannelConfig(this, "log_new_accounts")).title("Log New Accounts Channel");
		logLeavingChannel = config.add(new ChannelConfig(this, "log_leaving")).title("Log Leaving Channel");
		reportChannel = config.add(new ChannelConfig(this, "report_channel")).title("Report Channel");
		prefix = config.add(new StringConfig(this, "prefix", "!")).title("Command Prefix");
		macroPrefix = config.add(new StringConfig(this, "custom_command_prefix", "!")).title("Macro Prefix");
		inviteCode = config.add(new StringConfig(this, "invite_code", "")).title("Invite Code");
		//settings.add("message_filters", () -> messageFilters, v -> messageFilters = v, messageFilters, (ListTransformer<Document, MessageFilter>) MessageFilter::new, (ListTransformer<MessageFilter, Document>) value -> value == null ? new Document() : value.toDocument());
		lockdownMode = config.add(new BooleanConfig(this, "lockdown_mode", false)).title("Lockdown Mode");
		kickNewAccounts = config.add(new IntConfig(this, "kick_new_accounts", 0)).title("Kick New Accounts (in seconds since account created, e.g 604800 == 1 week)");
		lockdownModeText = config.add(new StringConfig(this, "lockdown_mode_text", "Sorry! The server is currently getting attacked by spammers and is in lockdown mode! If you are not a spammer, please return later! If you are a spammer, eat the most rotten and sour lemon to ever exist.")).title("Lockdown Mode Text");
		anonymousFeedback = config.add(new BooleanConfig(this, "anonymous_feedback", false)).title("Anonymous Feedback");
		adminsBypassAnonFeedback = config.add(new BooleanConfig(this, "anonymous_feedback_admin_bypass", true)).title("Admins Bypass Anonymous Feedback");
		font = config.add(new StringConfig(this, "font", "DejaVu Sans Light")).title("Font");
		forcePingableName = config.add(new BooleanConfig(this, "force_pingable_name", false)).title("Force Pingable Name");
		autoMuteEveryone = config.add(new IntConfig(this, "automute_everyone", 0, 0, 43800)).title("Auto-mute on @everyone mention (minutes)");
		autoMuteUrlShortener = config.add(new IntConfig(this, "automute_url_shortener", 0, 0, 43800)).title("Auto-mute url shortener link (minutes)");
		autoMuteScamUrl = config.add(new IntConfig(this, "automute_scam_url", 30, 0, 43800)).title("Auto-mute potential scam link (minutes)");
		feedbackNumber = config.add(new IntConfig(this, "feedback_number", 0)).internal();
		pollNumber = config.add(new IntConfig(this, "poll_number", 0)).internal();
		autoPaste = config.add(new BooleanConfig(this, "auto_paste", true)).title("Auto-paste text files");
		reportOptions = config.add(new StringConfig(this, "report_options", "Scam | Spam | NSFW | Hacks")).title("Report Options (separated by ' | ')");
		autoMuteEmbed = config.add(new BooleanConfig(this, "auto_mute_embed", true)).title("Post info embed about auto-muted users");

		unmuteMap = new HashMap<>();
		recentUsers = new ArrayList<>();

		Document settingsDoc = db.guildData.query(guildId.asLong()).firstDocument();

		if (settingsDoc == null || settingsDoc.isEmpty()) {
			settingsDoc = config.write();
			settingsDoc.put("_id", guildId.asLong());
			db.guildData.insert(settingsDoc);
		}

		if (config.read(dbid, settingsDoc)) {
			List<Bson> updates = new ArrayList<>();

			for (BaseConfig<?> setting : config.map.values()) {
				updates.add(Updates.set(setting.id, setting.toDB()));
			}

			db.guildData.query(guildId.asLong()).update(updates);
		}

		readSettings();

		ConfigFile file = new ConfigFile(paths.config);

		for (BaseConfig<?> setting : config.map.values()) {
			file.get(setting.id, setting.toJson());
		}

		// TODO: Implement this?
		file.needsSaving();
		file.save();

		discordJS = new DiscordJS(this, false);
	}

	public <T extends WrappedDocument<T>> WrappedCollection<T> create(String ci, BiFunction<WrappedCollection<T>, MapWrapper, T> w) {
		return db.create(ci, w).gc(this);
	}

	public Guild getGuild() {
		return Objects.requireNonNull(db.app.discordHandler.client.getGuildById(guildId).block());
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

	@Override
	public String toString() {
		return name.get();
	}

	public boolean isMM() {
		return guildId.asLong() == 166630061217153024L;
	}

	public GatewayDiscordClient getClient() {
		return db.app.discordHandler.client;
	}

	public void readSettings() {
		// App.info("Loading settings for " + this + "...");

		//Table settingsTable = new Table("Setting", "Value");
		//
		//for (DBSetting<?, ?> setting : settings.map.values())
		//{
		//	settingsTable.addRow(setting.key, setting);
		//}
		//
		//settingsTable.print();

		List<String> badWords1 = badWords.get().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();

		if (!badWords1.isEmpty()) {
			StringBuilder sb = new StringBuilder("\\b(?:");

			for (int i = 0; i < badWords1.size(); i++) {
				if (i > 0) {
					sb.append("|");
				}

				sb.append("(?:");

				String w = badWords1.get(i);

				for (int j = 0; j < w.length(); j++) {
					char c = Character.toLowerCase(w.charAt(j));

					if (j > 0) {
						sb.append("[\\s\\W]*");
					}

					MessageFilter.alias(sb, c);
				}

				sb.append(')');
			}

			sb.append(')');

			badWordRegex = Pattern.compile(sb.toString(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		} else {
			badWordRegex = null;
		}

		for (MessageFilter filter : messageFilters) {
			filter.compile();
		}

		updateMacroMap();
	}

	public String getClickableName() {
		if (inviteCode.get().isEmpty()) {
			return name.get();
		}

		return "[" + name.get() + "](https://discord.gg/" + inviteCode + ")";
	}

	public Optional<Message> findMessage(@Nullable Snowflake id, @Nullable ChannelInfo priority) {
		if (id == null) {
			return Optional.empty();
		}

		List<ChannelInfo> c = getChannelList();

		if (priority != null) {
			c.remove(priority);
			c.add(0, priority);
		}

		for (ChannelInfo channel : c) {
			Message m = channel.getMessage(id);

			if (m != null) {
				return Optional.of(m);
			}
		}

		return Optional.empty();
	}

	public void adminLogChannelEmbed(Consumer<EmbedBuilder> embed) {
		adminLogChannel.messageChannel().ifPresent(c -> {
					EmbedBuilder builder = EmbedBuilder.create();
					builder.color(EmbedColors.RED);
					builder.timestamp(Instant.now());
					embed.accept(builder);
					c.createMessage(builder).subscribe();
				}
		);
	}

	public long getUserID(String tag) {
		Member member = getGuild().getMembers().cache().filter(m1 -> m1.getTag().equals(tag)).blockFirst();
		return member == null ? 0L : member.getId().asLong();
	}

	public void unmute(Snowflake user, long seconds) {
		UnmuteTask task = unmuteMap.get(user);

		if (task != null) {
			task.cancelled = true;
		}

		task = new UnmuteTask(this, user, Math.max(0L, seconds));

		if (seconds > 0L) {
			unmuteMap.put(user, task);
			db.app.queueScheduledTask(System.currentTimeMillis() + task.seconds * 1000L, task);
		} else {
			task.unmute();
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

	public JsonObject getChannelJson(Snowflake channel) {
		JsonObject json = new JsonObject();
		json.addProperty("id", channel.asString());
		json.addProperty("name", getChannelName(channel));
		return json;
	}

	public void save(String key) {
		BaseConfig<?> c = config.map.get(key);

		if (c != null) {
			c.save();
		}
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
		macroMap = null;
	}

	public void guildUpdated(@Nullable Guild g) {
		App.LOGGER.refreshedGuildCache();
		refreshCache();

		if (g != null) {
			String n = g.getName();

			if (!name.get().equals(n)) {
				name.set(n);
				name.save();
			}

			String i = g.getIconUrl(Image.Format.PNG).orElse("");

			if (!iconUrl.get().equals(i)) {
				iconUrl.set(i);
				iconUrl.save();
			}

			Snowflake o = g.getOwnerId();

			if (!ownerId.get().equals(o)) {
				ownerId.set(o);
				ownerId.save();
			}
		}
	}

	public void channelUpdated(@Nullable CategorizableChannel old, TopLevelGuildMessageChannel channel, boolean deleted) {
		App.LOGGER.refreshedChannelCache();
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
				ci.updateFrom(channel);
			} else {
				App.error("Unknown channel " + channel.getId().asString() + "/" + channel.getName() + " updated!");
			}
		}
	}

	public void roleUpdated(Snowflake roleId, boolean deleted) {
		App.LOGGER.refreshedRoleCache();
		refreshCache();
	}

	// 0 update | 1 join | 2 leave
	public void memberUpdated(Snowflake userId, int type) {
		if (type == 0) {
			// App.LOGGER.refreshedMemberCache();
		}
		// refreshCache();
	}

	public void refreshChannelCache() {
		channelMap = null;
		channelList = null;
	}

	public Map<Snowflake, ChannelInfo> getChannelMap() {
		Map<Snowflake, ChannelInfo> ci = channelMap;

		if (ci == null) {
			ci = new LinkedHashMap<>();

			try {
				for (TopLevelGuildMessageChannel ch : getGuild().getChannels()
						.filter(c -> c instanceof TopLevelGuildMessageChannel)
						.cast(TopLevelGuildMessageChannel.class)
						.sort(Comparator.comparing(TopLevelGuildMessageChannel::getRawPosition).thenComparing(TopLevelGuildMessageChannel::getId))
						.toIterable()) {
					ChannelInfo c = channelInfo.findFirst(ch.getId());

					if (c == null) {
						Document document = new Document();
						document.put("_id", ch.getId().asLong());
						channelInfo.insert(document);
						c = new ChannelInfo(this, channelInfo, MapWrapper.wrap(document), ch.getId());
					}

					c.updateFrom(ch);
					ci.put(ch.getId(), c);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			channelMap = ci;
		}

		return ci;
	}

	public List<ChannelInfo> getChannelList() {
		if (channelList == null) {
			channelList = new ArrayList<>(getChannelMap().values());
		}

		return channelList;
	}

	public Map<Snowflake, CachedRole> getRoleMap() {
		if (roleMap == null) {
			roleMap = new LinkedHashMap<>();

			try {
				getGuild().getRoles()
						.filter(r -> !r.isEveryone())
						.sort(Comparator.comparing(Role::getRawPosition).thenComparing(Role::getId).reversed())
						.toStream()
						.forEach(r -> roleMap.put(r.getId(), new CachedRole(this, r)));
			} catch (Exception ex) {
			}

			roleList = new ArrayList<>(roleMap.values());

			CachedRole adminRoleW = roleMap.get(adminRole.get());
			int i = adminRoleW == null ? -1 : roleList.indexOf(adminRoleW);

			if (i != -1) {
				for (int j = 0; j <= i; j++) {
					roleList.get(j).adminRole = true;
				}
			}
		}

		return roleMap;
	}

	public List<CachedRole> getRoleList() {
		getRoleMap();
		return roleList;
	}

	public void auditLog(GnomeAuditLogEntry.Builder builder) {
		try {
			auditLog.insert(builder.build());
		} catch (Exception ex) {
			App.warn("Failed to write to audit log [" + builder.type.name + "]: " + Ansi.DARK_RED + ex);
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

	public ChannelInfo getOrMakeChannelInfo(Snowflake id) {
		ChannelInfo ci = getChannelMap().get(id);
		return ci == null ? new ChannelInfo(this, channelInfo, MapWrapper.EMPTY, id) : ci;
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
			macroMap = new HashMap<>();

			for (Macro macro : macros.query()) {
				macroMap.put(macro.getName().toLowerCase(), macro);
			}
		}

		return macroMap;
	}

	@Nullable
	public Macro getMacro(String name) {
		return getMacroMap().get(name.toLowerCase());
	}

	public void updateMacroMap() {
		macroMap = null;
	}
}