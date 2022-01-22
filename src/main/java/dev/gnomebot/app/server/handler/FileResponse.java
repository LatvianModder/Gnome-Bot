package dev.gnomebot.app.server.handler;

import io.javalin.http.Context;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class FileResponse implements Response {
	public static FileResponse of(String contentType, byte[] data) {
		return new FileResponse(contentType, data);
	}

	public static FileResponse plainText(String text) {
		return new FileResponse("text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
	}

	public static FileResponse image(BufferedImage img) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "PNG", out);
		return of("image/png", out.toByteArray());
	}

	public final String contentType;
	public final byte[] data;

	private FileResponse(String c, byte[] d) {
		contentType = c;
		data = d;
	}

	@Override
	public void result(Context ctx) throws Exception {
		ctx.status(200);
		ctx.contentType(contentType);
		ctx.result(data);
	}
}
