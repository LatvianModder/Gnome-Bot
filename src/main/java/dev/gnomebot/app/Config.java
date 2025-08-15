package dev.gnomebot.app;

import dev.gnomebot.app.discord.WebHookDestination;

public class Config {
	public static class Web {
		public String panel_url = "";
		public int port = 0;
	}

	public static class DB {
		public String uri = "mongodb://localhost:27017/?authSource=admin&retryWrites=true&w=majority";
	}

	public static class Discord {
		public String bot_token = "";
		public String restart_button_token = "";
		public WebHookDestination gnome_mention_webhook = null;
		public long gnome_dm_channel_id = 0L;
		public WebHookDestination gnome_dm_webhook = null;
		public WebHookDestination death_webhook = null;
		public long owner = 0L;
		public long[] trusted = new long[0];

		public boolean isTrusted(long id) {
			if (owner == id) {
				return true;
			}

			for (long l : trusted) {
				if (l == id) {
					return true;
				}
			}

			return false;
		}
	}

	public static class CloudFlare {
		public boolean required = true;
		public String email = "";
		public String api_key = "";
	}

	public static class Microsoft {
		public String client_id = "";
		public String client_secret = "";
	}

	public Web web = new Web();
	public DB db = new DB();
	public Discord discord = new Discord();
	public String wolfram_alpha_token = "";
	public CloudFlare cloudflare = new CloudFlare();
	public Microsoft microsoft = new Microsoft();
}