package dev.gnomebot.app.data;

import dev.gnomebot.app.App;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.webutils.json.JSONObject;

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
			var json = URLRequest.of("https://www.floatrates.com/daily/usd.json").toJsonObject().block();

			for (var entry : json.entrySet()) {
				var o = (JSONObject) entry.getValue();
				Currency currency = new Currency(o.asString("code").toUpperCase(), o.asString("name"), o.asDouble("rate"));
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
