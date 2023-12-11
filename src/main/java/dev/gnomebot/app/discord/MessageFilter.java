package dev.gnomebot.app.discord;

import dev.gnomebot.app.util.CharMap;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageFilter {
	public static void alias(StringBuilder sb, char c) {
		CharMap a = CharMap.get(c);

		if (!a.isEmpty()) {
			a.buildPattern(sb);
		} else {
			sb.append(c);
		}
	}

	public enum Type {
		REGEX,
		EVERYONE,
		SUSPICIOUS_INVITE,
		IP_ADDRESS;

		public String getId() {
			return name().toLowerCase();
		}

		public static final Map<String, Type> MAP = Arrays.stream(values()).collect(Collectors.toMap(Type::getId, t -> t));
	}

	public Type type;
	public String filter = "";
	public List<String> actions = new ArrayList<>();
	public Snowflake log = Utils.NO_SNOWFLAKE;
	public boolean cancel = false;
	public boolean leftBoundary = true;
	public boolean rightBoundary = false;
	public boolean spaces = true;

	public Pattern pattern;

	public MessageFilter(@Nullable Document doc) {
		if (doc == null || doc.isEmpty()) {
			return;
		}

		type = Type.MAP.getOrDefault(doc.getString("type"), Type.REGEX);
		filter = doc.getString("filter");
		actions = doc.getList("actions", String.class);
		log = Snowflake.of(doc.getLong("log"));
		cancel = doc.getBoolean("cancel");
		leftBoundary = doc.getBoolean("left_boundary");
		rightBoundary = doc.getBoolean("right_boundary");
		spaces = doc.getBoolean("spaces");
	}

	public Document toDocument() {
		Document doc = new Document();
		doc.put("type", type.getId());
		doc.put("filter", filter);
		doc.put("actions", actions);
		doc.put("log", log.asLong());
		doc.put("cancel", cancel);
		doc.put("left_boundary", leftBoundary);
		doc.put("right_boundary", rightBoundary);
		doc.put("spaces", spaces);
		return doc;
	}

	public void compile() {
		if (filter == null || filter.isEmpty()) {
			pattern = null;
			return;
		}

		StringBuilder sb = new StringBuilder();

		if (leftBoundary) {
			sb.append("\\b");
		}

		for (int i = 0; i < filter.length(); i++) {
			char c = Character.toLowerCase(filter.charAt(i));

			if (i > 0 && spaces) {
				sb.append("[\\s\\W]*");
			}

			if (c != filter.charAt(i)) {
				MessageFilter.alias(sb, c);
			} else {
				sb.append(c);
			}
		}

		if (rightBoundary) {
			sb.append("\\b");
		}

		pattern = Pattern.compile(sb.toString(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	}
}