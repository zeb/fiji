package fiji.plugin.imgflow;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.cursor.special.RegionOfInterestCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

public class LucasKanade extends MultiThreadedBenchmarkAlgorithm {

	private static final String BASE_ERROR_MESSAGE = "LucasKanade: ";
	private static final float EPSILON = 1e-6f; // Let's be conservative...
	private static final double THRESHOLD = 1;
	private List<Image<FloatType>> derivatives;
	private OutOfBoundsStrategyFactory<FloatType> factory= new OutOfBoundsStrategyMirrorFactory<FloatType>();
	private Image<FloatType> ux;
	private Image<FloatType> uy;
	private Image<FloatType> lambda1;
	private Image<FloatType> lambda2;

	/*
	 * CONSTRUCTOR
	 */
	
	public LucasKanade(List<Image<FloatType>> derivatives) {
		this.derivatives = derivatives;
	}	
	
	/*
	 * STATIC METHODS
	 */
	
	public static <T extends RealType<T>> Image<RGBALegacyType> convertToFlowImage(final Image<T> vx, final Image<T> vy) {
		LocalizableByDimCursor<T> cx = vx.createLocalizableByDimCursor();
		LocalizableByDimCursor<T> cy = vy.createLocalizableByDimCursor();
		
		
		ImageFactory<FloatType> floatFactory = new ImageFactory<FloatType>(new FloatType(), vx.getContainerFactory());
		Image<FloatType> norm 	= floatFactory.createImage(vx.getDimensions());
		Image<FloatType> theta 	= floatFactory.createImage(vx.getDimensions());
		LocalizableCursor<FloatType> cnorm = norm.createLocalizableCursor();
		LocalizableByDimCursor<FloatType> ctheta = theta.createLocalizableByDimCursor();
		
		float ux, uy, fnorm, ftheta;
		float maxNorm = Float.NEGATIVE_INFINITY;
		while (cnorm.hasNext()) {
			cnorm.fwd();
			cx.setPosition(cnorm);
			cy.setPosition(cnorm);
			ctheta.setPosition(cnorm);
			
			ux = cx.getType().getRealFloat(); 
			uy = cy.getType().getRealFloat(); 
			
			fnorm = (float) Math.sqrt(ux*ux+uy*uy);
			ftheta = (float) Math.atan2(uy, ux);
			cnorm.getType().set(fnorm);
			ctheta.getType().set(ftheta);
			if (fnorm > maxNorm)
				maxNorm = fnorm;
		}
		cx.close();
		cy.close();
		cnorm.reset();
		ctheta.reset();
		
		ImageFactory<RGBALegacyType> rgbFactory = new ImageFactory<RGBALegacyType>(new RGBALegacyType(), vx.getContainerFactory());
		Image<RGBALegacyType> flowImage = rgbFactory.createImage(vx.getDimensions());
		flowImage.setName("Flow color");
		LocalizableByDimCursor<RGBALegacyType> cf = flowImage.createLocalizableByDimCursor();
		
		float hue, brightness;
		int color;
		while (cnorm.hasNext()) {
			cnorm.fwd();
			ctheta.setPosition(cnorm);
			cf.setPosition(cnorm);
			
			brightness 	= cnorm.getType().get() / maxNorm;
			hue 		= (float) ((ctheta.getType().get() + Math.PI) / (2*Math.PI));
			color = Color.HSBtoRGB(hue, 1.0f, brightness);
			
			cf.getType().set(color);
		}
		cnorm.close();
		ctheta.close();
		cf.close();
		
		return flowImage;		
	}

	
	public static Image<RGBALegacyType> createIndicatorImage(int size) { 
		ImageFactory<RGBALegacyType> rgbFactory = new ImageFactory<RGBALegacyType>(new RGBALegacyType(), new PlanarContainerFactory());
		Image<RGBALegacyType> indicatorImage = rgbFactory.createImage(new int[] { size, size } );
		indicatorImage.setName("Flow indicator");
		LocalizableCursor<RGBALegacyType> cf = indicatorImage.createLocalizableCursor();
		
		int[] position = cf.createPositionArray();
		double norm, theta;
		float hue, brightness;
		int color;
		final float maxNorm = size / 2;
		while (cf.hasNext()) {
			
			cf.fwd();
			cf.getPosition(position);
			
			norm = Math.sqrt( (position[0]-size/2)*(position[0]-size/2) + (position[1]-size/2)*(position[1]-size/2));
			if (norm > maxNorm)
				continue;
			theta = Math.atan2(position[1]-size/2, position[0]-size/2);
			
			brightness 	= (float) (norm / maxNorm);
			hue 		= (float) ((theta + Math.PI) / (2*Math.PI));
			color = Color.HSBtoRGB(hue, 1.0f, brightness);
			
			cf.getType().set(color);
		}
		cf.close();
		return indicatorImage;
	}
	
	
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public boolean checkInput() {
		if (null == derivatives) {
			errorMessage = BASE_ERROR_MESSAGE + "Derivatives are null.";
			return false;
		}
		if (derivatives.size() < 2) {
			errorMessage = BASE_ERROR_MESSAGE + "Need at least 2 derivatives, one over space, one over time, got "+derivatives.size()+".";
			return false;
		}
		return true;
	}

