package dev.gnomebot.app.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Body {
	public byte[] bytes = new byte[0];
	public Map<String, String> properties = new LinkedHashMap<>();
	public String name = "payload";
	public String filename = "payload";
	public String contentType = "text/plain";

	public String getText() {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public String getProperty(String key, String def) {
		return properties.getOrDefault(key.toLowerCase(), def);
	}

	public Map<String, String> getPostData() {
		var text = getText();

		if (text.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, String> map = new LinkedHashMap<>();

		for (var s : text.split("&")) {
			var p = s.split("=", 2);

			if (p.length == 2) {
				try {
					var k = URLDecoder.decode(p[0], StandardCharsets.UTF_8);
					var v = URLDecoder.decode(p[1], StandardCharsets.UTF_8);

					if (!k.isEmpty() && !v.isEmpty()) {
						map.put(k, v);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		return map;
	}
}
