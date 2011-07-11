package fiji.plugin.constrainedshapes;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

public class SquareStroke implements Stroke {
	protected int size;

	/*
	 * CONSTRUCTOR
	 */

	public SquareStroke(int size) {
		this.size = size;
	}

	public Shape createStrokedShape(final Shape p) {
		GeneralPath path = new GeneralPath();
		final PathIterator pi = p.getPathIterator(null);
		int segmentType;
		float[] coords = new float[6];
		int x = 0;
		int y = 0;
		while (!pi.isDone()) {
			segmentType = pi.currentSegment(coords);
			switch (segmentType) {
			case PathIterator.SEG_LINETO:
			case PathIterator.SEG_MOVETO:
				x = (int) coords[0];
				y = (int) coords[1];
				path.append(convolve(x,y), false);
				break;
			case PathIterator.SEG_QUADTO:
				x = (int) coords[2];
				y = (int) coords[3];
				path.append(convolve(x,y), false);
				break;
			case PathIterator.SEG_CUBICTO:
				x = (int) coords[4];
				y = (int) coords[5];
				path.append(convolve(x,y), false);
				break;
			case PathIterator.SEG_CLOSE:
				break;
			}
			pi.next();
		}
		return path;
	}

	protected Shape convolve(final int x, final int y) {
		return new Rectangle(x-size/2, y-size/2, size, size);
	}
}