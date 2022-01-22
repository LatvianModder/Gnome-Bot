package dev.gnomebot.app.script;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.GuildMemberEditSpec;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

public class WrappedMember extends WrappedUser {
	public final transient Member member;

	public WrappedMember(WrappedGuild g, WrappedId i, @Nullable Member m) {
		super(g, i);
		member = m;

		if (member != null) {
			update(member.getUserData());
		}
	}

	@Override
	public boolean isMember() {
		return true;
	}

	public String getNickname() {
		return member.getNickname().orElse("");
	}

	public String getDisplayName() {
		return member.getDisplayName();
	}

	@Nullable
	public Date getDateJoined() {
		Instant i = member.getJoinTime().orElse(null);
		return i == null ? null : Date.from(i);
	}

	public boolean hasRole() {
		return !member.getMemberData().roles().isEmpty();
	}

	public boolean hasRole(Snowflake id) {
		return member.getRoleIds().contains(id);
	}

	public WrappedRole[] getRoles() {
		Set<Snowflake> list = member.getRoleIds();
		WrappedRole[] array = new WrappedRole[list.size()];

		int i = 0;
		for (Snowflake s : list) {
			array[i] = guild.roles.get(s);
			i++;
		}

		return array;
	}

	public boolean isPending() {
		return member.isPending();
	}

	public boolean isDeaf() {
		return member.getMemberData().deaf();
	}

	public boolean isMute() {
		return member.getMemberData().mute();
	}

	public void kick(@Nullable String reason) {
		member.kick(reason == null || reason.isBlank() ? null : reason).block();
	}

	public void ban(@Nullable String reason, boolean deleteMessages) {
		member.ban(BanQuerySpec.builder().reason(reason == null || reason.isBlank() ? null : reason).deleteMessageDays(deleteMessages ? 1 : null).build()).block();
	}

	public void addRole(Snowflake id) {
		member.addRole(id).block();
	}

	public void removeRole(Snowflake id) {
		member.removeRole(id).block();
	}

	public void toggleRole(Snowflake id) {
		if (member.getRoleIds().contains(id)) {
			member.removeRole(id).block();
		} else {
			member.addRole(id).block();
		}
	}

	public void setNickname(String nickname) {
		member.edit(GuildMemberEditSpec.builder().nicknameOrNull(nickname == null || nickname.isBlank() ? null : nickname).build()).block();
	}

	public void setDeaf(boolean b) {
		member.edit(GuildMemberEditSpec.builder().deafen(b).build()).block();
	}

	public void setMute(boolean b) {
		member.edit(GuildMemberEditSpec.builder().mute(b).build()).block();
	}

	public void move(Snowflake channel) {
		member.edit(GuildMemberEditSpec.builder().newVoiceChannelOrNull(channel).build()).block();
	}

	public void disconnect() {
		member.edit(GuildMemberEditSpec.builder().newVoiceChannelOrNull(null).build()).block();
	}

	public boolean hasVoiceConnection() {
		return member.getVoiceState().block().getChannelId().isPresent();
	}
}
