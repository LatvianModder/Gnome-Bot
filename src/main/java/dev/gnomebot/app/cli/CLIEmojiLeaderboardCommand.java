package dev.gnomebot.app.cli;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;

import javax.imageio.ImageIO;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.HashMap;

public class CLIEmojiLeaderboardCommand {
	private static class EmojiEntry {
		public final String name;
		public long id;
		public long count;

		private EmojiEntry(String n) {
			name = n;
		}

		private long getOrder() {
			return -count;
		}
	}

	public static final CLICommand COMMAND = CLICommand.make("emoji_leaderboard")
			.description("Emoji leaderboard")
			.noAdmin()
			.run(CLIEmojiLeaderboardCommand::run);

	private static void run(CLIEvent event) throws Exception {
		var c = event.reader.readChannelInfo().orElse(null);

		var emojiMap = new HashMap<String, EmojiEntry>();

		for (var message : event.gc.messages.query().filter(c == null ? Filters.regex("content", Emojis.GUILD_EMOJI_PATTERN) : Filters.and(Filters.eq("channel", c.id), Filters.regex("content", Emojis.GUILD_EMOJI_PATTERN)))) {
			var matcher = Emojis.GUILD_EMOJI_PATTERN_GROUPS.matcher(message.getContent());

			while (matcher.find()) {
				var entry = emojiMap.computeIfAbsent(matcher.group(1).toLowerCase(), EmojiEntry::new);
				var id = SnowFlake.num(matcher.group(2));

				if (entry.id == 0L || SnowFlake.timestamp(id) > SnowFlake.timestamp(entry.id)) {
					entry.id = id;
				}

				entry.count++;
			}
		}

		if (emojiMap.isEmpty()) {
			event.respond("What is this? There's no data to display. Come back when you have posted more emojis.");
			return;
		}

		var list = emojiMap.values()
				.stream()
				.sorted(Comparator.comparingLong(EmojiEntry::getOrder))
				.limit(50L).toList();

		var font = event.gc.font.create(36);
		var metrics = new Canvas().getFontMetrics(font);

		var w = 0;

		for (var entry : list) {
			w = Math.max(w, metrics.stringWidth(entry.name + entry.count) + 240);
		}

		w = Math.max(w, 50);
		var h = Math.max(list.size() * 45, 45);

		var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		var g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setBackground(new Color(0x36393F));
		g.clearRect(0, 0, w, h);
		g.setFont(font);

		var indexFormat = "#%0" + String.valueOf(list.size()).length() + "d";

		for (var i = 0; i < list.size(); i++) {
			var entry = list.get(i);
			g.setColor(Color.GRAY);
			g.drawString(String.format(indexFormat, i + 1), 6, 36 + i * 45);
			g.setColor(Color.WHITE);
			g.drawString(entry.name, 151, 36 + i * 45);
			g.setColor(Color.WHITE);

			var cs = FormattingUtils.format(entry.count);
			g.drawString(cs, w - 6 - metrics.stringWidth(cs), 36 + i * 45);

			try {
				var a = Utils.getEmoji(event.app, entry.id, 42);
				g.drawImage(a, 100, 3 + i * 45, 42, 42, null);
			} catch (Exception ex) {
				g.fillRect(100, 3 + i * 45, 42, 42);
			}
		}

		g.dispose();

		var imageData = new ByteArrayOutputStream();
		ImageIO.write(image, "png", imageData);
		event.respond("Emoji Leaderboard:");
		event.response.addFile("emoji-leaderboard-" + event.gc.guildId + ".png", imageData.toByteArray());
	}
}
