package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.ComponentCallback;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.rest.util.Permission;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RegexKickCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkGlobalPerms(Permission.KICK_MEMBERS);

		String s = event.get("regex").asString();

		if (s.isEmpty()) {
			throw error("Invalid regex!");
		}

		Pattern pattern = FormattingUtils.parseSafeRegEx(s, 0);

		if (pattern == null) {
			throw error("Invalid regex!");
		}

		String reason = event.get("reason").asString("scam");
		List<Member> members = new ArrayList<>();

		for (Member member : event.context.gc.getMembers()) {
			if (pattern.matcher(member.getUsername()).matches()) {
				members.add(member);
			}
		}

		Set<String> actionsToRemove = new HashSet<>();

		String idKick = ComponentCallback.id(event1 -> {
			event1.context.checkGlobalPerms(Permission.KICK_MEMBERS);
			event1.edit().respond("**Kicked " + members.size() + " members**");

			for (Member member : members) {
				member.kick(reason).subscribe();

				event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.BAN)
						.user(member)
						.source(event.context.sender)
						.content(reason)
				);
			}

			return actionsToRemove;
		});

		String idBan = ComponentCallback.id(event1 -> {
			event1.context.checkGlobalPerms(Permission.BAN_MEMBERS);
			event1.edit().respond("**Banned " + members.size() + " members**");

			for (var member : members) {
				member.ban(BanQuerySpec.builder().reason(reason).deleteMessageDays(1).build()).subscribe();

				event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.BAN)
						.user(member)
						.source(event.context.sender)
						.content(reason)
						.flags(GnomeAuditLogEntry.Flags.DM)
				);
			}

			return actionsToRemove;
		});

		actionsToRemove.add(idKick);
		actionsToRemove.add(idBan);

		event.respond(MessageBuilder.create("Matched " + members.size() + " members:")
				.addFile("kicked_members.txt", members.stream()
						.map(m -> m.getId().asString() + " " + m.getTag())
						.collect(Collectors.joining("\n"))
						.getBytes(StandardCharsets.UTF_8)
				)
				.addComponentRow(Button.secondary(idKick, "Kick"), Button.danger(idBan, "Ban"))
		);
	}
}
