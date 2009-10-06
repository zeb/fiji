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

		this.setStack(t, stack);

		int p0[] = new int[4], p1[] = new int[4];

		p0[0] = (int)(x * 0.1 * Math.random());
		p0[1] = (int)(y * 0.1 * Math.random());
		p0[2] = (int)(z * 0.1 * Math.random());
		p0[3] = 5;

		p1[0] = (int)(x - x * 0.1 * Math.random());
		p1[1] = (int)(y - y * 0.1 * Math.random());
		p1[2] = (int)(z - z * 0.1 * Math.random());
		p1[3] = 5;


		this.pixellist.add(p0);
		buildNeuron(p0, p1);
		this.pixellist.add(p1);

		for (int p[] : this.pixellist)
			paintBall(p);

		this.show();

		return;
	}

	private void buildNeuron(int a[], int z[])
	{
		int m[] = new int[4];
		double w = (double)this.getWidth();
		double h = (double)this.getHeight();
		double d = (double)this.getNSlices();

		for (int i = 0; i < 3; i++) {
			double offset = (double)Math.abs(a[i] - z[i]) * 0.5;
			m[i] = (int)(((double)(a[i] + z[i]) - offset) / 2.0 + offset * Math.random());
		}
		m[3] = (a[3] + z[3]) / 2;

		if (Math.abs(a[0] - m[0]) > 1 || Math.abs(a[1] - m[1]) > 1 || Math.abs(a[2] - m[2]) > 1)
			buildNeuron(a, m);

		this.pixellist.add(m);

		if (Math.abs(m[0] - z[0]) > 1 || Math.abs(m[1] - z[1]) > 1 || Math.abs(m[2] - z[2]) > 1)
			buildNeuron(m, z);

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

				for (int z1 = Math.max(0, z - r2); z1 < Math.min(this.getNSlices(), z + r2 + 1); z1++)
					set_pixel(x1, y1, z1, intensity(distance(co, x1, y1, z1), (double)r));
			}
		}
	}

	private double distance(int a[], int x1, int y1, int z1)
	{
		double x = (double)(a[0] - x1);
		double y = (double)(a[1] - y1);
		double z = (double)(a[2] - z1);

		return Math.sqrt(x*x + y*y + z*z);
	}

	private byte intensity(double dist, double r)
	{
		return (byte)((dist * dist) - dist * r - 255.0 * dist / r + 255.0);
	}

	private void set_pixel(int x, int y, int z, byte color)
	{
		this.pixels[z][y * this.width + x] = (byte)Math.max(
				(int)color, (int)this.pixels[z][y * this.width + x]);
		return;
	}
}
