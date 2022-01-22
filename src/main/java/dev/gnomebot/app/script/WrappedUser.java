package dev.gnomebot.app.script;

import discord4j.core.util.ImageUtil;
import discord4j.discordjson.json.UserData;
import discord4j.rest.util.Image;

import static discord4j.rest.util.Image.Format.GIF;

public class WrappedUser implements WithId {
	public final WrappedGuild guild;
	public final WrappedId id;
	private String name;
	private String discriminator;
	private boolean bot;
	private String avatarId;

	public WrappedUser(WrappedGuild g, WrappedId i) {
		guild = g;
		id = i;
		name = "Unknown";
		discriminator = "0000";
		bot = false;
		avatarId = null;
	}

	public void update(WrappedUser u) {
		name = u.name;
		discriminator = u.discriminator;
		bot = u.bot;
		avatarId = u.avatarId;
	}

	public void update(UserData u) {
		name = u.username();
		discriminator = u.discriminator();
		bot = u.bot().toOptional().orElse(false);
		avatarId = u.avatar().orElse(null);
	}

	@Override
	public WrappedId id() {
		return id;
	}

	public boolean isMember() {
		return false;
	}

	public boolean isSelf() {
		return id.asLong == guild.guild.db.app.discordHandler.selfId.asLong();
	}

	public String getMention() {
		return "<@" + id.asString + ">";
	}

	public boolean isBot() {
		return bot;
	}

	public String getName() {
		return name;
	}

	public String getNameWithS() {
		return name.endsWith("s") ? (name + "'") : (name + "'s");
	}

	public String getTag() {
		return name + "#" + discriminator;
	}

	public String getDiscriminator() {
		return discriminator;
	}

	public final String getDefaultAvatarUrl() {
		return ImageUtil.getUrl("embed/avatars/" + Integer.parseInt(discriminator) % 5, Image.Format.PNG);
	}

	public boolean hasAnimatedAvatar() {
		return avatarId != null && avatarId.startsWith("a_");
	}

	public String getStaticAvatarUrl() {
		if (avatarId == null) {
			return getDefaultAvatarUrl();
		}

		return ImageUtil.getUrl("avatars/" + id.asString + "/" + avatarId, Image.Format.PNG);
	}

	public String getAvatarUrl() {
		if (avatarId == null) {
			return getDefaultAvatarUrl();
		}

		return ImageUtil.getUrl("avatars/" + id.asString + "/" + avatarId, hasAnimatedAvatar() ? GIF : Image.Format.PNG);
	}
}
