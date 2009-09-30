import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.List;

public class Fake_Neuron
	extends ImagePlus
	implements PlugIn
{
	private int w = 320, h = 320, d = 64;
	private String title = "Fake Neuron";
	private byte[][] pixels;

	private List<int[]> pixellist;

	public Fake_Neuron()
	{
		super();
		this.pixellist = new ArrayList<int[]>();
	}

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

		paintNeuron(null, null);

		this.show();

		return;
	}

	private void paintNeuron(int a[], int z[])
	{
		int m[];
		double w = (double)this.getWidth();
		double h = (double)this.getHeight();
		double d = (double)this.getNSlices();


		if (a == null) {
			a = new int[4];
			a[0] = (int)(w * 0.1 * Math.random());
			a[1] = (int)(h * 0.1 * Math.random());
			a[2] = (int)(d * 0.1 * Math.random());
			a[3] = 5;
			this.pixellist.add(a);
			paintBall(a);
		}
		if (z == null) {
			z = new int[4];
			z[0] = (int)(w - w * 0.1 * Math.random());
			z[1] = (int)(h - h * 0.1 * Math.random());
			z[2] = (int)(d - d * 0.1 * Math.random());
			z[3] = 5;
			this.pixellist.add(z);
			paintBall(z);
		}

		System.err.print("(x,y,z)₁ = (" + a[0] + "," + a[1] + "," + a[2] + ") ");
		System.err.print("(x,y,z)₂ = (" + z[0] + "," + z[1] + "," + z[2] + ")");

		m = new int[4];

		if (Math.abs(a[0] - z[0]) < 2 && Math.abs(a[0] - z[0]) < 2 && Math.abs(a[2] - z[2]) < 2) {
			System.err.println();
			return;
		}

		for (int i = 0; i < 3; i++) {
			double offset = (double)Math.abs(a[i] - z[i]) * 0.5;
			m[i] = (int)(((double)(a[i] + z[i]) - offset) / 2.0 + offset * Math.random());
		}
		m[3] = (a[3] + z[3]) / 2;

		System.err.println(" (x,y,z)₂ = (" + m[0] + "," + m[1] + "," + m[2] + ")");

		this.pixellist.add(m);

		paintBall(m);

		if ((a[0] & m[0] & ~1) != 0 || (a[1] & m[1] & ~1) != 0 || (a[2] & m[2] & ~1) != 0)
			paintNeuron(a, m);
		if ((m[0] & z[0] & ~1) != 0 || (m[1] & m[1] & ~1) != 0 || (m[2] & m[2] & ~1) != 0)
			paintNeuron(m, z);

		return;
	}

	private void paintBall(int co[])
	{
		int x = co[0], y = co[1], z = co[2], r = co[3];
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
