package dev.gnomebot.app.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static discord4j.rest.util.Image.Format.GIF;
import static discord4j.rest.util.Image.Format.PNG;

/**
 * @author LatvianModder
 */
public class Utils {
	public static final Snowflake NO_SNOWFLAKE = Snowflake.of(0L);
	public static final Random RANDOM = new Random();
	public static final DecimalFormat LONG_FORMAT = new DecimalFormat("#,###");

	private static final GsonBuilder GSON_BUILDER = new GsonBuilder()
			.disableHtmlEscaping()
			.setLenient()
			.registerTypeAdapter(Snowflake.class, new TypeAdapter<Snowflake>() {
				@Override
				public void write(JsonWriter out, Snowflake value) throws IOException {
					out.value(value.asString());
				}

				@Override
				public Snowflake read(JsonReader in) throws IOException {
					String s = in.nextString();
					return s.isEmpty() || s.equals("0") ? NO_SNOWFLAKE : Snowflake.of(s);
				}
			})
			.serializeNulls();

	public static final Gson GSON = GSON_BUILDER.create();
	public static final Gson GSON_PRETTY = GSON_BUILDER.setPrettyPrinting().create();
	public static final MessageDigest MD5 = getMD5();
	public static final MessageDigest SHA1 = getSHA1();
	private static final Calendar formatCalendar = Calendar.getInstance();

	public static final String[] MONTH_NAMES = {
			"Jan",
			"Feb",
			"Mar",
			"Apr",
			"May",
			"Jun",
			"Jul",
			"Aug",
			"Sep",
			"Oct",
			"Nov",
			"Dec",
	};

	public static final Route THREAD_ROUTE = Routes.START_THREAD_WITH_MESSAGE;
	public static final Route GUILD_PROFILE_ROUTE = Route.patch("/guilds/{guild.id}/members/@me");
	public static final Route GET_DM_CHANNELS = Route.get("/users/@me/channels");
	public static final Pattern REGEX_PATTERN = Pattern.compile("/(.*)/([a-z]*)");

