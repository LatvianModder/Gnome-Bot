package dev.gnomebot.app.data.config;

import org.jetbrains.annotations.Nullable;

import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface GuildConfig {
	Map<String, ConfigKey<?, ?>> MAP = new LinkedHashMap<>();

	static <T, H extends ConfigHolder<T>> ConfigKey<T, H> key(ConfigType<T, H> type, String id, String title, T defaultValue) {
		var key = new ConfigKey<>(type, id, title, defaultValue);
		MAP.put(id, key);
		return key;
	}

	static <T, H extends ConfigHolder<T>> ConfigKey<T, H> key(ConfigType<T, H> type, String id, String title) {
		return key(type, id, title, type.defaultKeyValue());
	}

	ConfigKey<Integer, ConfigHolder<Integer>> GLOBAL_XP = key(IntConfigType.POSITIVE, "global_xp", "Global XP");
	ConfigKey<Integer, ConfigHolder<Integer>> REGULAR_MESSAGES = key(IntConfigType.POSITIVE, "regular_messages", "Regular Messages");
	ConfigKey<Integer, ConfigHolder<Integer>> REGULAR_XP = key(IntConfigType.POSITIVE, "regular_xp", "Regular XP");
	ConfigKey<Long, RoleConfigType.Holder> REGULAR_ROLE = key(RoleConfigType.DEFAULT, "regular_role", "Regular Role");
	ConfigKey<Long, RoleConfigType.Holder> ADMIN_ROLE = key(RoleConfigType.DEFAULT, "admin_role", "Admin Role");
	ConfigKey<Long, RoleConfigType.Holder> MUTED_ROLE = key(RoleConfigType.DEFAULT, "muted_role", "Muted Role");
	ConfigKey<Long, RoleConfigType.Holder> FEEDBACK_SUGGEST_ROLE = key(RoleConfigType.DEFAULT, "feedback_suggest_role", "Feedback Role for suggest command");
	ConfigKey<Long, RoleConfigType.Holder> FEEDBACK_VOTE_ROLE = key(RoleConfigType.DEFAULT, "feedback_vote_role", "Feedback Role for voting");
	ConfigKey<Long, RoleConfigType.Holder> REPORT_MENTION_ROLE = key(RoleConfigType.DEFAULT, "report_mention_role", "Message Report mention role");
	ConfigKey<Long, ChannelConfigType.Holder> FEEDBACK_CHANNEL = key(ChannelConfigType.DEFAULT, "feedback_channel", "Feedback Channel");
	ConfigKey<Long, ChannelConfigType.Holder> ADMIN_LOG_CHANNEL = key(ChannelConfigType.DEFAULT, "admin_log_channel", "Admin Log Channel");
	ConfigKey<Long, ChannelConfigType.Holder> ADMIN_MESSAGES_CHANNEL = key(ChannelConfigType.DEFAULT, "admin_messages_channel", "Admin Messages Channel");
	ConfigKey<Long, ChannelConfigType.Holder> MUTE_APPEAL_CHANNEL = key(ChannelConfigType.DEFAULT, "mute_appeal_channel", "Mute Appeal Channel");
	ConfigKey<Long, ChannelConfigType.Holder> LOG_NEW_ACCOUNTS_CHANNEL = key(ChannelConfigType.DEFAULT, "log_new_accounts_channel", "Log New Accounts Channel");
	ConfigKey<Long, ChannelConfigType.Holder> LOG_LEAVING_CHANNEL = key(ChannelConfigType.DEFAULT, "log_leaving_channel", "Log Leaving Channel");
	ConfigKey<Long, ChannelConfigType.Holder> REPORT_CHANNEL = key(ChannelConfigType.DEFAULT, "report_channel", "Report Channel");
	ConfigKey<Long, ChannelConfigType.Holder> LOG_IP_ADDRESSES_CHANNEL = key(ChannelConfigType.DEFAULT, "log_ip_addresses_channel", "log IP Addresses Channel");
	ConfigKey<Long, ChannelConfigType.Holder> APPEAL_CHANNEL = key(ChannelConfigType.DEFAULT, "appeal_channel", "Appeal Channel");
	ConfigKey<String, ConfigHolder<String>> LEGACY_PREFIX = key(StringConfigType.DEFAULT, "legacy_prefix", "Legacy Command Prefix", "!");
	ConfigKey<String, ConfigHolder<String>> MACRO_PREFIX = key(StringConfigType.DEFAULT, "macro_prefix", "Macro Prefix", "??");
	ConfigKey<String, ConfigHolder<String>> INVITE_CODE = key(StringConfigType.DEFAULT, "invite_code", "Invite Code");
	ConfigKey<Boolean, ConfigHolder<Boolean>> LOCKDOWN_MODE = key(BooleanConfigType.DEFAULT, "lockdown_mode", "Lockdown Mode");
	ConfigKey<Integer, ConfigHolder<Integer>> KICK_NEW_ACCOUNTS = key(IntConfigType.POSITIVE, "kick_new_accounts", "Kick New Accounts");
	ConfigKey<Boolean, ConfigHolder<Boolean>> ANONYMOUS_FEEDBACK = key(BooleanConfigType.DEFAULT, "anonymous_feedback", "Anonymous Feedback");
	ConfigKey<Boolean, ConfigHolder<Boolean>> ADMINS_BYPASS_ANON_FEEDBACK = key(BooleanConfigType.DEFAULT, "admins_bypass_anon_feedback", "Admins Bypass Anonymous Feedback", true);
	ConfigKey<Font, FontConfigType.Holder> FONT = key(FontConfigType.DEFAULT, "font", "Font");
	ConfigKey<Integer, ConfigHolder<Integer>> AUTO_MUTE_URL_SHORTENER = key(new IntConfigType(0, 43800), "auto_mute_url_shortener", "Auto-mute URL Shortener");
	ConfigKey<Integer, ConfigHolder<Integer>> AUTO_MUTE_SCAM_URL = key(new IntConfigType(0, 43800), "auto_mute_scam_url", "Auto-mute Scam URL");
	ConfigKey<Boolean, ConfigHolder<Boolean>> AUTO_PASTE = key(BooleanConfigType.DEFAULT, "auto_paste", "Auto-paste", true);
	ConfigKey<List<String>, ConfigHolder<List<String>>> REPORT_OPTIONS = key(StringConfigType.DEFAULT.asList(), "report_options", "Report Options"); // "Scam", "Spam", "NSFW", "Hacks"
	ConfigKey<Boolean, ConfigHolder<Boolean>> AUTO_MUTE_EMBED = key(BooleanConfigType.DEFAULT, "auto_mute_embed", "Auto-mute Embed", true);

	@Nullable
	static ConfigKey<?, ?> get(String id) {
		return switch (id) {
			case "log_new_accounts" -> LOG_NEW_ACCOUNTS_CHANNEL;
			case "log_leaving" -> LOG_LEAVING_CHANNEL;
			case "prefix" -> LEGACY_PREFIX;
			case "custom_command_prefix" -> MACRO_PREFIX;
			case "anonymous_feedback_admin_bypass" -> ADMINS_BYPASS_ANON_FEEDBACK;
			case "automute_url_shortener" -> AUTO_MUTE_URL_SHORTENER;
			case "automute_scam_url" -> AUTO_MUTE_SCAM_URL;
			default -> MAP.get(id);
		};
	}
}
