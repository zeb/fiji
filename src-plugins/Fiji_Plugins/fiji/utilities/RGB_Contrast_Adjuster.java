package fiji.utilities;

import ij.ImagePlus;

import ij.plugin.frame.PlugInFrame;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;

public class RGB_Contrast_Adjuster extends PlugInFrame
		implements HistogramPlot.SliderEventListener {
	public RGB_Contrast_Adjuster() {
		super("RGB Contrast Adjuster");
		HistogramPlot histogram = new HistogramPlot(new float[] {
				1, 3, 2
			}, Color.RED);
		histogram.addListener(this);
		add(histogram);
		pack();
	}

	public void run(String arg) {
		show(); // TODO: handle macros
	}

	public void sliderChanged(HistogramPlot.SliderEvent event) {
System.err.println("event: " + event.sliderLeft);
	}
}
