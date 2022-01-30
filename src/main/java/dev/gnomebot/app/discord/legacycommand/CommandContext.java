package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.DiscordHandler;
import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.InteractionEventWrapper;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.MessageId;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class CommandContext {
	public DiscordHandler handler;
	public GuildCollections gc;
	public ChannelInfo channelInfo;
	public Message message;
	public Member sender;
	public InteractionEventWrapper<?> interaction = null;
	public boolean referenceMessage = true;
	public AllowedMentions allowedMentions = DiscordMessage.noMentions();

	@Override
	public String toString() {
		return "Context{" +
				"gc=" + gc +
				", channel=#" + (channelInfo == null ? "<null>" : channelInfo.getName()) +
				", message=" + (message == null ? "<null>" : message.getContent()) +
				", sender=" + sender.getTag() +
				'}';
	}

	public void checkBotPerms(@Nullable ChannelInfo channelInfo, Permission... perms) throws DiscordCommandException {
		if (channelInfo != null && channelInfo.getSelfPermissions().containsAll(PermissionSet.of(perms))) {
			return;
		}

		throw new DiscordCommandException(DiscordCommandException.Type.NO_PERMISSION, "I don't have permission to do that :(\nPermissions required: " + Arrays.stream(perms).map(Enum::name).collect(Collectors.joining(", "))).reaction(Emojis.RAGE).ephemeral();
	}

	public void checkBotPerms(Permission... perms) throws DiscordCommandException {
		checkBotPerms(channelInfo, perms);
	}

	public void checkSenderPerms(@Nullable ChannelInfo channelInfo, Permission... perms) throws DiscordCommandException {
		if (channelInfo != null && channelInfo.getPermissions(sender.getId()).containsAll(PermissionSet.of(perms))) {
			return;
		}

		throw new DiscordCommandException(DiscordCommandException.Type.NO_PERMISSION, "You don't have permission to do that :(\nPermissions required: " + Arrays.stream(perms).map(Enum::name).collect(Collectors.joining(", "))).reaction(Emojis.RAGE).ephemeral();
	}

	public void checkSenderPerms(Permission... perms) throws DiscordCommandException {
		checkSenderPerms(channelInfo, perms);
	}

	public void checkSenderTrusted() throws DiscordCommandException {
		if (!isTrusted()) {
			throw new DiscordCommandException(DiscordCommandException.Type.NO_PERMISSION, "Only bot owners can do this!");
		}
	}

	public void checkSenderOwner() throws DiscordCommandException {
		if (!isOwner()) {
			throw new DiscordCommandException(DiscordCommandException.Type.NO_PERMISSION, "Wait a minute, you're not an owner! Only owners can do this");
		}
	}

	public void checkSenderAdmin() throws DiscordCommandException {
		if (!isAdmin()) {
			throw new DiscordCommandException(DiscordCommandException.Type.NO_PERMISSION, "Wait a minute, you're not an admin! Only admins can do this");
		}
	}

	public void adminLog(Color col, String text) {
		gc.adminLogChannelEmbed(spec -> {
			spec.color(col);
			spec.description(Utils.trimEmbedDescription(text));
			spec.timestamp(Instant.now());
			spec.footer(sender.getUsername(), sender.getAvatarUrl());
		});
	}

	public Message replyRaw(Consumer<MessageCreateSpec.Builder> msg) throws DiscordCommandException {
		try {
			MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

			if (referenceMessage && message != null) {
				builder.messageReference(message.getId());
			}

			builder.allowedMentions(allowedMentions);
			msg.accept(builder);

			Message m = Objects.requireNonNull(channelInfo.createMessage(builder.build()).block());

			if (referenceMessage && message != null) {
				MessageHandler.addAutoDelete(message.getId(), new MessageId(m));
			}

			return m;
		} catch (Exception ex) {
			throw new DiscordCommandException("Failed to reply! " + ex);
		}
	}

	public Message reply(Consumer<EmbedCreateSpec.Builder> espec) throws DiscordCommandException {
		return replyRaw(spec -> {
			EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();
			builder.color(EmbedColors.GRAY);
			espec.accept(builder);
			spec.addEmbed(builder.build());
		});
	}

	public Message reply(String title, String message) throws DiscordCommandException {
		return reply(spec -> {
			if (!title.isEmpty()) {
				spec.title(title);
			}

			spec.description(Utils.trimEmbedDescription(message));
		});
	}

	public Message reply(String content) throws DiscordCommandException {
		return replyRaw(spec -> spec.content(content));
	}

	public Message replyAsUser(Consumer<EmbedCreateSpec.Builder> spec) throws DiscordCommandException {
		return reply(espec -> {
			espec.footer(sender.getUsername(), sender.getAvatarUrl());
			spec.accept(espec);
		});
	}

	public Message replyAsUser(String title, String message) throws DiscordCommandException {
		return replyAsUser(spec -> {
			if (!title.isEmpty()) {
				spec.title(title);
			}

			spec.description(Utils.trimEmbedDescription(message));
		});
	}

	public Message replyAsUser(String message) throws DiscordCommandException {
		return replyAsUser("", message);
	}

	public Message replyFile(Consumer<MessageCreateSpec.Builder> msg, String filename, String ext, byte[] data, boolean createLocalFile) throws DiscordCommandException {
		String actualFileName = filename + "-" + Instant.now().toString().replace(':', '-') + "." + ext;

		try {
			if (createLocalFile) {
				Path directory = AppPaths.DATA_GUILDS.resolve(filename);

				if (Files.notExists(directory)) {
					Files.createDirectories(directory);
				}

				Path path = directory.resolve(actualFileName);
				App.info("Writing to " + path.toAbsolutePath());
				Files.createFile(path);
				Files.write(path, data);
			}

			return replyRaw(spec -> {
				spec.addFile(actualFileName, new BufferedInputStream(new ByteArrayInputStream(data)));
				msg.accept(spec);
			});
		} catch (Exception ex) {
			if (createLocalFile) {
				throw new DiscordCommandException("Could not upload file, it's too large (" + (data.length / 1048576L) + " MB)! DM Gnome and request \n" + filename);
			} else {
				throw new DiscordCommandException("Could not upload file, it's too large (" + (data.length / 1048576L) + " MB)!");
			}
		}
	}

	public Message replyFile(String filename, String ext, byte[] data, boolean createLocalFile) throws DiscordCommandException {
		return replyFile(spec -> {
		}, filename, ext, data, createLocalFile);
	}

	public Optional<Message> findMessage(Snowflake id) {
		return gc.findMessage(id, channelInfo);
	}

	public boolean isTrusted() {
		return isTrusted(sender.getId().asLong());
	}

	public boolean isAdmin() {
		return gc.getAuthLevel(sender).is(AuthLevel.ADMIN);
	}

	public boolean isOwner() {
		return gc.getAuthLevel(sender).is(AuthLevel.OWNER);
	}

	public void upvote() {
		if (message != null) {
			message.addReaction(Emojis.VOTEUP).subscribe();
		}
	}

	public static boolean isTrusted(long id) {
		// Lat / Green / Mikey / Gnomette
		return id == 143142144469762048L || id == 96827649573273600L || id == 196688486357663744L || id == 873185409604157460L;
	}
}
