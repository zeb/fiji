package fiji.plugin.constrainedshapes;

import ij.process.ImageProcessor;

import pal.math.ConjugateDirectionSearch;
import pal.math.MinimiserMonitor;
import pal.math.MultivariateFunction;
import pal.math.MultivariateMinimum;
import pal.math.OrthogonalHints;

public class GeomShapeFitter implements MultivariateFunction {

	/*
	 * ENUM 
	 */
	
	/**
	 * This is used as a factory to return an optimization method. As now,
	 * since there is only one working optimizer in the PAL package, there 
	 * is only one method in this enum. 
	 */
	public static enum Method {
		CONJUGATE_DIRECTION_SEARCH; // Unfortunately so far, the only one that works here 
		
		public MultivariateMinimum instantiate(GeomShape shape) {
			MultivariateMinimum optimizer = null;
			switch (this) {
			case CONJUGATE_DIRECTION_SEARCH:
				optimizer = new ConjugateDirectionSearch();
				break;
			}
			return optimizer;
		}
	}
	
	/*
	 * FIELDS
	 */
	
	private int n_points = 500;
	/** 
	 * Sets the desired precision of the optimization process. 
	 * These 2 numbers set the desired number of digits after 
	 * dot of parameters and function.
	 */
	private static final int PARAMETER_PRECISION = 2;
	private static final int FUNCTION_PRECISION = 2;
	
	private GeomShape shape; // Can't be null because of constructor.
	private ImageProcessor ip; 
	private GeomShape.EvalFunction function = GeomShape.EvalFunction.MEAN; 
	private MinimiserMonitor monitor = null;
	
	private double[] lower_bounds;
	private double[] upper_bounds;
	
	private Method method = Method.CONJUGATE_DIRECTION_SEARCH;
	private MultivariateMinimum optimizer;
	
	/*
	 * CONSTRUCTOR
	 */
	
	/**
	 * No empty constructor, for we need to initialize the fitter in a way 
	 * that depends on the {@link GeomShape} it will work on. 
	 */
	public GeomShapeFitter(GeomShape _shape) {
		setShape(_shape);
	}
		
	/*
	 * PUBLIC METHOD
	 */
	
	/**
	 * Optimize the current {@link GeomShape} of this instance, on the current 
	 * {@link ImageProcessor}, using the current {@link Method}.
	 * <p>
	 * This method operate on the {@link GeomShape} object which reference was 
	 * set by {@link #setShape(GeomShape)},
	 * and will <strong>modify</strong> it. To retrieve the found minimum, use the 
	 * {@link #getShape()} method. Internally, the optimizer accesses the parameter 
	 * array of the shape to optimize it, through the {@link GeomShape#getParameters()}.
	 * Since this is a reference to the double array of the shape, optimizing it
	 * will modify the shape.
	 * <p>
	 * If the starting point {@link GeomShape} or the {@link ImageProcessor} objects are
	 * not set, this method does nothing and return. 
	 */
	public void optimize() {
		if ( (shape==null) || (ip==null)) { return; }
		optimizer.findMinimum(
				this, 
				shape.getParameters(), 
				FUNCTION_PRECISION, 
				PARAMETER_PRECISION, 
				monitor);
	}
	
	/**
	 * Set the <code>n</code>th parameter lower bound to be <code>value</code>.
	 * @see {@link #setLowerBounds(double[])}, {@link #setUpperBounds(double[])}, {@link #setUpperBound(int, double)}
	 */
	public void setLowerBound(int n, double value) {
		this.lower_bounds[n] = value;
	}
	
	/**
	 * Set the <code>n</code>th parameter upper bound to be <code>value</code>.
	 * @see {@link #setLowerBounds(double[])}, {@link #setUpperBounds(double[])}, {@link #setLowerBound(int, double)}
	 */
	public void setUpperBound(int n, double value) {
		this.upper_bounds[n] = value;
	}
	
