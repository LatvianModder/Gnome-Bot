package dev.gnomebot.app.util;

import dev.gnomebot.app.data.Currency;
import dev.gnomebot.app.discord.legacycommand.GnomeException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalLong;

public class BasicOption {
	public final String name;
	protected final String value;

	public BasicOption(String n, String v) {
		name = n;
		value = v;
	}

	public boolean isPresent() {
		return !value.isEmpty();
	}

	@Override
	public String toString() {
		return "BasicOption{" +
				"name='" + name + '\'' +
				", value='" + value + '\'' +
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
		return value.isEmpty() ? Optional.empty() : Optional.of(value);
	}

	public String asString() {
		return value;
	}

	public String asString(String def) {
		return value.isEmpty() ? def : value;
	}

	public Optional<Boolean> asBoolean() {
		return asStringOptional().map("true"::equals);
	}

	public boolean asBoolean(boolean def) {
		return asBoolean().orElse(def);
	}

	public Optional<Long> asLongOptional() {
		return asStringOptional().map(Long::parseLong);
	}

	public long asLong(long def) {
		return asLongOptional().orElse(def);
	}

	public long asLong() {
		return asLong(0L);
	}

	public long asSnowflake() {
		return asLongOptional().orElse(0L);
	}

	public Optional<Integer> asIntOptional() {
		return asStringOptional().map(Integer::parseInt);
	}

	public int asInt(int def) {
		return asIntOptional().orElse(def);
	}

	public int asInt() {
		return asInt(0);
	}

	public Optional<Double> asDouble() {
		return asStringOptional().map(Double::parseDouble);
	}

	public double asDouble(double def) {
		return asDouble().orElse(def);
	}

	public OptionalLong asSeconds() {
		return value.isEmpty() ? OptionalLong.empty() : new SimpleStringReader(value).readSeconds();
	}

	public OptionalLong asDays() {
		return value.isEmpty() ? OptionalLong.empty() : new SimpleStringReader(value).readDays();
	}

	public Optional<Currency> asCurrency() {
		return asStringOptional().map(Currency::get);
	}

	public ZoneId asZone() {
		if (value.isEmpty()) {
			return ZoneOffset.UTC;
		}

		var string = asString("UTC");

		if (string.startsWith("GMT")) {
			string = "Etc/" + string;
		}

		try {
			return ZoneId.of(string);
		} catch (Exception ex) {
		}

		var l = string.toLowerCase();

		try {
			for (var s : ZoneId.getAvailableZoneIds()) {
				if (s.toLowerCase().equals(l)) {
					return ZoneId.of(s);
				}
			}
		} catch (Exception ex) {
		}

		try {
			for (var s : ZoneId.getAvailableZoneIds()) {
				if (s.toLowerCase().contains(l)) {
					return ZoneId.of(s);
				}
			}
		} catch (Exception ex) {
		}

		throw new GnomeException("Unknown zone ID!");
	}

	public String asContentOrFetch() throws Exception {
		var s = value.trim();

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
