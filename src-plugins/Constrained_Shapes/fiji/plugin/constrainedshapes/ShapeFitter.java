package fiji.plugin.constrainedshapes;

import ij.process.ImageProcessor;

import pal.math.ConjugateDirectionSearch;
import pal.math.MinimiserMonitor;
import pal.math.MultivariateFunction;
import pal.math.MultivariateMinimum;
import pal.math.OrthogonalHints;

public class ShapeFitter implements MultivariateFunction {
	/**
	 * This is used as a factory to return an optimization method. As now,
	 * since there is only one working optimizer in the PAL package, there
	 * is only one method in this enum.
	 */
	public static enum Method {
		CONJUGATE_DIRECTION_SEARCH; // Unfortunately so far, the only one that works here

		public MultivariateMinimum instantiate(ParameterizedShape shape) {
			MultivariateMinimum optimizer = null;
			switch (this) {
			case CONJUGATE_DIRECTION_SEARCH:
				optimizer = new ConjugateDirectionSearch();
				break;
			}
			return optimizer;
		}
	}

	protected int nPoints = 500;
	/**
	 * Sets the desired precision of the optimization process.
	 * These 2 numbers set the desired number of digits after
	 * dot of parameters and function.
	 */
	protected static final int PARAMETER_PRECISION = 2;
	protected static final int FUNCTION_PRECISION = 2;

	protected ParameterizedShape shape; // Can't be null because of constructor.
	protected ImageProcessor ip;
	protected ParameterizedShape.EvalFunction function = ParameterizedShape.EvalFunction.MEAN;
	protected MinimiserMonitor monitor = null;

	protected Method method = Method.CONJUGATE_DIRECTION_SEARCH;
	protected MultivariateMinimum optimizer;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * No empty constructor, for we need to initialize the fitter in a way
	 * that depends on the {@link ParameterizedShape} it will work on.
	 */
	public ShapeFitter(ParameterizedShape shape) {
		setShape(shape);
	}

	/*
	 * PUBLIC METHOD
	 */

	/**
	 * Optimize the current {@link ParameterizedShape} of this instance, on the current
	 * {@link ImageProcessor}, using the current {@link Method}.
	 * <p>
	 * This method operate on the {@link ParameterizedShape} object which reference was
	 * set by {@link #setShape(ParameterizedShape)},
	 * and will <strong>modify</strong> it. To retrieve the found minimum, use the
	 * {@link #getShape()} method. Internally, the optimizer accesses the parameter
	 * array of the shape to optimize it, through the {@link ParameterizedShape#getParameters()}.
	 * Since this is a reference to the double array of the shape, optimizing it
	 * will modify the shape.
	 * <p>
	 * If the starting point {@link ParameterizedShape} or the {@link ImageProcessor} objects are
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

	/*
	 * MULTIVARIATEFUNCTION METHODS
	 */

	/**
	 * Evalute the {@link ParameterizedShape} of this instance over the parameters given in argument.
	 * <p>
	 * This is done first throught the {@link ParameterizedShape#setParameters(double[])} method, then by
	 * calling the {@link ParameterizedShape#eval(ImageProcessor, ParameterizedShape.EvalFunction, int)}
	 * method, using the {@link ImageProcessor} and {@link ParameterizedShape.EvalFunction} of this object.
	 * @see {@link #setImageProcessor(ImageProcessor)}, {@link #setFunction(ParameterizedShape.EvalFunction)}
	 */
	public double evaluate(double[] params) {
		shape.setParameters(params);
		return shape.eval(ip, function, nPoints);
	}

	public double getLowerBound(int n) {
		return shape.getLowerBound(n);
	}

	public int getNumArguments() {
		return shape.getNumParameters();
	}

	public double getUpperBound(int n) {
		return shape.getUpperBound(n);
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

	public ParameterizedShape getShape() {	return shape;	}
	public ParameterizedShape.EvalFunction getFunction() {		return function;	}
	public void setFunction(ParameterizedShape.EvalFunction function) {		this.function = function;	}
	public int getNPoints() {		return nPoints; 	}
	public void setNPoints(int nPoints) {		nPoints = nPoints; 	}
	public ImageProcessor getImageProcessor() { 		return ip; 	}
	public void setImageProcessor(ImageProcessor ip) { 		this.ip = ip; 	}
	public Method getMethod() {		return method;	}
	public void setMonitor(MinimiserMonitor monitor) {		this.monitor = monitor;	}
	public MinimiserMonitor getMonitor() {		return monitor;	}

	/**
	 * Set the {@link Method} used to optimize the {@link ParameterizedShape} by this fitter.
	 * Using this method will reset the internal {@link MultivariateMinimum} optimizer
	 * of this instance.
	 * @param method  The method to use for optimization
	 */
	public void setMethod(Method method) {
		this.optimizer = method.instantiate(shape);
		this.method = method;
	}

	/**
	 * Set the {@link ParameterizedShape} to optimize by this instance. Using this method will
	 * reset the upper and lower bounds for this fitter, as well as the internal
	 * {@link MultivariateMinimum} optimizer function.
	 * @param shape  The shape to optimize
	 */
	public void setShape(ParameterizedShape shape) {
		this.optimizer = method.instantiate(shape);
		this.shape = shape;
	}
}