	/**
	 * <string>Replace</string> the lower bound array by the one in argument. No error check are made
	 * if the array size is not correct. 
	 * @see {@link #setUpperBounds(double[])}, {@link #setLowerBound(int, double)}, {@link #setLowerBound(int, double)}
	 */
	public void setLowerBounds(double[] arr) {
		this.lower_bounds = arr;
	}

	/**
	 * <string>Replace</string> the upper bound array by the one in argument. No error check are made
	 * if the array size is not correct. 
	 * @see {@link #setLowerBounds(double[])}, {@link #setLowerBound(int, double)}, {@link #setLowerBound(int, double)}
	 */
	public void setUpperBounds(double[] arr) {
		this.upper_bounds = arr;
	}
	
	/*
	 * MULTIVARIATEFUNCTION METHODS
	 */
	
	/**
	 * Evalute the {@link GeomShape} of this instance over the parameters given in argument.
	 * <p>
	 * This is done first throught the {@link GeomShape#setParameters(double[])} method, then by
	 * calling the {@link GeomShape#eval(ImageProcessor, GeomShape.EvalFunction, int)}
	 * method, using the {@link ImageProcessor} and {@link GeomShape.EvalFunction} of this object.
	 * @see {@link #setImageProcessor(ImageProcessor)}, {@link #setFunction(GeomShape.EvalFunction)}
	 */
	public double evaluate(double[] params) {
		shape.setParameters(params);
		return shape.eval(ip, function, n_points);
	}

	public double getLowerBound(int n) {		
		return lower_bounds[n];
	}

	public int getNumArguments() {
		return shape.getNumParameters();
	}

	public double getUpperBound(int n) {
		return upper_bounds[n];
	}

	/**
	 * Return a hint for Orthogonal optimizers. Since we are not using these, we simply return the 
	 * null hint.
	 * @return  An object that has no hints.
	 */
	public OrthogonalHints getOrthogonalHints() {
		return OrthogonalHints.Utils.getNull();
	}
	
	/*
	 * GETTERS AND SETTERS
	 */

	public GeomShape getShape() {	return shape;	}
	public GeomShape.EvalFunction getFunction() {		return function;	}
	public void setFunction(GeomShape.EvalFunction function) {		this.function = function;	}
	public int getNPoints() {		return n_points; 	}
	public void setNPoints(int nPoints) {		n_points = nPoints; 	}
	public ImageProcessor getImageProcessor() { 		return ip; 	}
	public void setImageProcessor(ImageProcessor ip) { 		this.ip = ip; 	}
	public Method getMethod() {		return method;	}
	public void setMonitor(MinimiserMonitor monitor) {		this.monitor = monitor;	}
	public MinimiserMonitor getMonitor() {		return monitor;	}

	/**
	 * Set the {@link Method} used to optimize the {@link GeomShape} by this fitter. 
	 * Using this method will reset the internal {@link MultivariateMinimum} optimizer
	 * of this instance.
	 * @param _method  The method to use for optimization
	 */
	public void setMethod(Method _method) {		
		this.optimizer = _method.instantiate(shape);
		this.method = _method;	
	}

	/**
	 * Set the {@link GeomShape} to optimize by this instance. Using this method will
	 * reset the upper and lower bounds for this fitter, as well as the internal
	 * {@link MultivariateMinimum} optimizer function.
	 * @param _shape  The shape to optimize
	 */
	public void setShape(GeomShape _shape) {	
		final double[] arr1 = new double[_shape.getNumParameters()];
		final double[] arr2 = new double[_shape.getNumParameters()];
		for (int i = 0; i < _shape.getNumParameters(); i++) {
			arr1[i] = Double.NEGATIVE_INFINITY;
			arr2[i] = Double.POSITIVE_INFINITY;
		}
		this.optimizer = method.instantiate(_shape);
		this.lower_bounds = arr1;
		this.upper_bounds = arr2;
		this.shape = _shape;		
	}

}
