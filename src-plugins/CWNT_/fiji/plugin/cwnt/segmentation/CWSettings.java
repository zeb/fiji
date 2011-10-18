package fiji.plugin.cwnt.segmentation;

import fiji.plugin.cwnt.gui.CwntGui;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

/** 
 * A set of default parameters suitable for masking, as determined
 * by Bhavna Rajaseka.
 */
public class CWSettings extends SegmenterSettings {

	/** the σ for the gaussian filtering in step 1 */
	public double 	sigmaf 	= 0.5;		// 0. σf
	/** the number of iteration for anisotropic filtering in step 2 */
	public int 		nAD 	= 5;		// 1. nAD
	/** κ, the gradient threshold for anisotropic filtering in step 2*/
	public double 	kappa 	= 50;		// 2. κAD
	/** the σ for the gaussian derivatives in step 3 */
	public double 	sigmag 	= 1;		// 3. σg
	/** γ, the <i>tanh</i> shift in step 4 */
	public double 	gamma 	= 1;		// 4. γ
	/** α, the gradient prefactor in step 4 */
	public double 	alpha 	= 2.7; 		// 5. α
	/** β, the laplacian positive magnitude prefactor in step 4 */
	public double 	beta 	= 14.9;		// 6. β
	/** ε, the hessian negative magnitude prefactor in step 4 */
	public double 	epsilon = 16.9;		// 7. ε
	/** δ, the derivative sum scale in step 4 */
	public double 	delta 	= 0.5;		// 8. δ
	/** A pre-factor, introduced by Bhavna, to increase the threshold in each slice,
	 * and have a more stringent thresholding.  */
	public double  thresholdFactor = 1.6;
	
	/**
	 * Convenience method.
	 * Return the masking parameters (all of them, but the thresholding factor) in
	 * a 9-elements double array.
	 * <ol start="0">
	 * 	<li> the σ for the gaussian filtering in step 1
	 *  <li> the number of iteration for anisotropic filtering in step 2
	 *  <li> κ, the gradient threshold for anisotropic filtering in step 2
	 * 	<li> the σ for the gaussian derivatives in step 3
	 *  <li> γ, the <i>tanh</i> shift in step 4
	 *  <li> α, the gradient prefactor in step 4
	 *  <li> β, the laplacian positive magnitude prefactor in step 4
	 *  <li> ε, the hessian negative magnitude prefactor in step 4
	 *  <li> δ, the derivative sum scale in step 4
	 * </ol>
	 */
	public double[] getMaskingParameters() {
		return new double[] {
				sigmaf,
				nAD,
				kappa,
				sigmag,
				gamma,
				alpha,
				beta,
				epsilon,
				delta
		};
	}
	
	/**
	 * Convenience method.
	 * Set the masking parameters (all of them, but the thresholding factor) 
	 * with the values given in the 9-elements double array.
	 *  <ol start="0">
	 * 	<li> the σ for the gaussian filtering in step 1
	 *  <li> the number of iteration for anisotropic filtering in step 2
	 *  <li> κ, the gradient threshold for anisotropic filtering in step 2
	 * 	<li> the σ for the gaussian derivatives in step 3
	 *  <li> γ, the <i>tanh</i> shift in step 4
	 *  <li> α, the gradient prefactor in step 4
	 *  <li> β, the laplacian positive magnitude prefactor in step 4
	 *  <li> ε, the hessian negative magnitude prefactor in step 4
	 *  <li> δ, the derivative sum scale in step 4
	 * </ol>
	 */
	public void putMaskingParameters(double[] params) {
		sigmaf 	= params[0];
		nAD		= (int) params[1];
		kappa	= params[2];
		sigmag	= params[3];
		gamma	= params[4];
		alpha	= params[5];
		beta	= params[6];
		epsilon	= params[7];
		delta	= params[8];
		
	}

	@Override
	public SegmenterConfigurationPanel createConfigurationPanel() {
		return new CwntGui();
	}

	
	
}
