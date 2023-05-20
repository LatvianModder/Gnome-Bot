package dev.gnomebot.app;

public interface Assets {
	class Asset {
		public final String filename;
		public final String contentType;
		public final String path;

		private Asset(String f, String c) {
			filename = f;
			contentType = c;
			path = "assets/" + filename;
		}

		public String getPath() {
			return App.url(path);
		}
	}

	static Asset add(String filename, String contentType) {
		return new Asset(filename, contentType);
	}

	Asset AVATAR = add("avatar.png", "image/png");
	Asset EMERGENCY = add("emergency.png", "image/png");
	Asset REPLY_PING = add("replyping.png", "image/png");
}
