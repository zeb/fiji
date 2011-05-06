package fiji.plugin.flowmate;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;

public class NormSquare extends MultiThreadedBenchmarkAlgorithm {
	
	private static final String BASE_ERROR_MESSAGE = "NormSquare: ";
	
	
	private Image<FloatType> X;
	private Image<FloatType> Y;


	private float[] sum;
	private int[] count;
	
	public NormSquare(final Image<FloatType> X, final Image<FloatType> Y) {
		this.X = X;
		this.Y = Y;
	}
	
	
	@Override
	public boolean checkInput() {
		if (X == null) {
			errorMessage = BASE_ERROR_MESSAGE + "X is null.";
			return false;
		}
		if (Y == null) {
			errorMessage = BASE_ERROR_MESSAGE + "Y is null.";
			return false;
		}
		if (X.getNumDimensions() != Y.getNumDimensions()) {
			errorMessage = BASE_ERROR_MESSAGE + "X and Y have different dimensions.";
			return false;
		}
		for(int i = 0; i < X.getNumDimensions(); i++ ) {
			if (X.getDimension(i) != Y.getDimension(i)) {
				errorMessage = BASE_ERROR_MESSAGE + "X and Y have different size.";
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean process() {
		count = new int[X.getDimension(2)];
		sum = new float[X.getDimension(2)];
		
		
		return true;
	}
	
	private void processOneSlice(int slice) {
		
		float tsum = 0;
		int tcount = 0;

		LocalizableByDimCursor<FloatType> cx = X.createLocalizableByDimCursor();
		LocalizableByDimCursor<FloatType> cy = Y.createLocalizableByDimCursor();
		float fx, fy;
		
		
		for (int i = 0; i < X.getDimension(0); i++) {
			cx.setPosition(i, 0);
			cy.setPosition(i, 0);
			
			for (int j = 0; j < X.getDimension(1); j++) {
				cx.setPosition(i, 0);
				cy.setPosition(i, 0);
				
			}
			
			
		}
		
		while(cx.hasNext()) {
			cx.fwd();
			cy.setPosition(cx);
			fx = cx.getType().get();
			fy = cy.getType().get();
			if (Float.isNaN(fx) || Float.isNaN(fy))
				continue;

			tsum += fx*fx + fy*fy;
			tcount++;
		}
		cx.close();
		cy.close();


		this.sum[slice] = tsum;
		this.count[slice] = tcount;
	}

}
