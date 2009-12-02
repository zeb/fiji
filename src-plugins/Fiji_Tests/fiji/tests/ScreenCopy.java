package fiji.tests;

import fiji.Main;
import ij.ImagePlus;

import java.awt.Component;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;


public class ScreenCopy
{
	private Component component = null;
	private int width = -1, height = -1;
	private ImagePlus image;

	public ScreenCopy(String path)
		throws java.awt.AWTException
	{
		this(fiji.Main.getComponent(path));
	}

	public ScreenCopy(Component component)
		throws java.awt.AWTException
	{
		Rectangle rect;
		Robot robot;
		Image capture;

		this.component = component;

		robot = new Robot();
		rect  = new Rectangle(this.component.getLocationOnScreen(),
				      this.component.getSize());

		capture = robot.createScreenCapture(rect);
		this.image = new ImagePlus("ScreenCopy", capture);

		this.width  = this.image.getWidth();
		this.height = this.image.getHeight();
	}

	public int[][] getGravCenters()
	{
		long rx = 0, ry = 0;
		long gx = 0, gy = 0;
		long bx = 0, by = 0;
		int pixels[];
		int gravities[][] = new int[3][2];

		pixels = (int[])this.image.getProcessor().getPixels();

		for (int i = 0; i < pixels.length; i++) {
			long red   = (pixels[i] >> 16) & 0xff;
			long green = (pixels[i] >>  8) & 0xff;
			long blue  = (pixels[i]      ) & 0xff;

			int x = i % this.width;
			int y = i / this.width;

			rx += x * red;   ry += y * red;
			gx += x * green; gy += y * green;
			bx += x * blue;  by += y * blue;
		}

		rx /= pixels.length;
		ry /= pixels.length;
		gx /= pixels.length;
		gy /= pixels.length;
		bx /= pixels.length;
		by /= pixels.length;

		gravities[0][0] = (int)rx; gravities[0][1] = (int)ry;
		gravities[1][0] = (int)gx; gravities[1][1] = (int)gy;
		gravities[2][0] = (int)bx; gravities[2][1] = (int)by;

		return gravities;
	}

/*	public int[][][]*/
}
