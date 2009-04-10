package levelsets.algorithm;

import ij.IJ;
import levelsets.algorithm.LevelSetImplementation.Parameter;
import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;

public class GeodesicActiveContour extends LevelSetImplementation {

	// Image gradients
	protected double [][][] gradients = null;
	// Image gradients 2nd differentation
	protected double [][][] grad_gradients = null;
	
	// Power of the advection force - expands contour along surface normals
	protected static double ALPHA_ADVECTION = 2.2;
	// Power of the advection force - expands contour along surface normals
	protected static double BETA_PROPAGATION = 1;
	// Power of regulatory curvature term
	protected static double GAMMA_CURVATURE = 1;
	// Greyscale values
	protected static double GAMMA_GREYSCALE = 0;

	// Greyscale intensity range (from GREY_T - GREY_EPSILON to GREY_T + GREY_EPSILON)
	protected static double GREY_EPSILON = 1;
	protected static double GREY_T = 1;
	
	public static Parameter [] known_params = { Parameter.W_ADVECTION, Parameter.W_CURVATURE, Parameter.W_PROPAGATION };

	
	public GeodesicActiveContour(ImageContainer image,
			ImageProgressContainer img_progress, StateContainer init_state) {
		super(image, img_progress, init_state);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setParameter(Parameter param, double value) {
		if ( ! needInit ) {
			return;
		}
		
		switch(param) {
		case W_ADVECTION:
			ALPHA_ADVECTION = value;
			break;
		case W_PROPAGATION:
			BETA_PROPAGATION = value;
			break;
		case W_CURVATURE:
			GAMMA_CURVATURE = value;
			break;
		default:
			break;
		}
	}
	
	public static Object getParameter(Parameter param) {
		switch(param) {
		case W_ADVECTION:
			return new Double(ALPHA_ADVECTION);
		case W_PROPAGATION:
			return new Double(BETA_PROPAGATION);
		case W_CURVATURE:
			return new Double(GAMMA_CURVATURE);
		}
		return null;
	}

	
	@Override
	protected void init() {
	    super.init();

	    gradients = this.img.calculateGradients();
		for (int z = 0; z < gradients[0][0].length; z++) {
			for (int x = 1; x < gradients.length - 1; x++) {
				for (int y = 1; y < gradients[0].length - 1; y++) {
					gradients[x][y][z] = (1 / (1 + ((gradients[x][y][z]) * 1)));
				}
			}
		}
		grad_gradients = this.calculateGradients(gradients);
	}

	
	@Override
	protected double getDeltaPhi(int x, int y, int z) {
        
        double curvature = getCurvatureTerm(x, y, z);
        double advection = getAdvectionTerm(x, y, z);
        double propagation = getPropagationTerm(x, y, z);
        
        // calculate net change
        double delta_phi = - DELTA_T *
                		(
                		advection * ALPHA_ADVECTION
                		+ propagation * BETA_PROPAGATION
                   		+ curvature * GAMMA_CURVATURE 
//                  	+ advection * GAMMA_GREYSCALE * (GREY_EPSILON - Math.abs(img.getPixel(x, y, z) - GREY_T))
                		);
        
        return delta_phi;
	}

	@Override
	protected void updateDeltaT() {
		DELTA_T = 1d/6d * 1d/(GAMMA_CURVATURE * BETA_PROPAGATION * ALPHA_ADVECTION);
	}

	// upwind scheme
	protected double getAdvectionTerm(int x, int y, int z) {
		double result = 0d;
		
		
		double xB = (x > 0) ?
				phi.get(x - 1, y, z) : Double.MAX_VALUE;
		double xF = (x + 1 < phi.getXLength()) ?
				phi.get(x + 1, y, z) : Double.MAX_VALUE;
		double yB = (y > 0) ?
				phi.get(x, y - 1, z) : Double.MAX_VALUE;
		double yF = (y + 1 < phi.getYLength()) ?
				phi.get(x, y + 1, z) : Double.MAX_VALUE;
		double zB = (z > 0) ?
				phi.get(x, y, z - 1) : Double.MAX_VALUE;
		double zF = (z + 1 < phi.getZLength()) ?
				phi.get(x, y, z + 1) : Double.MAX_VALUE;

		double cell_phi = phi.get(x, y, z);

		double xBdiff = cell_phi - xB;
		double xFdiff = xF - cell_phi;
		double yBdiff = cell_phi - yB;
		double yFdiff = yF - cell_phi;
//		double zBdiff = (cell_phi - zB) / zScale;
//		double zFdiff = (zF - cell_phi) / zScale;
		double zBdiff = 0;
		double zFdiff = 0;
		
		if ( grad_gradients[x][y][z] > 0 ) {
			result = xBdiff * grad_gradients[x][y][z] + yBdiff * grad_gradients[x][y][z] + zBdiff * grad_gradients[x][y][z];
		} else {
			result = xFdiff * grad_gradients[x][y][z] + yFdiff * grad_gradients[x][y][z] + zFdiff * grad_gradients[x][y][z];			
		}	
		
		return result;
	}
	
	// Identical to GeometricCurveEvolution
	protected double getPropagationTerm(int x, int y, int z) {
		double xB = (x > 0) ?
				phi.get(x - 1, y, z) : Double.MAX_VALUE;
		double xF = (x + 1 < phi.getXLength()) ?
				phi.get(x + 1, y, z) : Double.MAX_VALUE;
		double yB = (y > 0) ?
				phi.get(x, y - 1, z) : Double.MAX_VALUE;
		double yF = (y + 1 < phi.getYLength()) ?
				phi.get(x, y + 1, z) : Double.MAX_VALUE;
		double zB = (z > 0) ?
				phi.get(x, y, z - 1) : Double.MAX_VALUE;
		double zF = (z + 1 < phi.getZLength()) ?
				phi.get(x, y, z + 1) : Double.MAX_VALUE;

		double cell_phi = phi.get(x, y, z);

		double xBdiff, xFdiff, yBdiff, yFdiff, zBdiff = 0, zFdiff = 0;
		
		if ( gradients[x][y][z] > 0 ) {
			xBdiff = Math.max(cell_phi - xB, 0);
			xFdiff = Math.min(xF - cell_phi, 0);
			yBdiff = Math.max(cell_phi - yB, 0);
			yFdiff = Math.min(yF - cell_phi, 0);
//			zBdiff = Math.max((cell_phi - zB) / zScale, 0);
//			zFdiff = Math.min((zF - cell_phi) / zScale, 0);
		} else {
			xBdiff = Math.min(cell_phi - xB, 0);
			xFdiff = Math.max(xF - cell_phi, 0);
			yBdiff = Math.min(cell_phi - yB, 0);
			yFdiff = Math.max(yF - cell_phi, 0);
//			zBdiff = Math.min((cell_phi - zB) / zScale, 0);
//			zFdiff = Math.max((zF - cell_phi) / zScale, 0);			
		}

		return Math.sqrt(xBdiff * xBdiff + xFdiff * xFdiff +
				yBdiff * yBdiff + yFdiff * yFdiff +
				zBdiff * zBdiff + zFdiff * zFdiff) * gradients[x][y][z];
	}

	
	// Identical to GeometricCurveEvolution
	protected double getCurvatureTerm(int x, int y, int z) {
		if (x == 0 || x >= (phi.getXLength() - 1)) return 0;
		if (y == 0 || y >= (phi.getYLength() - 1)) return 0;
		boolean curvature_3d = false; //((z > 0) && (z < phi.getZLength() - 1));

		/* access to the deferred array is costly, so avoid multiple queries
	         for the same value and pre assign here
		 */
		double cell_phi = phi.get(x, y, z);
		double phiXB = phi.get(x - 1, y, z);
		double phiXF = phi.get(x + 1, y, z);
		double phiYB = phi.get(x, y - 1, z);
		double phiYF = phi.get(x, y + 1, z);

		double phiX = (phiXF - phiXB) / 2;
		double phiY = (phiYF - phiYB) / 2;
		double phiXX = (phiXF + phiXB - (2 * cell_phi));
		double phiYY = (phiYF + phiYB - (2 * cell_phi));
		double phiXY = (phi.get(x + 1, y + 1, z) - phi.get(x + 1, y - 1, z) -
				phi.get(x - 1, y + 1, z) + phi.get(x - 1, y - 1, z)) / 4;

		double phiZ = 0, phiZZ = 0, phiXZ = 0, phiYZ = 0;
		if (curvature_3d)
		{
			double phiZB = phi.get(x, y, z - 1);
			double phiZF = phi.get(x, y, z + 1);
			phiZ = (phiZF - phiZB) / 2;
			phiZZ = (phiZF + phiZB - (2 * cell_phi));
			phiXZ = (phi.get(x + 1, y, z + 1) - phi.get(x + 1, y, z - 1) - phi.get(x - 1, y, z + 1) + phi.get(x - 1, y, z - 1)) / 4;
			phiYZ = (phi.get(x, y + 1, z + 1) - phi.get(x, y + 1, z - 1) - phi.get(x, y - 1, z + 1) + phi.get(x, y - 1, z - 1)) / 4;
		}

		if (phiX == 0 || phiY == 0) return 0;
		if (curvature_3d && phiZ == 0) return 0;

		double curvature = 0, deltaPhi = 0;
		if (curvature_3d)
		{
			deltaPhi = Math.sqrt(phiX * phiX + phiY * phiY + phiZ * phiZ);
			curvature = -1 * ((phiXX * (phiY * phiY + phiZ * phiZ)) +
					(phiYY * (phiX * phiX + phiZ * phiZ)) +
					(phiZZ * (phiX * phiX + phiY * phiY)) -
					(2 * phiX * phiY * phiXY) -
					(2 * phiX * phiZ * phiXZ) -
					(2 * phiY * phiZ * phiYZ)) /
					Math.pow(phiX * phiX + phiY * phiY + phiZ * phiZ, 3/2);
		}
		else
		{
			deltaPhi = Math.sqrt(phiX * phiX + phiY * phiY);
			curvature = -1 * ((phiXX * phiY * phiY) + (phiYY * phiX * phiX)
					- (2 * phiX * phiY * phiXY)) /
					Math.pow(phiX * phiX + phiY * phiY, 3/2);
		}

		return curvature * deltaPhi * gradients[x][y][z];
	}
	
	private double getGradientImageTerm(int x, int y, int z)
	{
		return gradients[x][y][z];
		// return (1 / (1 + ((gradients[x][y][z]) * 2)));
	}


	
	   /**
	    * Calculates grey value gradients of an input double array
	    * Required for the grad_gradient calculation, taken from ImageContainer
	    * @return The result array
	    */
	protected double[][][] calculateGradients(double [][][]grad_src)
	{
		IJ.log("Calculating gradients");
		double zScale = img.getzScale();
		double[][][] gradients = new double[img.getWidth()][img.getHeight()][img.getImageCount()];

		for (int z = 0; z < gradients[0][0].length; z++)
		{
			for (int x = 1; x < gradients.length - 1; x++)
			{
				for (int y = 1; y < gradients[0].length - 1; y++)
				{

					double xGradient =
						(grad_src[x + 1][y][z] - grad_src[x - 1][y][z]) / 2;
					double yGradient =
						(grad_src[x][y + 1][z] - grad_src[x][y - 1][z]) / 2;
					double zGradient = 0;
					if ((z > 0) && (z < gradients[0][0].length - 1))
					{
						zGradient =
							(grad_src[x][y][z + 1] - grad_src[x][y][z - 1]) / (2 * zScale);
					}
					gradients[x][y][z] = -Math.sqrt(xGradient * xGradient + yGradient * yGradient + zGradient * zGradient);
				}
			}
		}

		return gradients;
	}
	
}
