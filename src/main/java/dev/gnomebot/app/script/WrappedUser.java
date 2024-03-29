package dev.gnomebot.app.script;

import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.core.object.entity.User;
import discord4j.core.util.ImageUtil;
import discord4j.rest.util.Image;

import java.util.Objects;
import java.util.Optional;

import static discord4j.rest.util.Image.Format.GIF;

public class WrappedUser extends DiscordObject {
	public final WrappedGuild guild;
	private String name;
	private String discriminator;
	private Boolean bot;
	private Optional<String> avatarId;

	public WrappedUser(WrappedId id, WrappedGuild g) {
		super(id);
		guild = g;
	}

	@Override
	public String toString() {
		return getTag();
	}

	public void clearCache() {
		name = null;
		discriminator = null;
		bot = null;
		avatarId = null;
	}

	@HideFromJS
	public User getDiscordUser() {
		return Objects.requireNonNull(guild.gc.db.app.discordHandler.getUser(id.asLong()));
	}

	public boolean isMember() {
		return false;
	}

	public boolean isSelf() {
		return id.asLong() == guild.gc.db.app.discordHandler.selfId;
	}

	public String getMention() {
		return "<@" + id + ">";
	}

	public boolean isBot() {
		if (bot == null) {
			bot = getDiscordUser().isBot();
		}

		return bot;
	}

	public String getName() {
		if (name == null) {
			name = getDiscordUser().getUsername();
		}

		return name;
	}

	public String getDiscriminator() {
		if (discriminator == null) {
			discriminator = getDiscordUser().getDiscriminator();
		}

		return discriminator;
	}

	public String getBelongingName() {
		var n = getName();
		return n + (n.endsWith("s") ? "'" : "'s");
	}

	public String getTag() {
		return getName() + "#" + getDiscriminator();
	}

	public final String getDefaultAvatarUrl() {
		return ImageUtil.getUrl("embed/avatars/" + Integer.parseInt(getDiscriminator()) % 5, Image.Format.PNG);
	}

	public Optional<String> getAvatarId() {
		if (avatarId == null) {
			avatarId = getDiscordUser().getUserData().avatar();
		}

		return avatarId;
	}

	public boolean hasAnimatedAvatar() {
		return getAvatarId().isPresent() && getAvatarId().get().startsWith("a_");
	}

	public String getStaticAvatarUrl() {
		if (getAvatarId().isEmpty()) {
			return getDefaultAvatarUrl();
		}

		return ImageUtil.getUrl("avatars/" + id + "/" + getAvatarId().get(), Image.Format.PNG);
	}

	public String getAvatarUrl() {
		if (getAvatarId().isEmpty()) {
			return getDefaultAvatarUrl();
		}

		return ImageUtil.getUrl("avatars/" + id + "/" + getAvatarId().get(), hasAnimatedAvatar() ? GIF : Image.Format.PNG);
	}
}
