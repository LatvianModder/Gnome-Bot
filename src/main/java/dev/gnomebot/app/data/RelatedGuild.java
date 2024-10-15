package dev.gnomebot.app.data;

import dev.latvian.apps.json.JSONObject;

import java.util.List;

public record RelatedGuild(Group group, int index, String invite, long guild, String name, String description, String icon) {
	public record Group(int index, String name, String description, List<RelatedGuild> guilds) {
		public JSONObject toJson() {
			var json = JSONObject.of();
			json.put("name", name);
			json.put("description", description);

			var arr = json.addArray("guilds");

			for (var guild : guilds) {
				arr.add(guild.toJson());
			}

			return json;
		}
	}

	public JSONObject toJson() {
		var json = JSONObject.of();
		json.put("invite", invite);
		json.put("guild", guild);
		json.put("name", name);
		json.put("description", description);
		json.put("icon", icon);
		return json;
	}

	public String getIcon(int size) {
		return "https://cdn.discordapp.com/icons/" + guild + "/" + icon + ".png?size=" + size;
	}
}
