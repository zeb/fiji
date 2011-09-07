package mpicbg.imglib.algorithm;

import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal2D;
import mpicbg.imglib.algorithm.math.ImageConverter;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.function.RealTypeConverter;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;

public class GaussianGradient2D <T extends RealType<T>> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<FloatType> {

	private Image<T> source;
	private double sigma;
	private Image<FloatType> Dx;
	private Image<FloatType> Dy;
	private List<Image<FloatType>> components = new ArrayList<Image<FloatType>>(2);


	/*
	 * CONSTRUCTOR
	 */


	public GaussianGradient2D(Image<T> source, double sigma) {
		super();
		this.source = source;
		this.sigma = sigma;
	}


	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		// Convert to float; needed to handle negative value properly
        final Image<FloatType> floatImage;
        if (source.createType().getClass().equals(FloatType.class)) {
                floatImage = (Image<FloatType>) source;
        } else {
                ImageConverter<T, FloatType> converter = new ImageConverter<T, FloatType>(
                		source,
                                new ImageFactory<FloatType>(new FloatType(), source.getContainerFactory()),
                                new RealTypeConverter<T, FloatType>());
                converter.setNumThreads();
                converter.checkInput();
                converter.process();
                floatImage = converter.getResult();
        }
		
		// In X
		GaussianConvolutionReal2D<FloatType> gx = new GaussianConvolutionReal2D<FloatType>(
				floatImage, 
				new OutOfBoundsStrategyMirrorFactory<FloatType>(), 
				new double[] { sigma, sigma} ) {
			
			protected void computeKernel() {
			
				double[][] kernel = getKernel();
				kernel[0] = Util.createGaussianKernel1DDouble( sigma, false );		
				kernel[1] = Util.createGaussianKernel1DDouble( sigma, true );
				int kSize = kernel[1].length;
				for (int i = 0; i < kSize; i++) {
					kernel[0][i] = kernel[0][i] * (i - (kSize-1)/2) * 2 / sigma;
				}
				
			};
			
		};
		
		boolean check = gx.checkInput() && gx.process();
		if (check) {
			Dx = gx.getResult();
			Dx.setName("Gx");
		} else {
			errorMessage = gx.getErrorMessage();
			return false;
		}
		
		// In Y
		GaussianConvolutionReal2D<FloatType> gy = new GaussianConvolutionReal2D<FloatType>(
				floatImage, 
				new OutOfBoundsStrategyMirrorFactory<FloatType>(), 
				new double[] { sigma, sigma} ) {
			
			protected void computeKernel() {
			
				double[][] kernel = getKernel();
				kernel[0] = Util.createGaussianKernel1DDouble( sigma, true );		
				kernel[1] = Util.createGaussianKernel1DDouble( sigma, false );
				int kSize = kernel[0].length;
				for (int i = 0; i < kSize; i++) {
					kernel[1][i] = kernel[1][i] * (i - (kSize-1)/2) * 2 / sigma; 
				}
				
			};
			
		};
		
		check = gy.checkInput() && gy.process();
		if (check) {
			Dy = gy.getResult();
			Dy.setName("Gy");
		} else {
			errorMessage = gy.getErrorMessage();
			return false;
		}

		components.clear();
		components.add(Dx);
		components.add(Dy);

		long end = System.currentTimeMillis();
		processingTime = end-start;
		return true;
	}
	
	
	public List<Image<FloatType>> getGradientComponents() {
		return components;
	}


	/**
	 * Return the gradient norm
	 */
	@Override
	public Image<FloatType> getResult() {
		Image<FloatType> norm = Dx.createNewImage("Gradient norm");
		Cursor<FloatType> cx = Dx.createCursor();
		Cursor<FloatType> cy = Dy.createCursor();
		Cursor<FloatType> cn = norm.createCursor();

		double x, y;
		while(cn.hasNext()) {
			cn.fwd();
			cx.fwd();
			cy.fwd(); // Ok because we have identical containers
			x = cx.getType().get();
			y = cy.getType().get();
			cn.getType().setReal(Math.sqrt(x*x+y*y));
		}
		cx.close();
		cy.close();
		cn.close();

		return norm;
	}

}
