package dev.gnomebot.app.data.config;

import dev.latvian.apps.webutils.ansi.Ansi;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

/**
 * @author LatvianModder
 */
public class DBConfig {
	public final LinkedHashMap<String, BaseConfig<?>> map = new LinkedHashMap<>();

	public boolean read(String name, @Nullable Document document) {
		if (document == null || document.isEmpty()) {
			return true;
		}

		boolean save = false;

		for (BaseConfig<?> config : map.values()) {
			if (document.containsKey(config.id)) {
				try {
					config.fromDB(document.get(config.id));
				} catch (ClassCastException ex) {
					Ansi.log(Ansi.of().append(Ansi.white("Failed to read " + name + "/" + config.id + " :").whiteBg()).append(ex));
					save = true;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				save = true;
			}
		}

		return save;
	}

	public Document write() {
		Document document = new Document();

		for (BaseConfig<?> setting : map.values()) {
			document.put(setting.id, setting.toDB());
		}

		return document;
	}

	@SuppressWarnings("unchecked")
	public <T extends BaseConfig<?>> T add(BaseConfig<?> config) {
		map.put(config.id, config);
		return (T) config;
	}
}
