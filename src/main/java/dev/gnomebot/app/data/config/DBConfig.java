package dev.gnomebot.app.data.config;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DBConfig {
	public final Map<String, BaseConfig<?>> map = new LinkedHashMap<>();

	public void read(String name, @Nullable Document document, List<Bson> updates) {
		if (document == null) {
			for (var config : map.values()) {
				if (config.isSet()) {
					updates.add(Updates.set(config.id, config.write()));
				}
			}

			return;
		}

		for (var config : map.values()) {
			if (document.containsKey(config.id)) {
				try {
					config.read(document);
				} catch (ClassCastException ex) {
					App.error("Failed to read " + name + "/" + config.id + " :" + ex);

					if (config.isSet()) {
						updates.add(Updates.set(config.id, config.write()));
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		for (var key : document.keySet()) {
			if (!key.equals("_id") && !map.containsKey(key)) {
				updates.add(Updates.unset(key));
			}
		}
	}

	public <T extends BaseConfig<?>> T add(T config) {
		map.put(config.id, config);
		return config;
	}
}
