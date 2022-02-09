package dev.gnomebot.app.util;

import discord4j.common.util.Snowflake;

public class MemberAction {
	public String name = "";
	public String type = "";
	public String reason = "";
	public String dm = "";
	public long timespan = 0L;
	public Snowflake role = Utils.NO_SNOWFLAKE;
	public boolean showScam = true;
	public boolean showReport = true;
}
