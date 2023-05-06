package dev.gnomebot.app;

import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.AnsiComponent;

public interface BrainEvents {
	AnsiComponent MESSAGE_CREATED_NO_ROLE = Ansi.lightGray("■");
	AnsiComponent MESSAGE_CREATED_ANY_ROLE = Ansi.yellow("■");
	AnsiComponent UNKNOWN_MESSAGE = Ansi.teal("■");
	AnsiComponent MESSAGE_CREATED_ADMIN = Ansi.purple("■");
	AnsiComponent MESSAGE_CREATED_BOT = Ansi.green("■");
	AnsiComponent MESSAGE_EDITED = Ansi.orange("■");
	AnsiComponent MESSAGE_DELETED = Ansi.red("■");
	AnsiComponent SUSPICIOUS_MESSAGE = Ansi.darkRed("■");
	AnsiComponent COMMAND_SUCCESS = Ansi.blue("◆");
	AnsiComponent COMMAND_FAIL = Ansi.red("◆");
	AnsiComponent REACTION_ADDED = Ansi.green("\uD83D\uDDF8"); // 🗸
	AnsiComponent REACTION_REMOVED = Ansi.red("\uD83D\uDDF8"); // 🗸
	AnsiComponent VOICE_JOINED = Ansi.green("♪");
	AnsiComponent VOICE_LEFT = Ansi.red("♪");
	AnsiComponent VOICE_CHANGED = Ansi.yellow("♪");
	AnsiComponent REFRESHED_GUILD_CACHE = Ansi.lightGray("\uD83D\uDE7E"); // 🙾
	AnsiComponent REFRESHED_CHANNEL_CACHE = Ansi.magenta("\uD83D\uDE7E"); // 🙾
	AnsiComponent REFRESHED_PINGS = Ansi.green("\uD83D\uDE7E"); // 🙾
	AnsiComponent REFRESHED_ROLE_CACHE = Ansi.yellow("\uD83D\uDE7E"); // 🙾
	AnsiComponent MEMBER_JOINED = Ansi.blue("⬤");
	AnsiComponent MEMBER_LEFT = Ansi.red("⬤");
	AnsiComponent MEMBER_MUTED = Ansi.red("☠");
	AnsiComponent MEMBER_BANNED = Ansi.darkRed("☠");
	AnsiComponent WEB_REQUEST = Ansi.cyan("◆");
	AnsiComponent PRESENCE_UPDATED = Ansi.lightGray("◆");
	AnsiComponent AUDIT_LOG = Ansi.yellow("◆");
}
