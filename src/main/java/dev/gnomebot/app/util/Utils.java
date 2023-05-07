package dev.gnomebot.app.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.latvian.apps.webutils.json.JSONObject;
import dev.latvian.apps.webutils.math.MathUtils;
import discord4j.common.util.Snowflake;
import discord4j.common.util.TimestampFormat;
import discord4j.core.object.Embed;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.json.UserData;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.route.Route;
import discord4j.rest.route.Routes;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;
import reactor.core.Exceptions;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static discord4j.rest.util.Image.Format.GIF;
import static discord4j.rest.util.Image.Format.PNG;

/**
 * @author LatvianModder
 */
public class Utils {
	public static final Snowflake NO_SNOWFLAKE = Snowflake.of(0L);
	public static final Route GUILD_PROFILE_ROUTE = Route.patch("/guilds/{guild.id}/members/@me");
	public static final Route GET_DM_CHANNELS = Route.get("/users/@me/channels");


	private static final int[] CHART_COLORS = new int[24];

	static {
		float off = 4F / 6F;

		for (int i = 0; i < 6; i++) {
			float f = off - i / 6F;
			CHART_COLORS[i] = java.awt.Color.HSBtoRGB(f, 0.8F, 0.8F) & 0xFFFFFF;
			CHART_COLORS[i + 6] = java.awt.Color.HSBtoRGB(f + 2F / 24F, 0.8F, 0.8F) & 0xFFFFFF;
			CHART_COLORS[i + 12] = java.awt.Color.HSBtoRGB(f + 1F / 24F, 0.8F, 0.8F) & 0xFFFFFF;
			CHART_COLORS[i + 18] = java.awt.Color.HSBtoRGB(f + 3F / 24F, 0.8F, 0.8F) & 0xFFFFFF;
		}
	}

	public static String simpleTimeString(long timeInSeconds) {
		StringBuilder builder = new StringBuilder();

		long days = timeInSeconds / 86400L;
		long hours = (timeInSeconds / 3600L) % 24L;
		long minutes = (timeInSeconds / 60L) % 60L;
		long seconds = timeInSeconds % 60L;

		if (days > 0L) {
			builder.append(days);
			builder.append(':');
		}

		if (hours < 10L) {
			builder.append('0');
		}

		builder.append(hours);

		builder.append(':');

		if (minutes < 10L) {
			builder.append('0');
		}

		builder.append(minutes);

		builder.append(':');

		if (seconds < 10L) {
			builder.append('0');
		}

		builder.append(seconds);
		return builder.toString();
	}

	public static String prettyTimeString(long seconds) {
		if (seconds <= 0L) {
			return "0 seconds";
		}

		StringBuilder builder = new StringBuilder();
		prettyTimeString(builder, seconds, true);
		return builder.toString();
	}

	private static void prettyTimeString(StringBuilder builder, long seconds, boolean addAnother) {
		if (seconds <= 0L) {
			return;
		} else if (!addAnother) {
			builder.append(" and ");
		}

		if (seconds < 60L) {
			builder.append(seconds);
			builder.append(seconds == 1L ? " second" : " seconds");
		} else if (seconds < 3600L) {
			builder.append(seconds / 60L);
			builder.append(seconds / 60L == 1L ? " minute" : " minutes");

			if (addAnother) {
				prettyTimeString(builder, seconds % 60L, false);
			}
		} else if (seconds < 86400L) {
			builder.append(seconds / 3600L);
			builder.append(seconds / 3600L == 1L ? " hour" : " hours");

			if (addAnother) {
				prettyTimeString(builder, seconds % 3600L, false);
			}
		} else {
			builder.append(seconds / 86400L);
			builder.append(seconds / 86400L == 1L ? " day" : " days");

			if (addAnother) {
				prettyTimeString(builder, seconds % 86400L, false);
			}
		}
	}

