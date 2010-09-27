package core.function.param;

import core.image.Image;

public class EGVDParams {

	public Image cellImage;
	public Image seedImage;
	public ThresholderParams cellThresholdParams;
	public int seedThreshold;
	public int particleSize;

	public EGVDParams() {
		cellThresholdParams = new ThresholderParams();
	}

}
