package dev.gnomebot.app.discord.legacycommand;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.QuoteHandler;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.OptionalLong;

/**
 * @author LatvianModder
 */
public class MuteCommand {
	@LegacyDiscordCommand(name = "mute", help = "Mutes a member", arguments = "<member> [time]", permissionLevel = AuthLevel.ADMIN)
	public static final CommandCallback COMMAND = (context, reader) -> {
		if (!context.gc.mutedRole.isSet()) {
			throw new DiscordCommandException("Muted role not set!");
		}

		Member m = reader.readMember().orElseThrow(() -> new DiscordCommandException("Member not found!"));

		if (m.isBot() || context.gc.getAuthLevel(m).is(AuthLevel.ADMIN)) {
			context.reply("Nice try.");
			return;
		}

		long seconds0 = 1800L;
		int pos = reader.position;

		try {
			OptionalLong sec = reader.readSeconds();

			if (sec.isPresent()) {
				seconds0 = sec.getAsLong();
			} else {
				reader.position = pos;
			}
		} catch (Exception ex) {
			reader.position = pos;
		}

		mute(context, m, seconds0, reader.readRemainingString().orElse("Not specified"), "");
	};

	public static void mute(CommandContext context, Member m, long seconds, String reason, String auto) throws DiscordCommandException {
		DiscordMember discordMember = context.gc.members.findFirst(m);
		Date expires = new Date(System.currentTimeMillis() + seconds * 1000L);
		Instant expiresInstant = expires.toInstant();

		if (discordMember != null) {
			discordMember.update(Updates.set("muted", expires));
		}

		context.gc.unmute(m.getId(), seconds);

		Message contextMessage;

		if (context.gc.autoMuteEmbed.get()) {
			EmbedBuilder embedBuilder = EmbedBuilder.create();

			embedBuilder.color(EmbedColors.RED);

			if (!auto.isEmpty()) {
				embedBuilder.description(auto);
			} else if (context.sender.getId().equals(m.getId())) {
				embedBuilder.description(context.sender.getMention() + " was auto-muted!");
			} else {
				embedBuilder.description(context.sender.getMention() + " muted " + m.getMention() + "!");
			}

			embedBuilder.inlineField("Expires", Utils.formatRelativeDate(expiresInstant));
			embedBuilder.inlineField("Reason", reason);

			contextMessage = context.reply(embedBuilder);
		} else {
			contextMessage = null;
		}

		if (context.gc.mutedRole.is(m)) {
			return;
		} else {
			context.gc.mutedRole.add(m.getId(), "Muted");
		}

		List<LayoutComponent> adminButtons = new ArrayList<>();

		if (auto.isEmpty()) {
			if (contextMessage != null) {
				adminButtons.add(ActionRow.of(Button.link(QuoteHandler.getMessageURL(context.gc.guildId, context.channelInfo.id, contextMessage.getId()), "Context")));
			}
		} else {
			String id = m.getId().asString() + "/" + ComponentEventWrapper.encode(reason);
			adminButtons.add(ActionRow.of(SelectMenu.of("button",
					SelectMenu.Option.of("None", "none"),
					SelectMenu.Option.of("Ban", "ban/" + id).withEmoji(Emojis.NO_ENTRY),
					SelectMenu.Option.of("Kick", "kick/" + id).withEmoji(Emojis.BOOT),
					// SelectMenu.Option.of("Warn", "warn/" + id).withEmoji(Emojis.WARNING),
					SelectMenu.Option.of("Unmute", "unmute/" + id).withEmoji(Emojis.CHECKMARK)
			).withPlaceholder("Select Action").withMinValues(0).withMaxValues(1)));

			if (contextMessage != null) {
				adminButtons.add(ActionRow.of(Button.link(QuoteHandler.getMessageURL(context.gc.guildId, context.channelInfo.id, contextMessage.getId()), "Context")));
			}
		}

		EmbedBuilder embed = EmbedBuilder.create();
		embed.color(EmbedColors.RED);
		embed.author(m.getTag() + " has been muted!", null, m.getAvatarUrl());
		embed.description(context.sender.getMention() + " muted " + m.getMention());
		embed.inlineField("Reason", reason);
		embed.inlineField("Expires", Utils.formatRelativeDate(expiresInstant));

		if (!auto.isEmpty()) {
			embed.field("Message", context.message.getContent());
		}

		context.gc.adminLogChannel.messageChannel().map(c -> c.createMessage(MessageBuilder.create(embed).components(adminButtons)).subscribe(m1 -> {
			List<ActionComponent> replyButtons = new ArrayList<>();

			if (context.gc.muteAppealChannel.isSet()) {
				replyButtons.add(Button.link(QuoteHandler.getChannelURL(context.gc.guildId, context.gc.muteAppealChannel.get()), "Appeal"));
			}

			replyButtons.add(Button.link(QuoteHandler.getMessageURL(context.gc.guildId, context.gc.adminLogChannel.get(), m1.getId()), "Take Action"));

			if (contextMessage != null) {
				contextMessage.edit(MessageEditSpec.builder().componentsOrNull(List.of(ActionRow.of(replyButtons))).build()).subscribe();
			}
		}));

		List<ActionComponent> dmButtons = new ArrayList<>();

		if (contextMessage != null) {
			dmButtons.add(Button.link(QuoteHandler.getMessageURL(context.gc.guildId, context.channelInfo.id, contextMessage.getId()), "Context"));
		}

		if (context.gc.muteAppealChannel.isSet()) {
			dmButtons.add(Button.link(QuoteHandler.getChannelURL(context.gc.guildId, context.gc.muteAppealChannel.get()), "Appeal"));
		}

		boolean dm = DM.send(context.handler, m, MessageBuilder.create()
						.addEmbed(embed)
						.components(dmButtons.isEmpty() ? null : Collections.singletonList(ActionRow.of(dmButtons)))
				, true).isPresent();

		context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.MUTE)
				.user(m)
				.source(context.handler.selfId)
				.content(reason)
				.extra("dm", dm)
		);
	}
}
