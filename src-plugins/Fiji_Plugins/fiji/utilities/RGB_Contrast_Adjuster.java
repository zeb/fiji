package fiji.utilities;

import ij.ImagePlus;

import ij.plugin.frame.PlugInFrame;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class RGB_Contrast_Adjuster extends PlugInFrame
		implements HistogramPlot.SliderEventListener {

	HistogramPlot red, green, blue, gray;
	CurrentSliceWatcher watcher;
	GridBagConstraints constraints;

	public RGB_Contrast_Adjuster() {
		super("RGB Contrast Adjuster");

		setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.fill = constraints.BOTH;
		constraints.weightx = constraints.weighty = 1;

		red = addHistogram(Color.RED);
		green = addHistogram(Color.GREEN);
		blue = addHistogram(Color.BLUE);
		gray = addHistogram(Color.BLACK);
		gray.setVisible(false);
		pack();

		watcher = new CurrentSliceWatcher();
	}


	protected HistogramPlot addHistogram(Color color) {
		constraints.gridy++;
		HistogramPlot histogram = new HistogramPlot(color, this);
		add(histogram, constraints);
		return histogram;
	}

	public void run(String arg) {
		show(); // TODO: handle macros
	}

	public void sliderChanged(HistogramPlot.SliderEvent event) {
		if (watcher.ip instanceof ColorProcessor)
			adjustColorContrast();
		// TODO: else
	}

	interface Action {
		void perform(int i, int j, int red, int green, int blue);
	}

	// TODO: rename and make stand-alone (do not use getLeft())
	class CurrentSliceWatcher extends CurrentSlice {
		public void obsolete() {
			if (image == null)
				return;
			if (ip instanceof ColorProcessor)
				ip.reset();
			else {
				image.resetDisplayRange();
				ip.setMinAndMax(ip.getMin(), ip.getMax());
				image.updateAndDraw();
			}
			red.setValues(null);
			green.setValues(null);
			blue.setValues(null);
			gray.setValues(null);
		}

		public void changed() {
			if (ip instanceof ColorProcessor) {
				ip.snapshot();
				calculateColorPlots();
			}
			else {
				gray.setVisible(true);
				red.setVisible(false);
				green.setVisible(false);
				blue.setVisible(false);
				repaint();
			}
		}

		void forEachSnapshotPixel(Action action) {
			if (image == null)
				return;
			int[] snapshot = (int[])ip.getSnapshotPixels();
			int w = ip.getWidth();
			int h = ip.getHeight();
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {
					int v = snapshot[i + w * j];
					int red = (v >> 16) & 0xff;
					int green = (v >> 9) & 0xff;
					int blue = v & 0xff;
					action.perform(i, j, red, green, blue);
				}
		}

		void calculateColorPlots() {
			final float[] reds = new float[256];
			final float[] greens = new float[256];
			final float[] blues = new float[256];
			forEachSnapshotPixel(new Action() {
				public void perform(int i, int j,
						int r, int g, int b) {
					reds[r]++;
					greens[g]++;
					blues[b]++;
				}
			});

			gray.setVisible(false);
			red.setValues(reds);
			red.setVisible(true);
			red.repaint();
			green.setValues(greens);
			green.setVisible(true);
			green.repaint();
			blue.setValues(blues);
			blue.setVisible(true);
			blue.repaint();
		}

	}

	/* value is between 0 and 1 */
	float contrast(float value, HistogramPlot histogram) {
		float left = histogram.getLeft();
		float middle = histogram.getMiddle();
		float right = histogram.getRight();

		return value < left ? left : value > right ? right :
			(value - left) / (right - left);
	}

	/* value is between 0 and 256 */
	int contrast(int value, HistogramPlot histogram) {
		float result = contrast(value / 255f, histogram) * 255;
		return Math.max(0, Math.min(255, (int)Math.round(result)));
	}

	byte contrast(byte value, HistogramPlot histogram) {
		return (byte)contrast(value & 0xff, histogram);
	}

	void adjustColorContrast() {
		if (watcher.image == null)
			return;
		final int[] pixels = (int[])watcher.ip.getPixels();
		final int w = watcher.ip.getWidth();
		watcher.forEachSnapshotPixel(new Action() {
			public void perform(int i, int j, int r, int g, int b) {
				pixels[i + w * j] =
					(contrast(r, red) << 16) |
					(contrast(g, green) << 8) |
					contrast(b, blue);
			}
		});
		watcher.image.updateAndDraw();
	}
}
