package dev.gnomebot.app.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public record RelatedGuild(Group group, int index, String invite, long guild, String name, String description, String icon) {
	public record Group(int index, String name, String description, List<RelatedGuild> guilds) {
		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("name", name);
			json.addProperty("description", description);

			var arr = new JsonArray();

			for (var guild : guilds) {
				arr.add(guild.toJson());
			}

			json.add("guilds", arr);
			return json;
		}
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("invite", invite);
		json.addProperty("guild", guild);
		json.addProperty("name", name);
		json.addProperty("description", description);
		json.addProperty("icon", icon);
		return json;
	}

	public String getIcon(int size) {
		return "https://cdn.discordapp.com/icons/" + guild + "/" + icon + ".png?size=" + size;
	}
}
