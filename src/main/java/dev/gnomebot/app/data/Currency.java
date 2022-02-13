package dev.gnomebot.app.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gnomebot.app.App;
import dev.gnomebot.app.util.URLRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public class Currency {
	public static final Currency USD = new Currency("USD", "U.S. Dollar", 1D);

	public static Currency get(String id) {
		return ALL.get().getOrDefault(id.toUpperCase(), USD);
	}

	public final String id;
	public final String name;
	public final double rate;

	private Currency(String i, String n, double d) {
		id = i;
		name = n;
		rate = d;
	}

	public static final LazyOptional<Map<String, Currency>> ALL = LazyOptional.of(() -> {
		Map<String, Currency> map = new LinkedHashMap<>();

		try {
			JsonObject json = URLRequest.of("https://www.floatrates.com/daily/usd.json").toJsonObject().block();

			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				JsonObject o = entry.getValue().getAsJsonObject();
				Currency currency = new Currency(o.get("code").getAsString().toUpperCase(), o.get("name").getAsString(), o.get("rate").getAsDouble());
				map.put(currency.id, currency);
			}
		} catch (Exception ex) {
			App.LOGGER.error("Failed to fetch all currencies");
			ex.printStackTrace();
		}

		if (map.isEmpty()) {
			map.put("EUR", new Currency("EUR", "Euro", 1D));
			map.put("GBP", new Currency("GBP", "U.K. Pound Sterling", 1D));
		}

		map.put("usd", USD);
		return map;
	});
}