	private static MessageDigest getMD5() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (Exception ex) {
			throw new RuntimeException("MD5 not found!");
		}
	}

	private static MessageDigest getSHA1() {
		try {
			return MessageDigest.getInstance("SHA-1");
		} catch (Exception ex) {
			throw new RuntimeException("SHA1 not found!");
		}
	}

	public static final Pattern REMOVE_WEIRD_CHARACTERS = Pattern.compile("[^\\w-.]");
	public static final Pattern REMOVE_DASHES = Pattern.compile("-{2,}");

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

	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

	@SuppressWarnings("all")
	public static <T> T cast(@Nullable Object value) {
		return value == null ? null : (T) value;
	}

	public static String normalize(@Nullable String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}

		s = REMOVE_DASHES.matcher(REMOVE_WEIRD_CHARACTERS.matcher(s.toLowerCase()).replaceAll("-")).replaceAll("-");

		while (s.startsWith("-")) {
			s = s.substring(1);
		}

		while (s.endsWith("-")) {
			s = s.substring(0, s.length() - 1);
		}

		return s;
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

	public static JsonObject singleJson(String key, JsonElement value) {
		JsonObject json = new JsonObject();
		json.add(key, value);
		return json;
	}

	public static String hex(byte[] bytes) {
		byte[] hexChars = new byte[bytes.length * 2];

		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}

		return new String(hexChars, StandardCharsets.UTF_8);
		//return javax.xml.bind.DatatypeConverter.printHexBinary(bytes).toLowerCase();
	}

	public static String md5(byte[] bytes) {
		MD5.reset();
		return hex(MD5.digest(bytes));
	}

	public static String md5(String string) {
		return md5(string.getBytes(StandardCharsets.UTF_8));
	}

	public static String sha1(byte[] bytes) {
		SHA1.reset();
		return hex(SHA1.digest(bytes));
	}

	public static String sha1(String string) {
		return sha1(string.getBytes(StandardCharsets.UTF_8));
	}

	public static String sha1(String key, String string) throws Exception {
		SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(keySpec);
		return hex(mac.doFinal(string.getBytes(StandardCharsets.UTF_8)));
	}

	public static String toAnsiString(JsonElement json) {
		StringBuilder builder = new StringBuilder();
		toAnsiString(builder, json, 0);
		return builder.toString();
	}

	private static void toAnsiString(StringBuilder builder, @Nullable JsonElement element, int depth) {
		if (element == null || element.isJsonNull()) {
			builder.append(Ansi.RED);
			builder.append("null");
			builder.append(Ansi.RESET);
		} else if (element instanceof JsonObject) {
			builder.append('{');
			boolean first = true;

			for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
				if (first) {
					first = false;
				} else {
					builder.append(',');
				}

				builder.append(Ansi.CYAN);
				builder.append('"');
				builder.append(entry.getKey());
				builder.append('"');
				builder.append(Ansi.RESET);

				builder.append(':');

				toAnsiString(builder, entry.getValue(), depth + 1);
			}

			builder.append('}');
		} else if (element instanceof JsonArray) {
			builder.append('[');
			boolean first = true;

			for (JsonElement e : element.getAsJsonArray()) {
				if (first) {
					first = false;
				} else {
					builder.append(',');
				}

				toAnsiString(builder, e, depth + 1);
			}

			builder.append(']');
		} else {
			JsonPrimitive primitive = (JsonPrimitive) element;

			if (primitive.isString()) {
				builder.append(Ansi.YELLOW);
			} else if (primitive.isBoolean()) {
				builder.append(primitive.getAsBoolean() ? Ansi.GREEN : Ansi.RED);
			} else {
				builder.append(Ansi.PURPLE);
			}

			builder.append(primitive);
			builder.append(Ansi.RESET);
		}
	}

	public static String toAnsiStringJson(JsonElement json) {
		StringBuilder builder = new StringBuilder();
		toAnsiStringJson(builder, json, 0);
		return builder.toString();
	}

	private static void toAnsiStringJson(StringBuilder builder, @Nullable JsonElement element, int depth) {
		if (element == null || element.isJsonNull()) {
			builder.append(Ansi.RED);
			builder.append("null");
			builder.append(Ansi.RESET);
		} else if (element instanceof JsonObject) {
			builder.append('{');
			boolean first = true;

			for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
				if (first) {
					first = false;
				} else {
					builder.append(',');
				}

				builder.append(Ansi.CYAN);
				builder.append('"');
				builder.append(entry.getKey());
				builder.append('"');
				builder.append(Ansi.RESET);

				builder.append(':');

				toAnsiStringJson(builder, entry.getValue(), depth + 1);
			}

			builder.append('}');
		} else if (element instanceof JsonArray) {
			builder.append('[');
			boolean first = true;

			for (JsonElement e : element.getAsJsonArray()) {
				if (first) {
					first = false;
				} else {
					builder.append(',');
				}

				toAnsiStringJson(builder, e, depth + 1);
			}

			builder.append(']');
		} else {
			JsonPrimitive primitive = (JsonPrimitive) element;

			if (primitive.isString()) {
				builder.append(Ansi.YELLOW);
			} else if (primitive.isBoolean()) {
				builder.append(primitive.getAsBoolean() ? Ansi.GREEN : Ansi.RED);
			} else {
				builder.append(Ansi.PURPLE);
			}

			builder.append(primitive);
			builder.append(Ansi.RESET);
		}
	}

	public static String toAnsiString(Object object) {
		StringBuilder builder = new StringBuilder();
		toAnsiString(builder, object, 0);
		return builder.toString();
	}

	@SuppressWarnings("rawtypes")
	private static void toAnsiString(StringBuilder builder, @Nullable Object element, int depth) {
		if (element == null) {
			builder.append(Ansi.RED);
			builder.append("null");
			builder.append(Ansi.RESET);
		} else if (element instanceof Map) {
			builder.append('{');
			boolean first = true;

			for (Map.Entry<?, ?> entry : ((Map<?, ?>) element).entrySet()) {
				if (first) {
					first = false;
				} else {
					builder.append(',');
				}

				builder.append(Ansi.CYAN);
				builder.append(entry.getKey());
				builder.append(Ansi.RESET);

				builder.append(':');

				toAnsiString(builder, entry.getValue(), depth + 1);
			}

			builder.append('}');
		} else if (element instanceof Iterable) {
			builder.append('[');
			boolean first = true;

			for (Object e : (Iterable) element) {
				if (first) {
					first = false;
				} else {
					builder.append(',');
				}

				toAnsiString(builder, e, depth + 1);
			}

			builder.append(']');
		} else {
			if (element instanceof String) {
				builder.append(Ansi.YELLOW);
				builder.append('"');
				builder.append(element);
				builder.append('"');
				builder.append(Ansi.RESET);
			} else if (element instanceof Boolean) {
				builder.append((Boolean) element ? Ansi.GREEN : Ansi.RED);
				builder.append(element);
				builder.append(Ansi.RESET);
			} else {
				builder.append(Ansi.PURPLE);
				builder.append(element);
				builder.append(Ansi.RESET);
			}
		}
	}

	public static String encode(String string) {
		try {
			return URLEncoder.encode(string, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			return string;
		}
	}

	public static String decode(String string) {
		try {
			return URLDecoder.decode(string, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			return string;
		}
	}

	public static String encode(Map<String, String> map, String sep) {
		StringBuilder sb = new StringBuilder();

		boolean first = true;

		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(sep);
			}

			sb.append(entry.getKey());

			if (!entry.getValue().isEmpty()) {
				sb.append('=');
				sb.append(encode(entry.getValue()));
			}
		}

		return sb.toString();
	}

	public static Map<String, String> decodeHeaders(String data) {
		Map<String, String> m = new LinkedHashMap<>();
		String[] s0 = data.split(";");

		for (int i = 0; i < s0.length; i++) {
			String s = s0[i].trim();
			int j = s.indexOf('=');

			if (j == -1) {
				if (i == 0) {
					m.put("", s);
				}

				m.put(s, "");
			} else {
				String s1 = s.substring(j + 1);

				if (s1.startsWith("\"") && s1.endsWith("\"")) {
					s1 = s1.substring(1, s1.length() - 1);
				}

				m.put(s.substring(0, j), decode(s1));
			}
		}

		return m;
	}

	public static String createToken() {
		byte[] bytes = new byte[30];

		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) RANDOM.nextInt();
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

	public static JsonElement readInternalJson(String path) {
		try {
			return internalRequest(path).toJson().block();
		} catch (Exception ex) {
			return JsonNull.INSTANCE;
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

		int w1 = Math.max(1, Math.min(Utils.ceil(w0 * ratio), width));
		int h1 = Math.max(1, Math.min(Utils.ceil(h0 * ratio), height));

		// App.info("Resizing image from " + w0 + "x" + h0 + " to " + w1 + "x" + h1 + " with ratio " + ratio + " and target size " + width + "x" + height);

		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, (width - w1) / 2, (height - h1) / 2, w1, h1, null);
		g.dispose();
		return resized;
	}

	public static String format(long number) {
		return LONG_FORMAT.format(number);//.replace(',', '\'');
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

	public static <T> Stream<T> toStream(Iterable<T> iterable) {
		if (iterable instanceof Collection) {
			return ((Collection<T>) iterable).stream();
		}

		List<T> list = new ArrayList<>();
		iterable.forEach(list::add);
		return list.stream();
	}

	public static JsonElement toJson(@Nullable Object o) {
		if (o == null) {
			return JsonNull.INSTANCE;
		} else if (o instanceof JsonElement) {
			return (JsonElement) o;
		} else if (o instanceof Map || o instanceof MapWrapper) {
			return MapWrapper.wrap(o).toJson();
		} else if (o instanceof Boolean) {
			return new JsonPrimitive((Boolean) o);
		} else if (o instanceof Number) {
			return new JsonPrimitive((Number) o);
		} else if (o instanceof Iterable) {
			JsonArray array = new JsonArray();

			for (Object o1 : (Iterable<?>) o) {
				array.add(toJson(o1));
			}

			return array;
		}

		return new JsonPrimitive(o.toString());
	}

	public static void pad0(StringBuilder sb, int num) {
		if (num < 10) {
			sb.append('0');
		}

		sb.append(num);
	}

	public static String formatDate(@Nullable Date date) {
		if (date == null) {
			return "Unknown";
		}

		formatCalendar.setTime(date);

		StringBuilder sb = new StringBuilder();

		pad0(sb, formatCalendar.get(Calendar.DAY_OF_MONTH));
		sb.append('-');
		sb.append(MONTH_NAMES[formatCalendar.get(Calendar.MONTH)]);
		sb.append('-');
		sb.append(formatCalendar.get(Calendar.YEAR));
		sb.append(' ');
		pad0(sb, formatCalendar.get(Calendar.HOUR_OF_DAY));
		sb.append(':');
		pad0(sb, formatCalendar.get(Calendar.MINUTE));
		sb.append(':');
		pad0(sb, formatCalendar.get(Calendar.SECOND));

		return sb.toString();
	}

	public static String formatRelativeDate(@Nullable Instant instant) {
		return instant == null ? "Unknown" : TimestampFormat.RELATIVE_TIME.format(instant);
	}

	public static String trim(String string, int max) {
		string = string.trim();

		if (string.length() > max) {
			return string.substring(0, max - 1) + '…';
		}

		return string;
	}

	public static String trimContent(String content) {
		return trim(content, 2000);
	}

	public static String verifyMaxLength(String string, int max) {
		if (string.length() > max) {
			throw new IllegalArgumentException("String is too long!");
		}

		return string;
	}

	public static void titleCase(StringBuilder sb, String string) {
		if (!string.isEmpty()) {
			sb.append(Character.toUpperCase(string.charAt(0)));
			sb.append(string, 1, string.length());
		}
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

	public static Object getPrivate(Object obj, Class<?> c, String f) {
		try {
			Field field = c.getDeclaredField(f);
			field.setAccessible(true);
			return field.get(obj);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
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

	public static int ceil(float f) {
		int i = (int) f;
		return f > (float) i ? i + 1 : i;
	}

	public static int ceil(double d) {
		int i = (int) d;
		return d > (double) i ? i + 1 : i;
	}

	public static JsonArray asArray(JsonElement element) {
		if (element.isJsonArray()) {
			return element.getAsJsonArray();
		}

		JsonArray array = new JsonArray();
		array.add(element);
		return array;
	}

	@Nullable
	public static Pattern parseSafeRegEx(String string, int defaultFlags) {
		if (string.length() < 3) {
			return null;
		}

		Matcher matcher = REGEX_PATTERN.matcher(string);

		if (!matcher.matches()) {
			return null;
		}

		int flags = defaultFlags;
		String f = matcher.group(2);

		for (int i = 0; i < f.length(); ++i) {
			switch (f.charAt(i)) {
				case 'U' -> flags |= 256;
				case 'd' -> flags |= 1;
				case 'i' -> flags |= 2;
				case 'm' -> flags |= 8;
				case 's' -> flags |= 32;
				case 'u' -> flags |= 64;
				case 'x' -> flags |= 4;
			}
		}

		String pattern = matcher.group(1);

		// check if pattern contains dangerous regex characters
		return Pattern.compile(pattern, flags);
	}

	public static String toRegexString(Pattern pattern) {
		StringBuilder sb = new StringBuilder("/");
		sb.append(pattern.pattern());
		sb.append('/');
		int flags = pattern.flags();
		if ((flags & 1) != 0) {
			sb.append('d');
		}

		if ((flags & 2) != 0) {
			sb.append('i');
		}

		if ((flags & 4) != 0) {
			sb.append('x');
		}

		if ((flags & 8) != 0) {
			sb.append('m');
		}

		if ((flags & 32) != 0) {
			sb.append('s');
		}

		if ((flags & 64) != 0) {
			sb.append('u');
		}

		if ((flags & 256) != 0) {
			sb.append('U');
		}

		return sb.toString();
	}
}
