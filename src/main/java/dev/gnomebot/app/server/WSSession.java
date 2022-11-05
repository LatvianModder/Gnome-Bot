package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.WebToken;
import discord4j.core.object.entity.User;
import org.eclipse.jetty.websocket.api.Session;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public record WSSession(WSHandler handler, Session session, WebToken token, User user) {
	@Override
	public String toString() {
		if (session.getRemoteAddress() instanceof InetSocketAddress inet) {
			return user.getUsername() + '#' + inet.getPort();
		} else {
			return user.getUsername() + '#' + session.getRemoteAddress();
		}
	}

	public void sendString(String message) {
		try {
			session.getRemote().sendString(message);
		} catch (Exception ex) {
			App.error(handler.getName() + " failed to send message " + message + " to " + this + ": " + ex.getMessage());
		}
	}

	public void sendBytes(ByteBuffer data) {
		try {
			session.getRemote().sendBytes(data);
		} catch (Exception ex) {
			App.error(handler.getName() + " failed to send bytes " + data + " to " + this + ": " + ex.getMessage());
		}
	}

	public void close(int code, String reason) {
		try {
			session.close(code, reason);
		} catch (Exception ex) {
			App.error(handler.getName() + " failed to close " + this + " connection: " + ex.getMessage());
		}
	}
}
