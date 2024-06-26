package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionBuilder;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.discord.command.FeedbackCommand;
import dev.gnomebot.app.discord.command.ScamsCommands;
import dev.gnomebot.app.discord.command.VerifyMinecraftCommand;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.ArrayList;

public class GnomeAdminCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("gnome-admin")
			.defaultMemberPermissions(PermissionSet.of(Permission.MODERATE_MEMBERS))
			.add(subGroup("settings")
					.description("Configure Gnome bot")
					.add(sub("set")
							.description("Change a setting")
							.add(string("key").required().suggest(SettingsCommands::suggestKey))
							.add(string("value").required().suggest(SettingsCommands::suggestValue))
							.run(SettingsCommands::set)
					)
					.add(sub("get")
							.description("Print a setting value")
							.add(string("key").required().suggest(SettingsCommands::suggestKey))
							.run(SettingsCommands::get)
					)
					.add(sub("logout-everyone")
							.description("Log everyone out of the panel (Invalidates everyone's tokens)")
							.run(GnomeAdminCommand::logoutEveryone)
					)
					.add(sub("fix-broken-commands")
							.description("Fix broken commands")
							.run(GnomeAdminCommand::fixBrokenCommands)
					)
			)
			.add(sub("echo")
					.description("Sends message back as bot")
					.add(string("message").required())
					.run(EchoCommand::run)
			)
			.add(sub("ban")
					.description("Bans a member")
					.add(user("user").required())
					.add(string("reason"))
					.add(bool("delete-messages").description("Deletes Messages"))
					.run(BanCommand::run)
			)
			.add(sub("unban")
					.description("Unbans a member")
					.add(user("user").required())
					.run(UnbanCommand::run)
			)
			.add(sub("kick")
					.description("Kicks a member")
					.add(user("user").required())
					.add(string("reason"))
					.run(KickCommand::run)
			)
			.add(sub("regex-kick")
					.description("Kicks multiple members at once based on their username")
					.add(string("regex").required())
					.add(string("reason"))
					.run(RegexKickCommand::run)
			)
			.add(sub("mute")
					.description("Mutes a member")
					.add(user("user").required())
					.add(string("reason"))
					.add(time("time", false, true))
					.run(MuteCommand::run)
			)
			.add(sub("unmute")
					.description("Unmutes a member")
					.add(user("user").required())
					.run(UnmuteCommand::run)
			)
			.add(sub("mutes")
					.description("Prints all mutes")
					.add(user("user"))
					.run(MutesCommand::run)
			)
			.add(sub("warn")
					.description("Warns a member")
					.add(user("user").required())
					.add(string("reason"))
					.run(WarnCommand::run)
			)
			.add(sub("audit-log")
					.description("Audit log of a member")
					.add(user("user").required())
					.run(UserAuditLogCommand::run)
			)
			.add(sub("note")
					.description("Adds note to member")
					.add(user("user").required())
					.add(string("note").required())
					.run(NoteCommand::run)
			)
			.add(subGroup("lockdown")
					.description("Lockdown controls")
					.add(sub("enable")
							.description("Enables lockdown mode")
							.add(time("kick-time", false, true))
							.run(LockdownCommand::enable)
					)
					.add(sub("disable")
							.description("Disables lockdown mode")
							.run(LockdownCommand::disable)
					)
			)
			.add(subGroup("display")
					.description("Displays messages, users, etc.")
					.add(sub("members")
							.add(string("name-regex"))
							.add(role("role"))
							.run(DisplayCommands::members)
					)
					.add(sub("messages")
							.add(string("content-regex"))
							.add(user("member"))
							.add(integer("flags"))
							.add(bool("recently-deleted"))
							.add(bool("activity"))
							.run(DisplayCommands::messages)
					)
					.add(sub("quiet-member-count")
							.run(DisplayCommands::quietMemberCount)
					)
					.add(sub("message-history-export")
							.add(user("member"))
							.run(DisplayCommands::messageHistoryExport)
					)
					.add(sub("message-count-per-month")
							.add(channel("channel"))
							.run(DisplayCommands::messageCountPerMonth)
					)
					.add(sub("admin-roles")
							.run(DisplayCommands::adminRoles)
					)
					.add(sub("hourly-activity")
							.description("Display hourly activity of a member")
							.add(user("member"))
							.add(integer("days"))
							.add(zone("timezone"))
							.run(DisplayCommands::hourlyActivity)
					)
					.add(sub("member-count")
							.description("Displays member count")
							.add(role("role"))
							.run(DisplayCommands::memberCount)
					)
					.add(sub("user-mention-leaderboard")
							.description("User Mention Leaderboard")
							.add(user("mention").required())
							.add(time("timespan", true, false))
							.add(integer("limit"))
							.add(channel("channel"))
							.run(DisplayCommands::userMentionLeaderboard)
					)
					.add(sub("role-mention-leaderboard")
							.description("Role Mention Leaderboard")
							.add(role("mention").required())
							.add(time("timespan", true, false))
							.add(integer("limit"))
							.add(channel("channel"))
							.run(DisplayCommands::roleMentionLeaderboard)
					)
			)
			.add(subGroup("feedback")
					.description("Feedback")
					.add(sub("approve")
							.description("Approves feedback")
							.add(string("reason"))
							.add(integer("id"))
							.run(FeedbackCommand::approve)
					)
					.add(sub("deny")
							.description("Denies feedback")
							.add(string("reason"))
							.add(integer("id"))
							.run(FeedbackCommand::deny)
					)
					.add(sub("consider")
							.description("Considers feedback")
							.add(string("reason"))
							.add(integer("id"))
							.run(FeedbackCommand::consider)
					)
					.add(sub("cleanup")
							.description("Removes all approved and denied suggestions")
							.run(FeedbackCommand::cleanup)
					)
			)
			.add(subGroup("scam-domains")
					.description("Manage scam URL domains")
					.add(sub("fetch")
							.description("Fetches list from SinkingYachts")
							.run(ScamsCommands::fetchDomains)
					)
					.add(sub("check")
							.description("Checks if domain is blocked or allowed")
							.add(string("domain").required())
							.run(ScamsCommands::checkDomain)
					)
					.add(sub("block")
							.description("Blocks domain (For scam URLs)")
							.add(string("domain").required())
							.run(ScamsCommands::blockDomain)
					)
					.add(sub("allow")
							.description("Allows domain (For false positives)")
							.add(string("domain").required())
							.run(ScamsCommands::allowDomain)
					)
					.add(sub("remove")
							.description("Removes domain from being either blocked or allowed")
							.add(string("domain").required())
							.run(ScamsCommands::removeDomain)
					)
			)
			.add(sub("test-scam")
					.description("Tests and prints back if input contains any scam URLs")
					.add(string("text").required())
					.run(ScamsCommands::test)
			)
			.add(subGroup("verify")
					.add(sub("minecraft")
							.description("Verify Minecraft")
							.add(realUser("user").required())
							.run(VerifyMinecraftCommand::run)
					)
			)
			.add(subGroup("delete-messages")
					.add(sub("between")
							.add(string("message-1").required())
							.add(string("message-2").required())
							.add(mentionable("from"))
					)
			)
			// END
			;

	public static void logoutEveryone(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkSenderTrusted();
		App.instance.db.invalidateAllTokens();
		event.respond("Everyone's Gnome Panel login tokens have been invalidated!");
	}

	public static void fixBrokenCommands(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		var broken = new ArrayList<ApplicationCommandData>();

		for (var command : event.context.gc.db.app.discordHandler.client.getRestClient().getApplicationService().getGuildApplicationCommands(event.context.gc.db.app.discordHandler.selfId, event.context.gc.guildId).toIterable()) {
			var macro = event.context.gc.getMacro(command.name());

			if (macro == null || macro.slashCommand != command.id().asLong()) {
				broken.add(command);
			}
		}

		event.respond("Found " + broken.size() + " broken commands: " + broken.stream().map(c -> "</" + c.name() + ":" + c.id().asString() + ">").toList());

		for (var command : broken) {
			event.context.gc.db.app.discordHandler.client.getRestClient().getApplicationService().deleteGuildApplicationCommand(event.context.gc.db.app.discordHandler.selfId, event.context.gc.guildId, command.id().asLong()).subscribe();
		}
	}
}
