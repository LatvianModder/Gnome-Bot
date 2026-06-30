package dev.gnomebot.app.data.ai;

import dev.latvian.apps.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AIMessage {
	public long user = 0L;
	public String userName = "";
	public String displayName = "";
	public long timestamp = 0L;
	public String channelName = "";
	public final List<AIPart> parts = new ArrayList<>();

	public void toJson(JSONObject json) {
		json.put("role", displayName.isEmpty() ? "model" : "user");
		var partsArray = json.addArray("parts");

		for (var part : parts) {
			if (part.text != null) {
				var partText = new StringBuilder();

				if (!displayName.isEmpty()) {
					partText.append(displayName);

					if (user != 0L || !userName.isEmpty()) {
						partText.append(" (ID ");

						if (user != 0L && !userName.isEmpty()) {
							partText.append(userName);
							partText.append('/');
							partText.append(Long.toUnsignedString(user));
						} else if (user != 0L) {
							partText.append(Long.toUnsignedString(user));
						} else {
							partText.append(userName);
						}

						partText.append(")");
					}

					if (!channelName.isEmpty()) {
						partText.append(" in ");
						partText.append(channelName);
					}

					partText.append(" at ").append(Instant.ofEpochMilli(timestamp));
					partText.append(" says: ");
				}

				partText.append(part.text);

				partsArray.addObject().put("text", partText.toString());
			} else if (part.inlineData != null && part.inlineDataType != null) {
				var data = partsArray.addObject().addObject("inline_data");
				data.put("mime_type", part.inlineDataType);
				data.put("data", Base64.getEncoder().encodeToString(part.inlineData));
			}
		}
	}
}
