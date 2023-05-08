package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageId;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.entity.RestChannel;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class Paste extends WrappedDocument<Paste> {
	public Paste(WrappedCollection<Paste> c, MapWrapper d) {
		super(c, d);
	}

	public long getChannelID() {
		return document.getLong("channel");
	}

	public String getFilename() {
		return document.getString("filename");
	}

	public String getUser() {
		return document.getString("user", "Unknown");
	}

	public static String getOriginalUrl(String channelId, String attachmentId, String filename) {
		return "https://cdn.discordapp.com/attachments/" + channelId + "/" + attachmentId + "/" + filename;
	}

	public static String getUrl(String attachmentId) {
		return App.url("paste/" + attachmentId);
	}

	public static void pasteMessage(Databases db, RestChannel channel, Message m, List<Attachment> attachments) {
		if (attachments.isEmpty()) {
			return;
		}

		String user = m.getUserData().username();
		List<Button> buttons = new ArrayList<>();

		for (Attachment attachment : attachments) {
			Paste.createPaste(db, m.getChannelId().asLong(), attachment.getId().asLong(), attachment.getFilename(), user);
			buttons.add(Button.link(getUrl(attachment.getId().asString()), "View " + attachment.getFilename()));
		}

		channel.createMessage(MessageCreateSpec.builder()
				.allowedMentions(DiscordMessage.noMentions())
				.content("Paste version of " + attachments.stream().map(a -> "`" + a.getFilename() + "`").collect(Collectors.joining(", ")) + " from " + m.getAuthor().map(User::getMention).orElse("Unknown"))
				.addComponent(ActionRow.of(buttons))
				.build().asRequest()
		).subscribe(m1 -> MessageHandler.addAutoDelete(m.getId(), new MessageId(m1.channelId().asLong(), m1.id().asLong())));
	}

	public static void createPaste(Databases db, long channelId, long attachmentId, String filename, String user) {
		var updates = new ArrayList<Bson>();
		updates.add(Updates.set("channel", channelId));
		updates.add(Updates.set("filename", filename));

		if (!user.isEmpty()) {
			updates.add(Updates.set("user", user));
		}

		db.pastes.query(attachmentId).upsert(updates);
	}
}