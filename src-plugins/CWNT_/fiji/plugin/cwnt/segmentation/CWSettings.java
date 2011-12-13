package fiji.plugin.cwnt.segmentation;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;
import fiji.plugin.trackmate.segmentation.BasicSegmenterSettings;
import fiji.plugin.trackmate.util.TMUtils;

/** 
 * A set of default parameters suitable for masking, as determined
 * by Bhavna Rajaseka.
 */
public class CWSettings extends BasicSegmenterSettings {

	
	/** If true, do 2x2 median-filtering before anything. */
	public boolean 	doMedianFiltering = false;
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
	public double 	alpha 	= 1; 		// 5. α
	/** β, the laplacian positive magnitude prefactor in step 4 */
	public double 	beta 	= 1;		// 6. β
	/** ε, the hessian negative magnitude prefactor in step 4 */
	public double 	epsilon = 1;		// 7. ε
	/** δ, the derivative sum scale in step 4 */
	public double 	delta 	= 1;		// 8. δ
	/** A pre-factor, introduced by Bhavna, to increase the threshold in each slice,
	 * and have a more stringent thresholding.  */
	public double  thresholdFactor = 1;
	
	@Override
	public SegmenterConfigurationPanel createConfigurationPanel() {
		return new CWNTPanel();
	}

	@Override
	public String toString() {
		String str = "";
		str += "  1. Pre-Filtering:\n";
		str += String.format("    - do median filtering: " + doMedianFiltering+"\n");
		str += String.format("    - gaussian filter sigma: %.1f\n", sigmaf);
		str += "  2. Anisotropic diffusion:\n";
		str += String.format("    - number of iterations: %d\n", nAD);
		str += String.format("    - gradient threshold kappa: %.1f\n", kappa);
		str += "  3. Derivatives calculation:\n";
		str += String.format("    - gaussian gradient sigma: %.1f\n", sigmag);
		str += "  4. Mask parameters:\n";
		str += String.format("    - ϒ tanh shift: %.1f\n", gamma);
		str += String.format("    - α gradient contribution: %.1f\n", alpha);
		str += String.format("    - β positive laplacian contribution: %.1f\n", beta);
		str += String.format("    - ε negative hessian contribution: %.1f\n", epsilon);
		str += String.format("    - δ derivatives sum scale: %.1f\n", delta);
		str += "  5. Thresholding:\n";
		str += String.format("    - threshold pre-factor: %.1f\n", thresholdFactor);
		return str;
	}
	
	@Override
	public void marshall(Element element) {
		element.setAttributes(getAttributes());
	}
	
	@Override
	public void unmarshall(Element element) {
		super.unmarshall(element); // Deal with expected radius
		doMedianFiltering = TMUtils.readBooleanAttribute(element, MEDIAN_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		sigmaf 	= TMUtils.readDoubleAttribute(element, SIGMA_F_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		nAD 	= TMUtils.readIntAttribute(element, N_AD_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		kappa 	= TMUtils.readDoubleAttribute(element, KAPPA_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		sigmag 	= TMUtils.readDoubleAttribute(element, SIGMA_G_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		gamma 	= TMUtils.readDoubleAttribute(element, GAMMA_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		alpha 	= TMUtils.readDoubleAttribute(element, ALPHA_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		beta 	= TMUtils.readDoubleAttribute(element, BETA_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		epsilon = TMUtils.readDoubleAttribute(element, EPSILON_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		delta 	= TMUtils.readDoubleAttribute(element, DELTA_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		thresholdFactor = TMUtils.readDoubleAttribute(element, THRESHOLD_FACTOR_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
	}
	
	protected List<Attribute> getAttributes() {
		Attribute attMedian = new Attribute(MEDIAN_ATTRIBUTE_NAME, 	"" + doMedianFiltering);
		Attribute attSigmaF = new Attribute(SIGMA_F_ATTRIBUTE_NAME, "" + sigmaf);
		Attribute attNAD 	= new Attribute(N_AD_ATTRIBUTE_NAME, 	"" + nAD);
		Attribute attKappa 	= new Attribute(KAPPA_ATTRIBUTE_NAME, 	"" + kappa);
		Attribute attSigmaG	= new Attribute(SIGMA_G_ATTRIBUTE_NAME, "" + sigmag);
		Attribute attGamma	= new Attribute(GAMMA_ATTRIBUTE_NAME, 	"" + gamma);
		Attribute attAlpha	= new Attribute(ALPHA_ATTRIBUTE_NAME, 	"" + alpha);
		Attribute attBeta	= new Attribute(BETA_ATTRIBUTE_NAME, 	"" + beta);
		Attribute attEpsilon= new Attribute(EPSILON_ATTRIBUTE_NAME, "" + epsilon);
		Attribute attDelta	= new Attribute(DELTA_ATTRIBUTE_NAME, 	"" + delta);
		Attribute attThreshFact	= new Attribute(THRESHOLD_FACTOR_ATTRIBUTE_NAME, "" + thresholdFactor);
	
		List<Attribute> atts = new ArrayList<Attribute>(11);
		atts.add(super.getAttribute());
		atts.add(attMedian);
		atts.add(attSigmaF);
		atts.add(attNAD);
		atts.add(attKappa);
		atts.add(attSigmaG);
		atts.add(attGamma);
		atts.add(attAlpha);
		atts.add(attBeta);
		atts.add(attEpsilon);
		atts.add(attDelta);
		atts.add(attThreshFact);
		return atts;
	}

	private static final String MEDIAN_ATTRIBUTE_NAME 	= "medianFiltering";
	private static final String SIGMA_F_ATTRIBUTE_NAME 	= "sigmaf";
	private static final String N_AD_ATTRIBUTE_NAME 	= "nAD";
	private static final String KAPPA_ATTRIBUTE_NAME 	= "kapa";
	private static final String SIGMA_G_ATTRIBUTE_NAME 	= "sigmag";
	private static final String ALPHA_ATTRIBUTE_NAME 	= "alpha";
	private static final String GAMMA_ATTRIBUTE_NAME 	= "gamma";
	private static final String BETA_ATTRIBUTE_NAME 	= "beta";
	private static final String EPSILON_ATTRIBUTE_NAME 	= "epsilon";
	private static final String DELTA_ATTRIBUTE_NAME 	= "delta";
	private static final String THRESHOLD_FACTOR_ATTRIBUTE_NAME = "thresholdFactor";	
}
