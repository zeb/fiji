import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Fake_Neuron
	extends ImagePlus
	implements PlugIn
{
	private int w = 320, h = 320, d = 64;
	private String title = "Fake Neuron";
	private byte[][] pixels;

	public void run(String args)
	{
		ImageStack stack;
		ImageProcessor ip;
		String t = "Fake Neuron";
		int x = 320, y = 320, z = 64;

		GenericDialog gd = new GenericDialog("New Fake-Neuron");
		gd.addStringField("Title: ", t, 16);
		gd.addNumericField("Width: ", x, 0);
		gd.addNumericField("Height: ", y, 0);
		gd.addNumericField("Depth: ", z, 0);
		gd.showDialog();

		if (gd.wasCanceled())
			return;

		t =      gd.getNextString();
		x = (int)gd.getNextNumber();
		y = (int)gd.getNextNumber();
		z = (int)gd.getNextNumber();

		/* zero-initialized, thus "black": perfect */
		this.pixels = new byte[z][x * y];

		stack = new ImageStack(x, y, z);

		for (int i = 0; i < z; i++)
			stack.setPixels(this.pixels[i], i+1);

		this.setStack(title, stack);

		paintBall((int)(Math.random() * x),
				(int)(Math.random() * y),
				(int)(Math.random() * z), 10);

		this.show();

		return;
	}

	private void paintBall(int x, int y, int z, int r)
	{
		/* Could someone tell me how to indent this nicely to < 80
		 * characters without completely ripping it apart? */

		for (int x1 = Math.max(0, x - r); x1 < Math.min(this.getWidth(),
					x + r + 1); x1++) { int r1 =
			(int)Math.sqrt(r * r - (x1 - x) * (x1 - x));

			for (int y1 = Math.max(0, y - r1); y1 < Math.min(this.getHeight(), y + r1 + 1); y1++) {
				int r2 = (int)Math.sqrt(r1 * r1 - (y1 - y) * (y1 - y));

				for (int z1 = Math.max(0, z - r2); z1 < Math.min(this.getNSlices(), z + r2 + 1); z1++) {
					set_pixel(x1, y1, z1, (byte)255);
				}
			}
		}
	}

	private void set_pixel(int x, int y, int z, byte color)
	{
		this.pixels[z][y * this.width + x] = color;
		return;
	}
}
