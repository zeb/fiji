package fiji.plugin.constrainedshapes;

import java.awt.geom.PathIterator;

public class SamplingPathIterator implements PathIterator {
	
	/*
	 * FIELDS 
	 */
	Sampling2DShape shape;
	double[] x;
	double[] y;
	int current_index, n_points;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public SamplingPathIterator(Sampling2DShape _shape, int n) {
		this.shape = _shape;
		this.n_points = n;
		double[][] xy = shape.sample(n);
		this.x = xy[0];
		this.y = xy[1];
		this.current_index = 0;
	}

	
	/*
	 * PATHITERATOR METHODS
	 */
	
	public int currentSegment(float[] coords) {
		coords[0] = (float) x[current_index];
		coords[1] = (float) y[current_index];
		coords[2] = 0.0f;
		coords[3] = 0.0f;
		coords[4] = 0.0f;
		coords[5] = 0.0f;
		int segment_type;
		if (current_index == 0)			{ 
			segment_type = PathIterator.SEG_MOVETO; 
		} else {
			segment_type = PathIterator.SEG_LINETO; 
		}
		return segment_type;
	}

	public int currentSegment(double[] coords) {
		coords[0] = x[current_index];
		coords[1] = y[current_index];
		coords[2] = 0.0;
		coords[3] = 0.0;
		coords[4] = 0.0;
		coords[5] = 0.0;
		int segment_type;
		if (current_index == 0)			{ 
			segment_type = PathIterator.SEG_MOVETO; 
		} else {
			segment_type = PathIterator.SEG_LINETO; 
		}
		return segment_type;
	}

	public int getWindingRule() {
		return PathIterator.WIND_EVEN_ODD;
	}

	public boolean isDone() {
		return current_index >= n_points;
	}

	public void next() {
		current_index++;
	}

}
