package dev.gnomebot.app.data.ai;

import dev.latvian.apps.json.JSONObject;

public class AIPart {
	public JSONObject json;
	public String text;
	public String inlineDataType;
	public byte[] inlineData;

	public AIPart(String text) {
		this.text = text;
	}

	public AIPart(String inlineDataType, byte[] inlineData) {
		this.inlineDataType = inlineDataType;
		this.inlineData = inlineData;
	}
}
