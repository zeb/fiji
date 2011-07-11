package fiji.plugin.constrainedshapes;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

public class CircleStroke implements Stroke {
	protected float size;

	public CircleStroke(float size) {
		this.size = size;
	}

	public Shape createStrokedShape(final Shape p) {
		GeneralPath path = new GeneralPath();
		final PathIterator pi = p.getPathIterator(null);
		int segmentType;
		float[] coords = new float[6];
		float x = 0;
		float y = 0;
		while (!pi.isDone()) {
			segmentType = pi.currentSegment(coords);
			switch (segmentType) {
			case PathIterator.SEG_LINETO:
			case PathIterator.SEG_MOVETO:
				x = coords[0];
				y = coords[1];
				path.append(convolve(x,y), false);
				break;
			case PathIterator.SEG_QUADTO:
				x = coords[2];
				y = coords[3];
				path.append(convolve(x,y), false);
				break;
			case PathIterator.SEG_CUBICTO:
				x = coords[4];
				y = coords[5];
				path.append(convolve(x,y), false);
				break;
			case PathIterator.SEG_CLOSE:
				break;
			}
			pi.next();
		}
		return path;
	}

	protected Shape convolve(final float x, final float y) {
		return new Ellipse2D.Float(x-size/2, y-size/2, size, size);
	}
}