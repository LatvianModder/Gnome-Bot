package dev.gnomebot.app.util.canvas;

import java.awt.Color;
import java.awt.Graphics2D;

public class ICColor extends ICInvisible {
	public final Color color;

	public ICColor(ImageCanvas canvas, Color color) {
		super(canvas);
		this.color = color;
	}

	@Override
	public void draw(Graphics2D g) {
		g.setColor(color);
	}
}
