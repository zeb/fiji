package core.function.param;

import core.image.Image;

public class ThresholderParams {

	public Image image;
	public int min;
	public int max;
	public int thresholdLevels;

	public ThresholderParams() {
		thresholdLevels = 1;		
	}
	
}