	public static String createToken() {
		byte[] bytes = new byte[30];

		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) MathUtils.RANDOM.nextInt();
		}

		return Base64.getUrlEncoder().encodeToString(bytes);
	}

	public static String percent(double d) {
		if (d <= 0D) {
			return "0%";
		} else if (d >= 1D) {
			return "100%";
		}

		if (d < 0.0001D) {
			return (d * 100D) + "%";
		}

		return ((int) (d * 10000L) / 100D) + "%";
	}

	public static String percent(long value, long total) {
		return percent((double) value / (double) total);
	}

	// TODO: Get rid of this eventually
	public static URLRequest<InputStream> internalRequest(String path) {
		return URLRequest.of(App.url(path)).addHeader("Authorization", "Bearer " + App.instance.db.selfToken.token);
	}

	public static JSONObject readInternalJson(String path) {
		try {
			return internalRequest(path).toJsonObject().block();
		} catch (Exception ex) {
			return null;
		}
	}

	public static BufferedImage getAvatar(Snowflake id, int size) throws Exception {
		Path path = AppPaths.AVATAR_CACHE.resolve(id.asString() + "-" + size + ".png");

		if (Files.notExists(path) || Files.getLastModifiedTime(path).toInstant().isBefore(Instant.now().minusSeconds(259200L))) {
			BufferedImage img = internalRequest("api/info/avatar/" + id.asString() + "/" + size).toImage().block();
			ImageIO.write(img, "PNG", path.toFile());
			return img;
		}

		return ImageIO.read(path.toFile());
	}

	public static BufferedImage getEmoji(Snowflake id, int size) throws Exception {
		Path path = AppPaths.EMOJI_CACHE.resolve(id.asString() + "-" + size + ".png");

		if (Files.notExists(path) || Files.getLastModifiedTime(path).toInstant().isBefore(Instant.now().minusSeconds(259200L))) {
			BufferedImage img = internalRequest("api/info/emoji/" + id.asString() + "/" + size).toImage().block();
			ImageIO.write(img, "PNG", path.toFile());
			return img;
		}

		return ImageIO.read(path.toFile());
	}

	public static BufferedImage resize(BufferedImage image, int width, int height) {
		int w0 = image.getWidth();
		int h0 = image.getHeight();

		if (w0 == width && h0 == height) {
			return image;
		}

		double ratio;

		if (w0 > h0) {
			ratio = width / (float) w0;
		} else {
			ratio = height / (float) h0;
		}

		int w1 = Math.max(1, Math.min(MathUtils.ceil(w0 * ratio), width));
		int h1 = Math.max(1, Math.min(MathUtils.ceil(h0 * ratio), height));

		// App.info("Resizing image from " + w0 + "x" + h0 + " to " + w1 + "x" + h1 + " with ratio " + ratio + " and target size " + width + "x" + height);

		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, (width - w1) / 2, (height - h1) / 2, w1, h1, null);
		g.dispose();
		return resized;
	}

	public static int getChartColor(int id) {
		return CHART_COLORS[id % CHART_COLORS.length];
	}

	public static String reactionToString(ReactionEmoji emoji) {
		if (emoji.asUnicodeEmoji().isPresent()) {
			return emoji.asUnicodeEmoji().get().getRaw();
		} else if (emoji.asCustomEmoji().isPresent()) {
			return emoji.asCustomEmoji().get().asFormat();
		}

		return "";
	}

	public static ReactionEmoji stringToReaction(String s) {
		if (s.isEmpty()) {
			return null;
		} else if (s.indexOf('<') == 0 && s.indexOf('>') == s.length() - 1) {
			String[] s1 = s.substring(1, s.length() - 1).split(":", 3);
			return ReactionEmoji.custom(Snowflake.of(s1[2]), s1[1], s1[0].equals("a"));
		} else {
			return ReactionEmoji.unicode(s);
		}
	}

	public static String formatRelativeDate(@Nullable Instant instant) {
		return instant == null ? "Unknown" : TimestampFormat.RELATIVE_TIME.format(instant);
	}

	public static void editComponents(@Nullable Message message, @Nullable List<ComponentData> c) {
		EditMessageComponentsRequest request = new EditMessageComponentsRequest(c);

		Routes.MESSAGE_EDIT.newRequest(message.getChannelId().asLong(), message.getId().asLong())
				.body(request)
				.exchange(App.instance.discordHandler.client.getCoreResources().getRouter())
				.skipBody()
				.subscribe();
	}

	public static int getComponentWidth(ActionComponent component) {
		if (component instanceof SelectMenu) {
			return 5;
		}

		return 1;
	}

	public static List<ActionRow> splitIntoRows(List<ActionComponent> buttons) {
		List<ActionRow> rows = new ArrayList<>();
		// TODO: Implement me
		return rows;
	}

	public static String createLocalFile(String name, byte[] data) {
		// TODO: Implement me, or not, im not a cop, im just a comment
		return "";
	}

	@Nullable
	public static EmbedCreateFields.Footer getFooter(@Nullable Message m) {
		if (m == null || m.getEmbeds().isEmpty()) {
			return null;
		}

		Embed.Footer footer = m.getEmbeds().get(0).getFooter().orElse(null);
		return footer == null ? null : EmbedCreateFields.Footer.of(footer.getText(), footer.getIconUrl().orElse(null));
	}

	public static Snowflake oldest(Snowflake a, Snowflake b) {
		return a.getTimestamp().toEpochMilli() < b.getTimestamp().toEpochMilli() ? a : b;
	}

	public static Snowflake newest(Snowflake a, Snowflake b) {
		return a.getTimestamp().toEpochMilli() > b.getTimestamp().toEpochMilli() ? a : b;
	}

	@Nullable
	public static String getAvatarUrl(UserData data) {
		return data.avatar().map(s -> "https://cdn.discordapp.com/avatars/" + data.id().asString() + "/" + s + ".png?size=128").orElse(null);
	}

	public static String getAvatarUrl(User user, @Nullable Member member) {
		if (member != null) {
			boolean animated = member.hasAnimatedGuildAvatar();
			return member.getGuildAvatarUrl(animated ? GIF : PNG).orElse(member.getAvatarUrl());
		}

		return user.getAvatarUrl();
	}

	public static PermissionSet getEffectivePermissions(@Nullable GuildChannel channel, Snowflake member) {
		if (channel == null) {
			return PermissionSet.none();
		}

		try {
			PermissionSet set = channel.getEffectivePermissions(member).block();

			if (set == null || set.isEmpty()) {
				return PermissionSet.none();
			} else if (set.contains(Permission.ADMINISTRATOR)) {
				return PermissionSet.all();
			}

			return set;
		} catch (ClientException ex) {
			if (ex.getStatus().code() != 404) {
				App.error("Failed to get permissions for " + member.asString() + " in " + channel.getName() + ": " + ex);
			}
		} catch (Exception ex) {
			App.error("Failed to get permissions for " + member.asString() + " in " + channel.getName() + ": " + ex);
			ex.printStackTrace();
		}

		return PermissionSet.none();
	}

	public static String bodyToString(Object body) {
		try {
			return App.instance.discordHandler.client.getCoreResources().getJacksonResources().getObjectMapper().writeValueAsString(body);
		} catch (JsonProcessingException e) {
			throw Exceptions.propagate(e);
		}
	}
}
