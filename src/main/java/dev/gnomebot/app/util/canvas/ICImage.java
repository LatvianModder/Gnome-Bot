package dev.gnomebot.app.util.canvas;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ICImage extends ICWidget {
	public final BufferedImage image;

	public ICImage(ImageCanvas canvas, int x, int y, int w, int h, BufferedImage image) {
		super(canvas, x, y, w, h);
		this.image = image;
	}

	@Override
	public void draw(Graphics2D g) {
		g.drawImage(image, x + canvas.padding, y + canvas.padding, w, h, null);
	}
}
