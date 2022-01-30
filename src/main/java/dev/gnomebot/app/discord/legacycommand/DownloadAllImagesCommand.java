package dev.gnomebot.app.discord.legacycommand;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.ReactionHandler;
import dev.gnomebot.app.discord.UserCache;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.URLRequest;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author LatvianModder
 */
public class DownloadAllImagesCommand {
	private static class ImageFile {
		public String originalFilename;
		public String ext;
		public String filename;
		public String url;
		public String author;
		public Instant timestamp;
	}

	@LegacyDiscordCommand(name = "download_all_images", help = "Downloads all images in current channel as .zip", permissionLevel = AuthLevel.OWNER)
	public static final CommandCallback COMMAND = (context, reader) -> context.gc.db.app.queueBlockingTask(task -> {
		HashMap<String, ImageFile> linkMap = new HashMap<>();
		UserCache userCache = context.handler.createUserCache();

		Path tempDir = AppPaths.DATA_GUILDS
				.resolve("images-" + context.channelInfo.id.asString())
				.resolve("images-" + context.channelInfo.id.asString() + "-" + Instant.now().toString().replace(':', '-'));

		Files.createDirectories(tempDir);

		for (DiscordMessage m : context.gc.messages.query().eq("channel", context.channelInfo.id.asLong()).filter(Filters.bitsAnySet("flags", DiscordMessage.FLAG_IMAGES))) {
			if (task.cancelled) {
				return;
			}

			for (String url : m.getImages()) {
				ImageFile image = new ImageFile();
				image.originalFilename = url.substring(url.lastIndexOf('/') + 1);

				if (image.originalFilename.endsWith(":large")) {
					continue;
				}

				int extIndex = image.originalFilename.lastIndexOf('.');

				if (extIndex == -1) {
					continue;
				}

				image.ext = image.originalFilename.substring(extIndex + 1);
				image.ext = image.ext.replaceAll("[\\?\\%].*", "");

				image.originalFilename = image.originalFilename.substring(0, extIndex);

				image.filename = image.originalFilename + "." + image.ext;
				int x = 0;

				while (linkMap.containsKey(image.filename)) {
					x++;
					image.filename = image.originalFilename + "_" + x + "." + image.ext;
				}

				image.url = url;
				Snowflake userId = Snowflake.of(m.getUserID());
				User u = userCache.get(userId).orElse(null);
				image.author = u == null ? userId.asString() : u.getTag();
				image.timestamp = m.getDate().toInstant();

				linkMap.put(image.filename, image);
			}
		}

		context.message.addReaction(Emojis.VOTENONE).block();
		ReactionHandler.addListener(new ReactionHandler.Callback(context.gc, context.message) {
			@Override
			public boolean onReaction(Member member, ReactionEmoji emoji) throws Exception {
				if (member.getId().equals(context.sender.getId())) {
					task.cancelled = true;
					ReactionHandler.removeListener(message.getId());
				}

				return true;
			}
		});

		AtomicInteger remaining = new AtomicInteger(linkMap.size());

		byte[] buffer = new byte[65536];

		for (ImageFile image : linkMap.values()) {
			if (task.cancelled) {
				return;
			}

			try (InputStream urlStream = new BufferedInputStream(URLRequest.of(image.url).block());
				 OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(tempDir.resolve(image.filename)))) {
				int len;
				while ((len = urlStream.read(buffer)) > 0) {
					fileOut.write(buffer, 0, len);
				}
			} catch (FileNotFoundException ex) {
				App.info("Not found!");
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			int i = remaining.decrementAndGet();
			App.info("Downloaded " + image.url + ", " + i + " left");
		}

		context.reply("Done!");
		ReactionHandler.removeListener(context.message.getId());
		context.message.removeReactions(Emojis.VOTENONE).subscribe();
	});
}
