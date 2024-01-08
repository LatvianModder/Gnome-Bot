package dev.gnomebot.app.cli;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.common.util.Snowflake;

import javax.imageio.ImageIO;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

public class CLIEmojiLeaderboardCommand {
	private static class EmojiEntry {
		public final String name;
		public Snowflake id;
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
		ChannelInfo c = event.reader.readChannelInfo().orElse(null);

		var emojiMap = new HashMap<String, EmojiEntry>();

		for (var message : event.gc.messages.query().filter(c == null ? Filters.regex("content", Emojis.GUILD_EMOJI_PATTERN) : Filters.and(Filters.eq("channel", c.id.asLong()), Filters.regex("content", Emojis.GUILD_EMOJI_PATTERN)))) {
			Matcher matcher = Emojis.GUILD_EMOJI_PATTERN_GROUPS.matcher(message.getContent());

			while (matcher.find()) {
				var entry = emojiMap.computeIfAbsent(matcher.group(1).toLowerCase(), EmojiEntry::new);
				var id = Utils.snowflake(matcher.group(2));

				if (entry.id == null || id.getTimestamp().toEpochMilli() > entry.id.getTimestamp().toEpochMilli()) {
					entry.id = id;
				}

				entry.count++;
			}
		}

		if (emojiMap.isEmpty()) {
			event.respond("What is this? There's no data to display. Come back when you have posted more emojis.");
			return;
		}

		List<EmojiEntry> list = emojiMap.values()
				.stream()
				.sorted(Comparator.comparingLong(EmojiEntry::getOrder))
				.limit(50L).toList();

		Font font = event.gc.font.create(36);
		FontMetrics metrics = new Canvas().getFontMetrics(font);

		int w = 0;

		for (EmojiEntry entry : list) {
			w = Math.max(w, metrics.stringWidth(entry.name + entry.count) + 240);
		}

		w = Math.max(w, 50);
		int h = Math.max(list.size() * 45, 45);

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setBackground(new Color(0x36393F));
		g.clearRect(0, 0, w, h);
		g.setFont(font);

		String indexFormat = "#%0" + String.valueOf(list.size()).length() + "d";

		for (int i = 0; i < list.size(); i++) {
			EmojiEntry entry = list.get(i);
			g.setColor(Color.GRAY);
			g.drawString(String.format(indexFormat, i + 1), 6, 36 + i * 45);
			g.setColor(Color.WHITE);
			g.drawString(entry.name, 151, 36 + i * 45);
			g.setColor(Color.WHITE);

			String cs = FormattingUtils.format(entry.count);
			g.drawString(cs, w - 6 - metrics.stringWidth(cs), 36 + i * 45);

			try {
				BufferedImage a = Utils.getEmoji(entry.id, 42);
				g.drawImage(a, 100, 3 + i * 45, 42, 42, null);
			} catch (Exception ex) {
				g.fillRect(100, 3 + i * 45, 42, 42);
			}
		}

		g.dispose();

		ByteArrayOutputStream imageData = new ByteArrayOutputStream();
		ImageIO.write(image, "png", imageData);
		event.respond("Emoji Leaderboard:");
		event.response.addFile("emoji-leaderboard-" + event.gc.guildId.asString() + ".png", imageData.toByteArray());
	}
}
