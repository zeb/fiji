package fiji.plugin.constrainedshapes;

/**
 * This interface aims at describing a 2D geometric shape that can be defined using
 * primitive shapes, such as circles, ellipses, ... and can be sampled over XY
 * coordinates. The 
 * 
 * @author Jean-Yves Tinevez - Dec 2009
 *
 */
public interface Sampling2DShape {

	/**
	 * Returns a <code>2 x n_points</code> array of double containing the X & Y 
	 * coordinates of an interpolation of this shape.
	 * 
	 * @param n_points  The number of point to sample this shape on
	 * @return  The X & Y coordinates
	 */
	public double[][] sample(int n_points);
	
}
