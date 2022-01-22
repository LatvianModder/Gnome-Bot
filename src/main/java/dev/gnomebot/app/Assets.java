package dev.gnomebot.app;

import java.util.HashMap;
import java.util.Map;

public interface Assets {
	class Asset {
		public final String filename;
		public final String contentType;
		public final String path;

		private Asset(String f, String c) {
			filename = f;
			contentType = c;
			path = "api/assets/" + filename;
		}

		public String getPath() {
			return App.url(path);
		}
	}

	Map<String, Asset> MAP = new HashMap<>();

	static Asset add(String filename, String contentType) {
		Asset a = new Asset(filename, contentType);
		MAP.put(a.filename, a);
		return a;
	}

	Asset AVATAR = add("avatar.png", "image/png");
	Asset EMERGENCY = add("emergency.png", "image/png");
	Asset REPLY_PING = add("replyping.png", "image/png");
	Asset STYLE = add("style.css", "text/css");
}
