package fiji.plugin.constrainedshapes;

import java.awt.geom.PathIterator;

public class SamplingPathIterator implements PathIterator {
	protected ParameterizedShape shape;
	protected double[] x;
	protected double[] y;
	protected int currentIndex, nPoints;

	/*
	 * CONSTRUCTORS
	 */

	public SamplingPathIterator(ParameterizedShape shape, int n) {
		this.shape = shape;
		this.nPoints = n;
		double[][] xy = shape.sample(n);
		this.x = xy[0];
		this.y = xy[1];
		this.currentIndex = 0;
	}


	/*
	 * PATHITERATOR METHODS
	 */

	public int currentSegment(float[] coords) {
		coords[0] = (float) x[currentIndex];
		coords[1] = (float) y[currentIndex];
		coords[2] = 0.0f;
		coords[3] = 0.0f;
		coords[4] = 0.0f;
		coords[5] = 0.0f;
		int segmentType;
		if (currentIndex == 0)			{
			segmentType = PathIterator.SEG_MOVETO;
		} else {
			segmentType = PathIterator.SEG_LINETO;
		}
		return segmentType;
	}

	public int currentSegment(double[] coords) {
		coords[0] = x[currentIndex];
		coords[1] = y[currentIndex];
		coords[2] = 0.0;
		coords[3] = 0.0;
		coords[4] = 0.0;
		coords[5] = 0.0;
		int segmentType;
		if (currentIndex == 0)			{
			segmentType = PathIterator.SEG_MOVETO;
		} else {
			segmentType = PathIterator.SEG_LINETO;
		}
		return segmentType;
	}

	public int getWindingRule() {
		return PathIterator.WIND_EVEN_ODD;
	}

	public boolean isDone() {
		return currentIndex >= nPoints;
	}

	public void next() {
		currentIndex++;
	}
}