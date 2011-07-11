package fiji.plugin.constrainedshapes;

import java.awt.Shape;

import java.util.ArrayList;
import java.util.Arrays;

import ij.process.ImageProcessor;

/**
 * This interface aims at describing a 2D geometric shape that can be defined using
 * primitive shapes, such as circles, ellipses, ... and can be sampled over XY
 * coordinates. The
 *
 * @author Jean-Yves Tinevez - Dec 2009
 *
 */
public abstract class ParameterizedShape implements Shape, Cloneable {
	protected double[] lowerBounds, upperBounds;

	public ParameterizedShape() {
		lowerBounds = new double[getNumParameters()];
		upperBounds = new double[getNumParameters()];
		Arrays.fill(lowerBounds, Double.NEGATIVE_INFINITY);
		Arrays.fill(upperBounds, Double.POSITIVE_INFINITY);
	}

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
		public float compute(final float[] pixelList)  {
			float result = 0.0f;
			switch (this) {
			case MEAN:
				result = pixelList[0];
				for (int i = 1; i < pixelList.length; i++) {
					result += pixelList[i];
				}
				result /= pixelList.length;
				break;
			case MINUS_MEAN:
				result = - pixelList[0];
				for (int i = 1; i < pixelList.length; i++) {
					result -= pixelList[i];
				}
				result /= pixelList.length;
				break;
			}
			return result;
		}
	}


	/**
	 * Returns a <code>2 x nPoints</code> array of double containing the X &amp; Y
	 * coordinates of an interpolation of this shape.
	 *
	 * @param nPoints  The number of point to sample this shape on
	 * @return  The X &amp; Y coordinates
	 */
	public abstract double[][] sample(int nPoints);

	/**
	 * Return the number of parameters needed to entirely specify this shape. This
	 * will be used e.g. by {@link ShapeFitter} to adapt prepare the
	 * optimizer for the fit.
	 * @see {@link #getParameters()}, {@link #setParameters(double[])}
	 */
	public abstract int getNumParameters();

	/**
	 * Return a reference to the parameters that specify entirely this shape as a double array.
	 * <p>
	 * By contract, this array is sufficient to determine entirely the shape, and modifying
	 * it, even through a reference, must modify the shape.
	 *
	 * @see {@link #getNumParameters()}, {@link #setParameters(double[])}
	 */
	public abstract double[] getParameters();

	/**
	 * Specify the parameters needed to describe entirely this shape.
	 * <p>
	 * By contract, this method must internally <strong>replace</strong>
	 * the array describing the shape by the new one given in argument.
	 * Previous references to the parameter
	 * array would then be invalidated by calling this method.
	 * @see {@link #getParameters()}, {@link #getNumParameters()}
	 */
	public abstract void setParameters(double[] params);

	/**
	 * Set the <code>n</code>th parameter lower bound to be <code>value</code>.
	 * @see {@link #setLowerBounds(double[])}, {@link #setUpperBounds(double[])}, {@link #setUpperBound(int, double)}
	 */
	public void setLowerBound(int n, double value) {
		this.lowerBounds[n] = value;
	}

	/**
	 * Set the <code>n</code>th parameter upper bound to be <code>value</code>.
	 * @see {@link #setLowerBounds(double[])}, {@link #setUpperBounds(double[])}, {@link #setLowerBound(int, double)}
	 */
	public void setUpperBound(int n, double value) {
		this.upperBounds[n] = value;
	}

	/**
	 * <string>Replace</string> the lower bound array by the one in argument. No error check are made
	 * if the array size is not correct.
	 * @see {@link #setUpperBounds(double[])}, {@link #setLowerBound(int, double)}, {@link #setLowerBound(int, double)}
	 */
	public void setLowerBounds(double[] arr) {
		this.lowerBounds = arr;
	}

	/**
	 * <string>Replace</string> the upper bound array by the one in argument. No error check are made
	 * if the array size is not correct.
	 * @see {@link #setLowerBounds(double[])}, {@link #setLowerBound(int, double)}, {@link #setLowerBound(int, double)}
	 */
	public void setUpperBounds(double[] arr) {
		this.upperBounds = arr;
	}

	public double getLowerBound(int n) {
		return lowerBounds[n];
	}

	public double getUpperBound(int n) {
		return upperBounds[n];
	}

	/**
	 * Return a copy of this shape. The copy will be made by instantiating a new object, and
	 * setting its parameter array to be a copy that of this object.
	 */
	public abstract ParameterizedShape clone();

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Compute an evaluation of this shape on the given {@link ImageProcessor}, using
	 * the evaluating function {@link EvalFunction}.
	 * <p>
	 * This shape is first sampled over <code>nPoints</code> using the concrete method
	 * {@link #sample(int)}. This method returns a list of pixel coordinates that define a sampling
	 * of this geometrical shape over <code>nPoints</code>.
	 * Pixel values for these coordinates are then retrieved, and a value is computed through the
	 * {@link EvalFunction} <code>function</code>, and returned by this method.
	 *
	 *  @param ip  The {@link ImageProcessor} to evaluate this shape on
	 *  @param function  The {@link EvalFunction} that will give a value from a pixel list
	 *  @param nPoints  The number of points to sample this shape on
	 *  @return  The calculated value
	 */
	public float eval(final ImageProcessor ip, final EvalFunction function, final int nPoints) {
		final float[][] floatPixels = ip.getFloatArray();
		final int width = floatPixels.length;
		final int height = floatPixels[0].length;
		final double[][] xy = sample(nPoints);
		final double[] x = xy[0];
		final double[] y = xy[1];
		final ArrayList<Float> pixels = new ArrayList<Float>(x.length);
		int ix, iy;
		for (int i = 0; i < x.length; i++) {
			ix = (int) Math.round(x[i]);
			iy = (int) Math.round(y[i]);
			if (ix<0 || ix>width || iy<0 || iy>height) {continue;}
			pixels.add( floatPixels[ix][iy] );
		}
		final float[] pixelList = new float[pixels.size()];
		for (int i = 0; i < pixelList.length; i++) {
			pixelList[i] = pixels.get(i);
		}
		return function.compute(pixelList);
	}
}