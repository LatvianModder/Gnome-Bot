package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.server.HTTPResponseCode;
import io.javalin.http.Context;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class FileResponse implements Response {
	public static FileResponse of(HTTPResponseCode code, String contentType, byte[] data) {
		return new FileResponse(code, contentType, data);
	}

	public static FileResponse plainText(String text) {
		return of(HTTPResponseCode.OK, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
	}

	public static FileResponse image(BufferedImage img) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "PNG", out);
		return of(HTTPResponseCode.OK, "image/png", out.toByteArray());
	}

	public final HTTPResponseCode code;
	public final String contentType;
	public final byte[] data;

	private FileResponse(HTTPResponseCode cd, String c, byte[] d) {
		code = cd;
		contentType = c;
		data = d;
	}

	@Override
	public void result(Context ctx) {
		ctx.status(code.code);
		ctx.contentType(contentType);
		ctx.result(data);
	}
}
