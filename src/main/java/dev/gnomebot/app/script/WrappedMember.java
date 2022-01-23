package dev.gnomebot.app.script;

import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.GuildMemberEditSpec;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

public class WrappedMember extends WrappedUser {
	private String nickname;

	public WrappedMember(WrappedGuild g, WrappedId i) {
		super(g, i);
	}

	@Override
	public void clearCache() {
		super.clearCache();
		nickname = null;
	}

	@Override
	public boolean isMember() {
		return true;
	}

	@HideFromJS
	public Member getDiscordMember() {
		return Objects.requireNonNull(guild.guild.getMember(id.asSnowflake()));
	}

	public String getNickname() {
		if (nickname == null) {
			nickname = getDiscordMember().getNickname().orElse("");
		}

		return nickname;
	}

	public String getDisplayName() {
		if (getNickname().isEmpty()) {
			return getName();
		}

		return getNickname();
	}

	@Nullable
	public Date getDateJoined() {
		Instant i = getDiscordMember().getJoinTime().orElse(null);
		return i == null ? null : Date.from(i);
	}

	public boolean hasRoles() {
		return !getDiscordMember().getMemberData().roles().isEmpty();
	}

	public boolean hasRole(Snowflake id) {
		return getDiscordMember().getRoleIds().contains(id);
	}

	public WrappedRole[] getRoles() {
		Set<Snowflake> list = getDiscordMember().getRoleIds();
		WrappedRole[] array = new WrappedRole[list.size()];

		int i = 0;
		for (Snowflake s : list) {
			array[i] = guild.roles.get(s);
			i++;
		}

		return array;
	}

	public boolean isPending() {
		return getDiscordMember().isPending();
	}

	public boolean isDeaf() {
		return getDiscordMember().getMemberData().deaf();
	}

	public boolean isMute() {
		return getDiscordMember().getMemberData().mute();
	}

	public void kick(@Nullable String reason) {
		getDiscordMember().kick(reason == null || reason.isBlank() ? null : reason).block();
	}

	public void ban(@Nullable String reason, boolean deleteMessages) {
		getDiscordMember().ban(BanQuerySpec.builder().reason(reason == null || reason.isBlank() ? null : reason).deleteMessageDays(deleteMessages ? 1 : null).build()).block();
	}

	public void addRole(Snowflake id, @Nullable String reason) {
		getDiscordMember().addRole(id, reason).block();
	}

	public void removeRole(Snowflake id, @Nullable String reason) {
		getDiscordMember().removeRole(id, reason).block();
	}

	public void toggleRole(Snowflake id, @Nullable String reason) {
		if (getDiscordMember().getRoleIds().contains(id)) {
			removeRole(id, reason);
		} else {
			addRole(id, reason);
		}
	}

	public void setNickname(String n) {
		nickname = n == null ? "" : n;
		getDiscordMember().edit(GuildMemberEditSpec.builder().nicknameOrNull(nickname.isBlank() ? null : nickname).build()).block();
	}

	public void setDeaf(boolean b) {
		getDiscordMember().edit(GuildMemberEditSpec.builder().deafen(b).build()).block();
	}

	public void setMute(boolean b) {
		getDiscordMember().edit(GuildMemberEditSpec.builder().mute(b).build()).block();
	}

	public void move(Snowflake channel) {
		getDiscordMember().edit(GuildMemberEditSpec.builder().newVoiceChannelOrNull(channel).build()).block();
	}

	public void disconnect() {
		getDiscordMember().edit(GuildMemberEditSpec.builder().newVoiceChannelOrNull(null).build()).block();
	}

	public boolean hasVoiceConnection() {
		return getDiscordMember().getVoiceState().block().getChannelId().isPresent();
	}
}
