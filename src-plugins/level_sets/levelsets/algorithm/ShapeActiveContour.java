package levelsets.algorithm;

import java.util.Iterator;

import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;

import net.sourceforge.jswarm_pso.*;


public class ShapeActiveContour extends GeodesicActiveContour {

	protected Swarm optimizer_swarm;
	protected PCAShapeModel shape;
	double [] params, params_mean, params_sigma;
	final double gauss_factor;
	
	public ShapeActiveContour(ImageContainer image, ImageProgressContainer img_progress, StateContainer init_state,
			double convergence, double advection, double prop, double curve, double grey, double shape ) {
		super(image, img_progress, init_state, convergence, advection, prop, curve, grey);
	
		// constant for gauss estimate, not worth calculating each time
		gauss_factor = 1 / Math.sqrt(2 * Math.PI);
	}

	protected void init()
	{
		super.init();
		
		if (shape == null) {
			throw new ArithmeticException("No Shape set");
		}
		
	}
	
	protected void loadShape(String fn) {
		// load the current shape model and init parameters that need to be optimized
		shape = new PCAShapeModel(fn);
		params = new double[shape.getNoCurveParams() + shape.getNoPoseParams()];
		params_mean = null;
		params_sigma = null;
	}
	
	
	protected void initOptimizer(int no_vars) 
	{
		FitnessFunction optimizer_fit = new FitnessMAPsurface(this);
		
		// Create a swarm (using 'MyParticle' as sample particle 
		// and 'MyFitnessFunction' as finess function)
		optimizer_swarm = new Swarm(Swarm.DEFAULT_NUMBER_OF_PARTICLES
				, new SurfaceParticle(no_vars)
				, optimizer_fit);
		// Set position (and velocity) constraints. 
		// i.e.: where to look for solutions
		optimizer_swarm.setMaxPosition(1);
		optimizer_swarm.setMinPosition(0);
		optimizer_fit.setMaximize(false); // need minimum of MAP cost function
	}
	
	
	protected final double getModifierTerm(int x, int y, int z) {
		
		return shape.getCurveValue(x, y, z, params) - phi.get(x, y, z);
	}
	
	
	
	
	protected void ImagePCA() {
		
	}
	
	
	protected double shapePriorMAP() {
		// run only once per updateActiveLayer()
		
		// Optimize a few times
		for( int i = 0; i < 20; i++ ) optimizer_swarm.evolve();

		Particle [] opt_part = optimizer_swarm.getParticles();
		for ( int i= 0; i<opt_part.length; i++ ) {
			params[i] = opt_part[i].getBestFitness();
		}
		
		return 0;
	}
	
	
	protected double getShapeCost(double []vars)
	{
		double cost = 0;
		
		cost += getLogInsideTerm(vars);
		cost += getLogGradientTerm(vars);
		cost += getLogShapePrior(vars);
		
		return cost;
	}
	
	protected double getLogInsideTerm(double [] vars)
	{
		
		double counter = 0;
		
		for (int i = 0; i <= (NUM_LAYERS - 1); i++)
		{
			final Iterator<BandElement> it = layers[ZERO_LAYER + i * INSIDE].iterator();
			while (it.hasNext())
			{
				final BandElement elem = it.next();
				final int x = elem.getX();
				final int y = elem.getY();
				final int z = elem.getZ();

				double value = shape.getCurveValue(x, y, z, vars);
				if ( value > 0.0 ) { 
					counter += 1.0;
				}
				else if ( value > -1.0 ) {
					counter += ( 1.0 + value );
				}
			}

		}

		// TODO log of (counter * weight)
		return counter;
	}
	
	
	protected double getLogGradientTerm(double [] vars)
	{
		double sum = 0;
		
		for (int i = 0; i <= (NUM_LAYERS - 1); i++)
		{
			final Iterator<BandElement> it = layers[ZERO_LAYER + i * INSIDE].iterator();
			while (it.hasNext())
			{
				final BandElement elem = it.next();
				final int x = elem.getX();
				final int y = elem.getY();
				final int z = elem.getZ();
				
				sum += Math.sqrt(getGaussianFx(shape.getCurveValue(x, y, z, vars)) - 1.0  + gradients[x][y][z]);
			}
		}
		
		return sum;
	}
	
	protected double getGaussianFx(double x) {
		return gauss_factor * Math.exp(-0.5 * Math.sqrt(x));
	}
	
	
	protected double getLogShapePrior(double [] vars)
	{
		double value = 0;
		
		if ( params_mean == null) {
			return 0; // just the sum of all p's which is const?
		}
		for ( int p_no=0; p_no<vars.length; p_no++) {
			value += Math.sqrt(vars[p_no] - params_mean[p_no] / params_sigma[p_no]);
		}
		
		return value;
	}
	
	protected double getPCASignedDistance() {
		return 0;
	}
	
	
	public class FitnessMAPsurface extends FitnessFunction {
		ShapeActiveContour shape;
		
		public FitnessMAPsurface(ShapeActiveContour shape) {
			this.shape = shape;
		}
		
		public double evaluate(double vars[]) { 
			return shape.getShapeCost(vars);
			//return position[0] + position[1]; 
		}
	} 
	
	public class SurfaceParticle extends Particle {
		// Create a 2-dimentional particle
		public SurfaceParticle(int no_vars) { super(no_vars); } 
	}
}
