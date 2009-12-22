package fiji.plugin.constrainedshapes;

import ij.process.ImageProcessor;
import fiji.util.optimization.ConjugateDirectionSearch;
import fiji.util.optimization.MinimiserMonitor;
import fiji.util.optimization.MultivariateFunction;
import fiji.util.optimization.MultivariateMinimum;
import fiji.util.optimization.OrthogonalHints;

public class GeomShapeFitter implements MultivariateFunction {

	
	/*
	 * ENUM 
	 */
	
	public static enum Method {
		CONJUGATE_DIRECTION_SEARCH; // TODO add the rest!
		
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
	
	private GeomShape shape; // Can't default.
	private ImageProcessor ip; 
	private GeomShape.EvalFunction function = GeomShape.EvalFunction.MEAN; 
	private int n_points = 500; // Default value TODO
	private MinimiserMonitor monitor = null;
	
	private double[] lower_bounds;
	private double[] upper_bounds;
	
	private Method method = Method.CONJUGATE_DIRECTION_SEARCH;
	private MultivariateMinimum optimizer;
	
	
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
		optimizer.findMinimum(this, shape.getParameters(), 0, 0, monitor);
	}
	
	
	public void setLowerBound(int n, double value) {
		this.lower_bounds[n] = value;
	}
	
	public void setUpperBound(int n, double value) {
		this.upper_bounds[n] = value;
	}
	
	
	/*
	 * MULTIVARIATEFUNCTION METHODS
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


	public OrthogonalHints getOrthogonalHints() {
		// TODO Auto-generated method stub
		return null;
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
