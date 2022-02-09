package dev.gnomebot.app.util.canvas;

import java.awt.Graphics2D;

public class ICWidget {
	public final ImageCanvas canvas;
	public final int x, y, w, h;

	public ICWidget(ImageCanvas canvas, int x, int y, int w, int h) {
		this.canvas = canvas;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	public void draw(Graphics2D g) {
	}
}
