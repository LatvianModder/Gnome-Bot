package dev.gnomebot.app.util;

import dev.gnomebot.app.data.Currency;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalLong;

public class BasicOption {
	public final String name;
	protected final String rawValue;
	protected final Optional<String> value;

	public BasicOption(String n, Optional<String> v) {
		name = n;
		value = v;
		rawValue = value.orElse("");
	}

	public BasicOption(String n, String v) {
		name = n;
		value = Optional.of(v);
		rawValue = v;
	}

	public boolean isPresent() {
		return value.isPresent();
	}

	@Override
	public String toString() {
		return "BasicOption{" +
				"name='" + name + '\'' +
				", value='" + rawValue + '\'' +
				'}';
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		} else {
			return name.equals(((BasicOption) o).name);
		}
	}

	public Optional<String> asStringOptional() {
		return value;
	}

	public String asString() {
		return rawValue;
	}

	public String asString(String def) {
		return rawValue.isEmpty() ? def : rawValue;
	}

	public Optional<Boolean> asBoolean() {
		return value.map("true"::equalsIgnoreCase);
	}

	public boolean asBoolean(boolean def) {
		return asBoolean().orElse(def);
	}

	public Optional<Long> asLongOptional() {
		return value.map(Long::parseLong);
	}

	public long asLong(long def) {
		return asLongOptional().orElse(def);
	}

	public long asLong() {
		return asLong(0L);
	}

	public Optional<Integer> asIntOptional() {
		return value.map(Integer::parseInt);
	}

	public int asInt(int def) {
		return asIntOptional().orElse(def);
	}

	public int asInt() {
		return asInt(0);
	}

	public Optional<Double> asDouble() {
		return value.map(Double::parseDouble);
	}

	public double asDouble(double def) {
		return asDouble().orElse(def);
	}

	public OptionalLong asSeconds() throws DiscordCommandException {
		return value.isPresent() ? new SimpleStringReader(rawValue).readSeconds() : OptionalLong.empty();
	}

	public OptionalLong asDays() throws DiscordCommandException {
		return value.isPresent() ? new SimpleStringReader(rawValue).readDays() : OptionalLong.empty();
	}

	public Optional<Currency> asCurrency() {
		return value.map(s -> Currency.ALL.get().getOrDefault(s.toUpperCase(), Currency.USD));
	}

	public ZoneId asZone() throws DiscordCommandException {
		if (value.isEmpty()) {
			return ZoneOffset.UTC;
		}

		String string = value.orElse("UTC");

		if (string.startsWith("GMT")) {
			string = "Etc/" + string;
		}

		try {
			return ZoneId.of(string);
		} catch (Exception ex) {
		}

		String l = string.toLowerCase();

		try {
			for (String s : ZoneId.getAvailableZoneIds()) {
				if (s.toLowerCase().equals(l)) {
					return ZoneId.of(s);
				}
			}
		} catch (Exception ex) {
		}

		try {
			for (String s : ZoneId.getAvailableZoneIds()) {
				if (s.toLowerCase().contains(l)) {
					return ZoneId.of(s);
				}
			}
		} catch (Exception ex) {
		}

		throw new DiscordCommandException("Unknown zone ID!");
	}

	public String asContentOrFetch() throws Exception {
		String s = value.orElse("").trim();

		if (s.startsWith("^http")) {
			s = URLRequest.of(s.substring(1)).toJoinedString().block();
		}

		s = s.trim().replaceAll("role:(\\d+)", "<@&$1>")
				.replaceAll("user:(\\d+)", "<@$1>")
				.replaceAll("channel:(\\d+)", "<#$1>")
				.replace("mention:here", "@here")
				.replace("mention:everyone", "@everyone");

		return s;
	}
}
