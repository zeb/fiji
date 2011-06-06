package fiji.plugin.flowmate.util;

public class Windows {

	public static final float[] getFlat3x3Window() {
		return new float[] {
				1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f	
		};
	}

	public static final float[] getFlat5x5Window() {
		return new float[] {
				1/25f, 1/25f, 1/25f, 1/25f, 1/25f,   
				1/25f, 1/25f, 1/25f, 1/25f, 1/25f,   
				1/25f, 1/25f, 1/25f, 1/25f, 1/25f,   
				1/25f, 1/25f, 1/25f, 1/25f, 1/25f,   
				1/25f, 1/25f, 1/25f, 1/25f, 1/25f
		};
	}

	
	public static final float[] getGaussianWindow() {
		return new float[] {
			    0.0000f,
			    0.0002f,
			    0.0011f,
			    0.0018f,
			    0.0011f,
			    0.0002f,
			    0.0000f,
			    0.0002f,
			    0.0029f,
			    0.0131f,
			    0.0216f,
			    0.0131f,
			    0.0029f,
			    0.0002f,
			    0.0011f,
			    0.0131f,
			    0.0586f,
			    0.0966f,
			    0.0586f,
			    0.0131f,
			    0.0011f,
			    0.0018f,
			    0.0216f,
			    0.0966f,
			    0.1592f,
			    0.0966f,
			    0.0216f,
			    0.0018f,
			    0.0011f,
			    0.0131f,
			    0.0586f,
			    0.0966f,
			    0.0586f,
			    0.0131f,
			    0.0011f,
			    0.0002f,
			    0.0029f,
			    0.0131f,
			    0.0216f,
			    0.0131f,
			    0.0029f,
			    0.0002f,
			    0.0000f,
			    0.0002f,
			    0.0011f,
			    0.0018f,
			    0.0011f,
			    0.0002f,
			    0.0000f
		};	
	}
	
	
}
