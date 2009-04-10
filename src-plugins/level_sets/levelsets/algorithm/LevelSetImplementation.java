package levelsets.algorithm;

import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;

public abstract class LevelSetImplementation extends SparseFieldLevelSet {

	public enum Parameter { W_ADVECTION, W_PROPAGATION, W_CURVATURE, W_GRAYSCALE, TOL_GRAYSCALE, IMG_INPUT };
	
	public static Parameter [] known_params = {};
	
	public LevelSetImplementation(ImageContainer image,
			ImageProgressContainer img_progress, StateContainer init_state) {
		super(image, img_progress, init_state);
		// TODO Auto-generated constructor stub
	}


	public static Parameter [] getImplParams() {
		return known_params;
	}
	
	public static Object getParameter(Parameter param) {
		return null;
	}
		
	public abstract void setParameter(Parameter param, double value);
	
	
}
