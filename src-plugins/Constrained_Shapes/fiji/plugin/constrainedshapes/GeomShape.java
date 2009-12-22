package fiji.plugin.constrainedshapes;

import java.awt.Shape;
import java.util.ArrayList;

import ij.process.ImageProcessor;

/**
 * This interface aims at describing a 2D geometric shape that can be defined using
 * primitive shapes, such as circles, ellipses, ... and can be sampled over XY
 * coordinates. The 
 * 
 * @author Jean-Yves Tinevez - Dec 2009
 *
 */
public abstract class GeomShape implements Shape, Cloneable {

	/*
	 * INNER CLASSES & ENUMS
	 */
	
	/**
	 * Enum to specify how we compute an energy value from a list of pixel values.
	 * If the usage requires it, we can make this a proper interface with taylored 
	 * implementing classes.
	 */
	public static enum EvalFunction  {
		/** Will return the mean of the pixel intensities, which will minimize it. */
		MEAN,
		/** Will return the opposite of the pixel value mean, effectively maximizing it. */
		MINUS_MEAN;
		public float compute(final float[] pixel_list)  {			
			float result = 0.0f;
			switch (this) {
			case MEAN:
				result = pixel_list[0];
				for (int i = 1; i < pixel_list.length; i++) {
					result += pixel_list[i];
				}
				result /= pixel_list.length;
				break;
			case MINUS_MEAN:
				result = - pixel_list[0];
				for (int i = 1; i < pixel_list.length; i++) {
					result -= pixel_list[i];
				}
				result /= pixel_list.length;
				break;
			}
			return result;
		}
	}
	
	
	/**
	 * Returns a <code>2 x n_points</code> array of double containing the X & Y 
	 * coordinates of an interpolation of this shape.
	 * 
	 * @param n_points  The number of point to sample this shape on
	 * @return  The X & Y coordinates
	 */
	public abstract double[][] sample(int n_points);

	/**
	 * Return the number of parameters needed to entirely specify this shape. This 
	 * will be used e.g. by {@link GeomShapeFitter} to adapt prepare the 
	 * optimizer for the fit.
	 * @see {@link #getParameters()}, {@link #setParameters(double[])}
	 */
	public abstract int getNumParameters();
	
	/**
	 * Return the parameters that specify entirely this shape as a souble array.
	 * @see {@link #getNumParameters()}, {@link #setParameters(double[])}
	 */
	public abstract double[] getParameters();

	/**
	 * Specify the parameter needed to describe entirely this shape.
	 * @see {@link #getParameters()}, {@link #getNumParameters()}
	 */
	public abstract void setParameters(double[] params);
	
	public abstract GeomShape clone();
	
	/*
	 * PUBLIC METHODS
	 */
	
	// SHOULD IT BE A TWOCIRCLEROI METHOD? TODO
	public float eval(final ImageProcessor ip, final EvalFunction function, final int n_points) {
		final float[][] pixels_as_float = ip.getFloatArray();
		final int width = pixels_as_float.length;
		final int height = pixels_as_float[0].length;
		final double[][] xy = sample(n_points);
		final double[] x = xy[0];
		final double[] y = xy[1];
		final ArrayList<Float> pixels = new ArrayList<Float>(x.length);
		int ix, iy;
		for (int i = 0; i < x.length; i++) {
			ix = (int) Math.round(x[i]);
			iy = (int) Math.round(y[i]);
			if (ix<0 || ix>width || iy<0 || iy>height) {continue;}
			pixels.add( pixels_as_float[ix][iy] );
		}
		final float[] pixel_list = new float[pixels.size()];
		for (int i = 0; i < pixel_list.length; i++) {
			pixel_list[i] = pixels.get(i);
		}
		return function.compute(pixel_list);
	}	


	
}
