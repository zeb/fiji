package fiji.utilities;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.util.ArrayList;
import java.util.List;

public class HistogramPlot extends Canvas
		implements MouseListener, MouseMotionListener {
	private float[] values;
	private float min, max;

	private Color color;

	private int margin = 2;
	private int sliderHeight = 5, sliderWidth = 7;
	private int fontSize = 7;
	private Font font = new Font("SansSerif", Font.PLAIN, fontSize);

	private float sliderLeft, sliderMiddle, sliderRight;

	public HistogramPlot() {
		setSize(new Dimension(256, 50));
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public HistogramPlot(float[] values, Color color) {
		this();
		this.color = color;
		setValues(values);
	}

	public void updateMinAndMax() {
		min = max = values[0];
		for (int i = 1; i < values.length; i++)
			if (min > values[i])
				min = values[i];
			else if (max < values[i])
				max = values[i];
	}

	public void setValues(float[] values) {
		this.values = values;
		updateMinAndMax();
		sliderLeft = 0;
		sliderMiddle = .5f;
		sliderRight = 1;
	}

	public void paint(Graphics g) {
		if (values == null)
			return;

		g.setColor(color);

		Dimension size = getSize();
		int left = margin;
		int w = size.width - margin;
		int h = size.height - 2 * margin;

		if (sliderHeight > 0)
			h -= margin + sliderHeight;

		if (size.height > 2 * fontSize)
			left += margin
				+ paintLabels(g, margin, margin, h);

		if (sliderHeight > 0)
			paintSliders(g, left, h + margin, w - left);

		paintHistogram(g, left, margin, w - left, h);
	}

	int paintLabels(Graphics g, int x, int y, int h) {
		String min = "" + this.min, max = "" + this.max;
		FontMetrics metrics = g.getFontMetrics();
		int w1 = metrics.stringWidth(min);
		int w2 = metrics.stringWidth(max);
		int w3 = Math.max(w1, w2);
		g.drawString(min, x + w3 - w1, y + h);
		g.drawString(max, x + w3 - w2, y + fontSize);
		w3 += margin;
		g.drawLine(x + w3, y, x + w3 + margin, y);
		g.drawLine(x + w3 + margin, y, x + w3 + margin, y + h);
		g.drawLine(x + w3 + margin, y + h, x + w3, y + h);
		return w3 + margin;
	}

	void setShade(Graphics g, float shade) {
		Color gray = new Color((int)(color.getRed() * shade),
				(int)(color.getGreen() * shade),
				(int)(color.getBlue() * shade));
		g.setColor(gray);
	}

	void paintHistogram(Graphics g, int x, int y, int w, int h) {
		for (int i = 0; i < values.length; i++) {
			int x1 = x + i * w / values.length;
			int x2 = x + (i + 1) * w / values.length;
			int h1 = (int)((values[i] - min) * h
					/ (max - min));
			setShade(g, i / (float)(values.length - 1));
			g.fillRect(x1, y + h - h1, x2 - x1, h1);
		}
	}

	void paintSlider(Graphics g, int x, int y, int w, float value,
			float shade) {
		setShade(g, shade);
		x += (int)Math.round(value * w);
		g.drawLine(x, y, x - sliderWidth / 2, y + sliderHeight);
		g.drawLine(x, y, x + sliderWidth / 2, y + sliderHeight);
	}

	private int sliderMinX, sliderMaxX;

	void paintSliders(Graphics g, int x, int y, int w) {
		w--;
		paintSlider(g, x, y, w, sliderLeft, 0);
		paintSlider(g, x, y, w, sliderMiddle, 0.5f);
		paintSlider(g, x, y, w, sliderRight, 1);

		sliderMinX = x;
		sliderMaxX = x + w;
	}

	private boolean dragging;
	private int draggedSlider;

	public void mouseDragged(MouseEvent e) {
		if (!dragging)
			return;
		float delta = 1f / (sliderMaxX + 1 - sliderMinX);
		float f = (e.getX() - sliderMinX)
			/ (float)(sliderMaxX - sliderMinX);
		switch (draggedSlider) {
			case 0:
				if (sliderLeft == f || f > 1f - 2 * delta ||
						f < 0)
					return;
				sliderLeft = f;
				if (sliderMiddle <= f) {
					sliderMiddle = f + delta;
					if (sliderRight <= sliderMiddle)
						sliderRight = sliderMiddle
							+ delta;
				}
				break;
			case 1:
				if (sliderMiddle == f || f > 1f - delta ||
						f < delta)
					return;
				sliderMiddle = f;
				if (sliderLeft >= f)
					sliderLeft = f - delta;
				if (sliderRight <= f)
					sliderRight = f + delta;
				break;
			case 2:
				if (sliderRight == f || f < 2 * delta || f > 1)
					return;
				sliderRight = f;
				if (sliderMiddle >= f) {
					sliderMiddle = f - delta;
					if (sliderLeft >= sliderMiddle)
						sliderLeft = sliderMiddle
							- delta;
				}
				break;
		}
		repaint();
		notifyListeners();
	}

	// TODO: rethink sliderMaxX (+1)
	public void mousePressed(MouseEvent e) {
		if (sliderHeight == 0)
			return;

		dragging = false;
		int x = e.getX();
		if (x < sliderMinX || x > sliderMaxX)
			return;

		Dimension size = getSize();
		int y = e.getY();
		if (y < size.height - 2 * margin - sliderHeight)
			return;

		dragging = true;
		float f = (x - sliderMinX) / (float)(sliderMaxX - sliderMinX);
		if (f - sliderLeft < sliderMiddle - f)
			draggedSlider = 0;
		else if (f - sliderMiddle < sliderRight - f)
			draggedSlider = 1;
		else
			draggedSlider = 2;
	}

	public void mouseReleased(MouseEvent e) {
		dragging = false;
	}

	public void mouseMoved(MouseEvent e) { }
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	private List<SliderEventListener> listeners =
		new ArrayList<SliderEventListener>();

	public static class SliderEvent {
		float sliderLeft, sliderMiddle, sliderRight;
	}

	public interface SliderEventListener {
		void sliderChanged(SliderEvent event);
	}

	public void addListener(SliderEventListener listener) {
		listeners.add(listener);
	}

	public void removeListener(SliderEventListener listener) {
		listeners.remove(listener);
	}

	protected void notifyListeners() {
		if (listeners.isEmpty())
			return;
		SliderEvent event = new SliderEvent();
		event.sliderLeft = sliderLeft;
		event.sliderMiddle = sliderMiddle;
		event.sliderRight = sliderRight;
		for (SliderEventListener listener : listeners)
			listener.sliderChanged(event);
	}
}