	@Override
	public boolean process() {
		final long startTime = System.currentTimeMillis();

		boolean returnValue = false;
		if (derivatives.size() == 3) 
			returnValue  = process2D();
		
		processingTime = System.currentTimeMillis() - startTime;
		return returnValue;
	}
	
	public List<Image<FloatType>> getResults() {
		List<Image<FloatType>> results = new ArrayList<Image<FloatType>>(derivatives.get(0).getNumDimensions()-1);
		results.add(ux);
		results.add(uy);
		return results;
	}
	
	public List<Image<FloatType>> getEigenvalues() {
		List<Image<FloatType>> eigenValues = new ArrayList<Image<FloatType>>(derivatives.get(0).getNumDimensions()-1);
		eigenValues.add(lambda1);
		eigenValues.add(lambda2);
		return eigenValues;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private boolean process2D() {
		
		final Image<FloatType> Ix = derivatives.get(0);
		final Image<FloatType> Iy = derivatives.get(1);
		final Image<FloatType> It = derivatives.get(2);		
		final LocalizableByDimCursor<FloatType> cx = Ix.createLocalizableByDimCursor(factory);
		final LocalizableByDimCursor<FloatType> cy = Iy.createLocalizableByDimCursor(factory);
		final LocalizableByDimCursor<FloatType> ct = It.createLocalizableByDimCursor(factory);
		
		// Result holder
		ux = Ix.createNewImage();
		uy = Ix.createNewImage();
		lambda1 = Ix.createNewImage();
		lambda2 = Ix.createNewImage();
		ux.setName("Vx");
		uy.setName("Vy");
		lambda1.setName("Lambda1");
		lambda2.setName("Lambda2");
		
		final LocalizableCursor<FloatType> cux = ux.createLocalizableCursor();
		LocalizableByDimCursor<FloatType> cuy = uy.createLocalizableByDimCursor();
		LocalizableByDimCursor<FloatType> cl1 = lambda1.createLocalizableByDimCursor();
		LocalizableByDimCursor<FloatType> cl2 = lambda2.createLocalizableByDimCursor();
		
		// ROI
		int[] offset = new int[] { -1, -1, 0};
		final int[] size   = new int[] { 3, 3 , 1};
		
		RegionOfInterestCursor<FloatType> lct = ct.createRegionOfInterestCursor(offset, size);
		final int[] position = cux.createPositionArray();
		final int[] offsetPos = lct.createPositionArray();
		
		while (cux.hasNext()) {
			
			// Move cursors to next pixel
			cux.fwd();
			cuy.setPosition(cux);
			ct.setPosition(cux);
			cl1.setPosition(cux);
			cl2.setPosition(cux);
			
			// Gather neighborhood data
			float M11 = 0;
			float M12 = 0;
			float M22 = 0;
			float B1 = 0;
			float B2 = 0;
			float det;
			FloatType tx, ty, tt;
			
			cux.getPosition(position);
			lct.reset(positionOffset(position, offsetPos, offset));
			while (lct.hasNext()) {
				lct.fwd();
				cx.setPosition(ct);
				cy.setPosition(ct);
				ct.setPosition(ct);

				tx = cx.getType();
				ty = cy.getType();
				tt = ct.getType();
				
				M11 += tx.get() * tx.get(); 
				M22 += ty.get() * ty.get(); 
				M12 += tx.get() * ty.get();
				B1  += - tx.get() * tt.get();
				B2  += - ty.get() * tt.get();
			}
			lct.close();
			
			// Determinant
			det = M11 * M22 - M12 * M12;
			if (Math.abs(det) < EPSILON) {
				cux.getType().set(Float.NaN);
				cuy.getType().set(Float.NaN);
				continue;
			}
			
			// Inverse matrix
			float Minv11 = M22 / det;
			float Minv12 = - M12 / det; // = Minv21
			float Minv22 = M11 / det;
			
			// Eigenvalues
			double l2 = (M11+M22)/2 + Math.sqrt(((M11+M22)*(M11+M22))/4 - det);
			double l1 = (M11+M22)/2 - Math.sqrt(((M11+M22)*(M11+M22))/4 - det);
			cl1.getType().set((float) l1);
			cl2.getType().set((float) l2);
			
			// Threshold
			float vx, vy;
			if (l1  > THRESHOLD) {
				vx = Minv11 * B1 + Minv12 * B2;
				vy = Minv12 * B1 + Minv22 * B2;
			} else {
				vx = Float.NaN;
				vy = Float.NaN;
			}
			
			cux.getType().set(vx);
			cuy.getType().set(vy);
			
		}
		cux.close();
		cuy.close();
		cx.close();
		cy.close();
		ct.close();
		
		
		return true;
	}

	
	
	/**
	 * Offsets the given position to reflect the origin of the patch being in its center, rather
	 * than at the top-left corner as is usually the case.
	 * @param position the position to be offset
	 * @param offsetPosition an int array to contain the newly offset position coordinates
	 * @return offsetPosition, for convenience.
	 */
	private static final int[] positionOffset(final int[] position, final int[] offsetPosition, final int[] originOffset)	{
			
		for (int i = 0; i < position.length; ++i)
			offsetPosition[i] = position[i] - originOffset[i];
		return offsetPosition;
	}

}
