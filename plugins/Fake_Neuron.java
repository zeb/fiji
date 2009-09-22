import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Fake_Neuron implements PlugIn
{
	static int width = 320, height = 320, depth = 64;
	static String title = "Fake Neuron";

	public void run(String arg)
	{
		GenericDialog gd = new GenericDialog("New Fake-Neuron");
		gd.addStringField("Title: ", title, 16);
		gd.addNumericField("Width: ", width, 0);
		gd.addNumericField("Height: ", height, 0);
		gd.addNumericField("Depth: ", depth, 0);
		gd.showDialog();

		if (gd.wasCanceled())
			return;

		title  = gd.getNextString();
		width  = (int)gd.getNextNumber();
		height = (int)gd.getNextNumber();
		depth  = (int)gd.getNextNumber();


		ImagePlus imp = IJ.createImage(title, "black", width, height, depth);

		paintBall(imp, -1, -1, -1, 10);

		imp.show();

		return;
	}

	private void paintBall(ImagePlus imp, int x, int y, int z, int r)
	{
		if (x == -1)
			x = (int)(Math.random() * imp.getWidth());
		if (y == -1)
			y = (int)(Math.random() * imp.getHeight());
		if (z == -1)
			z = (int)(Math.random() * imp.getNSlices());

		if (r == -1)
			r = 7;

		for (int x1 = Math.max(0, x - r); x1 < Math.min(imp.getWidth(), x + r + 1); x1++) {
			int r1 = (int)Math.sqrt(r * r - (x1 - x) * (x1 - x));

			for (int y1 = Math.max(0, y - r1); y1 < Math.min(imp.getHeight(), y + r1 + 1); y1++) {
				int r2 = (int)Math.sqrt(r1 * r1 - (y1 - y) * (y1 - y));

				for (int z1 = Math.max(0, z - r2); z1 < Math.min(imp.getNSlices(), z + r2 + 1); z1++) {
					set_pixel(imp, x1, y1, z1, 255);
				}
			}
		}
	}

	private void set_pixel(ImagePlus imp, int x, int y, int z, int color)
	{
		imp.setSlice(z);
		imp.getProcessor().putPixel(x, y, color);
		return;
	}


}
