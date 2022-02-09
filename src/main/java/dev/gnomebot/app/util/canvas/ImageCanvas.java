package dev.gnomebot.app.util.canvas;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImageCanvas {
	public static final Color BACKGROUND_COLOR = new Color(0x36393F);

	public final List<ICWidget> widgets;
	public int padding;
	public int width;
	public int height;
	public Font font;
	public int fontSize;
	public FontMetrics metrics;

	public ImageCanvas() {
		widgets = new ArrayList<>();
		padding = 6;
		width = 0;
		height = 0;
	}

	public void setFont(Font font) {
		this.font = font;
		this.fontSize = font.getSize();
		this.metrics = new Canvas().getFontMetrics(font);
	}

	public void add(ICWidget widget) {
		widgets.add(widget);

		if (!(widget instanceof ICInvisible)) {
			width = Math.max(width, widget.x + widget.w);
			height = Math.max(height, widget.y + widget.h);
		}
	}

	public void setColor(Color color) {
		add(new ICColor(this, color));
	}

	public void addString(int x, int y, String string, Color color) {
		Rectangle2D bounds = metrics.getStringBounds(string, null);
		add(new ICText(this, x, y, (int) bounds.getWidth(), (int) bounds.getHeight(), string, color));
	}

	public void addImage(int x, int y, int w, int h, BufferedImage image) {
		add(new ICImage(this, x, y, w, h, image));
	}

	public BufferedImage createImage() {
		if (width <= 0 || height <= 0) {
			throw new IllegalStateException("No widgets added to image!");
		}

		BufferedImage image = new BufferedImage(width + padding * 2, height + padding * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setBackground(BACKGROUND_COLOR);
		g.clearRect(0, 0, image.getWidth(), image.getHeight());
		g.setFont(font);
		g.setColor(Color.WHITE);

		for (ICWidget widget : widgets) {
			widget.draw(g);
		}

		g.dispose();
		return image;
	}
}
