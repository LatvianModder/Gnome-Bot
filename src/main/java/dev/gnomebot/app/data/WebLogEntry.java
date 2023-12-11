package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

import java.util.Date;

public class WebLogEntry extends WrappedDocument<WebLogEntry> {
	public WebLogEntry(WrappedCollection<WebLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	public String getIP() {
		return document.getString("ip");
	}

	public String getCountry() {
		return document.getString("country");
	}

	public int getStatus() {
		return document.getInt("status");
	}

	public String getMethod() {
		return document.getString("method");
	}

	public String getRequestURL() {
		return document.getString("url");
	}

	public long getUser() {
		return document.getLong("user");
	}

	@Override
	public Date getDate() {
		return document.getDate("timestamp");
	}
}