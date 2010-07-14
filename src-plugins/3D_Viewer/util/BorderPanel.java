package util;

import java.awt.Panel;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Graphics;

public class BorderPanel extends Panel {

	final Color BC = new Color(139, 142, 255);
	String title = null;

	public BorderPanel() {
		super();
	}

	public BorderPanel(String title) {
		this();
		this.title = title;
	}

	public Insets getInsets() {
		return new Insets(20, 20, 20, 20);
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		super.paint(g);
		if(getWidth() == 0 || getHeight() == 0)
			return;
		g.setColor(BC);
		int x1 = 10;
		int y1 = 10;
		int x2 = getWidth() - 10;
		int y2 = getHeight() - 10;
		if(title == null)
			g.drawLine(x1, y1, x2, y1);
		else {
			int w = g.getFontMetrics().stringWidth(title);
			g.drawLine(x1, y1, x1 + 15, y1);
			g.drawString(title, x1 + 20, y1 + 5);
			g.drawLine(x1 + 25 + w, y1, x2, y1);
		}
		g.drawLine(x2, y1, x2, y2);
		g.drawLine(x2, y2, x1, y2);
		g.drawLine(x1, y2, x1, y1);
	}

	// for testing
	public static void main(String[] args) {
		java.awt.Frame f = new java.awt.Frame("blubber");
		BorderPanel bp = new BorderPanel("bla");
		bp.setPreferredSize(new java.awt.Dimension(200, 200));
		f.add(bp);
		f.pack();
		f.show();
	}
}

