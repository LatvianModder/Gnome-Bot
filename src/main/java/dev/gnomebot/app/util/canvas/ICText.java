package dev.gnomebot.app.util.canvas;

import java.awt.Color;
import java.awt.Graphics2D;

public class ICText extends ICWidget {
	public final String string;
	public final Color color;

	public ICText(ImageCanvas canvas, int x, int y, int w, int h, String string, Color color) {
		super(canvas, x, y, canvas.metrics.stringWidth(string), canvas.fontSize);
		this.string = string;
		this.color = color;
	}

	@Override
	public void draw(Graphics2D g) {
		Color c = g.getColor();
		//g.setColor(new Color(Color.HSBtoRGB(Utils.RANDOM.nextFloat(), 1F, 1F)));
		//g.drawRect(x + canvas.padding, y + canvas.padding, w, h);
		g.setColor(color);
		// g.drawString(string, x + canvas.padding, y + canvas.padding + canvas.metrics.getAscent() - canvas.metrics.getDescent() - canvas.metrics.getLeading());
		g.drawString(string, x + canvas.padding, y + canvas.padding);
		g.setColor(c);
	}
}
