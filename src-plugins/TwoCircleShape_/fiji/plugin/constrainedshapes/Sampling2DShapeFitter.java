package fiji.plugin.constrainedshapes;

import ij.process.ImageProcessor;
import pal.math.ConjugateDirectionSearch;
import pal.math.ConjugateGradientSearch;
import pal.math.DifferentialEvolution;
import pal.math.MultivariateFunction;
import pal.math.MultivariateMinimum;
import pal.math.OrthogonalSearch;
import pal.math.StochasticOSearch;

public class Sampling2DShapeFitter implements MultivariateFunction {

	
	/*
	 * ENUM
	 */
	
	public static enum Method {
		CONJUGATE_DIRECTION_SEARCH,
		CONJUGATE_GRADIENT_SEARCH,
		DIFFERENTIAL_EVOLUTION,
		ORTHOGONAL_SEARCH,
		STOCHASTIC_ORTHOGONAL_SEARCH;
		
		public MultivariateMinimum instantiate(final Sampling2DShape shape) {
			MultivariateMinimum optimizer = null;
			switch (this) {
			case CONJUGATE_DIRECTION_SEARCH:
				optimizer = new ConjugateDirectionSearch();
				break;
			case CONJUGATE_GRADIENT_SEARCH:
				optimizer = new ConjugateGradientSearch(2); // BEALE_SORENSON_HESTENES_STIEFEL_UPDATE
				break;
			case DIFFERENTIAL_EVOLUTION:
				optimizer = new DifferentialEvolution(shape.getNumParameters());
				break;
			case ORTHOGONAL_SEARCH:
				optimizer = new OrthogonalSearch();
				break;
			case STOCHASTIC_ORTHOGONAL_SEARCH:
				optimizer = new StochasticOSearch();
				break;				
			}
			return optimizer;
		}
	}
	
	/*
	 * FIELDS
	 */
	
	private Sampling2DShape shape; // Can't default.
	private ImageProcessor ip; // Can't default.
	private Sampling2DShape.EvalFunction function = Sampling2DShape.EvalFunction.MEAN; 
	private int n_points = 500; // Default value
	
	private double[] lower_bounds;
	private double[] upper_bounds;
	
	private Method method = Method.CONJUGATE_DIRECTION_SEARCH;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public Sampling2DShapeFitter(Sampling2DShape _shape, ImageProcessor _ip) {
		this.shape = _shape;
		this.ip = _ip;
		lower_bounds = new double[shape.getNumParameters()];
		upper_bounds = new double[shape.getNumParameters()];
		for (int i = 0; i < shape.getNumParameters(); i++) {
			lower_bounds[i] = Double.NEGATIVE_INFINITY;
			upper_bounds[i] = Double.POSITIVE_INFINITY;
		}
	}
	
	
	/*
	 * PUBLIC METHOD
	 */
	
	public Sampling2DShape optimize() {
		MultivariateMinimum optimizer = method.instantiate(shape);
		double[] xvec = shape.getParameters().clone();
		optimizer.optimize(this, shape.getParameters(), 0.0	, 0.0);
		Sampling2DShape minimum = shape.clone();
		minimum.setParameters(xvec);
		return minimum;
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
	
	public double evaluate(double[] argument) {
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

	
	/*
	 * GETTERS AND SETTERS
	 */

	public Sampling2DShape getShape() {	return shape;	}
	public void setShape(Sampling2DShape shape) {	this.shape = shape; }
	public Sampling2DShape.EvalFunction getFunction() {		return function;	}
	public void setFunction(Sampling2DShape.EvalFunction function) {		this.function = function;	}
	public int getNPoints() {		return n_points; 	}
	public void setNPoints(int nPoints) {		n_points = nPoints; 	}
	public ImageProcessor getImageProcessor() { 		return ip; 	}
	public void setImageProcessor(ImageProcessor ip) { 		this.ip = ip; 	}
	public void setMethod(Method method) {		this.method = method;	}
	public Method getMethod() {		return method;	}

}
