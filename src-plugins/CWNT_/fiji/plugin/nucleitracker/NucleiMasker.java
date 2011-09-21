package fiji.plugin.nucleitracker;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.algorithm.GaussianGradient2D;
import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.algorithm.OutputAlgorithm;
import mpicbg.imglib.algorithm.gauss.GaussianConvolutionReal2D;
import mpicbg.imglib.algorithm.pde.AnisotropicDiffusion;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

public class NucleiMasker <T extends RealType<T>> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<T> {

	private static final boolean DEBUG = false;
	/** The source image (left unchanged). */
	private Image<T> image;
	/** The target image for the pre-processing steps. */
	private Image<T> target;
	
	// Step 1
	private Image<T> filtered;
	private double gaussFilterSigma = 0.5;

	// Step 2
	private Image<T> anDiffImage;
	private Image<FloatType> scaled;
	private double kappa = 50;
	private int nIterAnDiff = 5;

	// Step 3
	private double gaussGradSigma = 1;
	private Image<FloatType> Gx;
	private Image<FloatType> Gy;
	private Image<FloatType> Gnorm;
	private Image<FloatType> Gxx;
	private Image<FloatType> Gxy;
	private Image<FloatType> Gyx;
	private Image<FloatType> Gyy;
	private Image<FloatType> H;
	private Image<FloatType> L;

	// Step 4
	private Image<FloatType> M;
	private double gamma = 1;
	private double beta = 14.9;
	private double alpha = 2.7;
	private double epsilon = 16.9;
	private double delta = 0.5;
	


	/*
	 * CONSTRUCTOR
	 */

	public NucleiMasker(Image<T> image) {
		super();
		this.image = image;
	}

	/*
	 * METHODS
	 */
	
	public Image<T> getGaussianFilteredImage() {
		return filtered;
	}
	
	public Image<T> getAnisotropicDiffusionImage() {
		return anDiffImage;
	}
	
	public Image<FloatType> getGradientNorm() {
		return Gnorm;
	}
	
	public Image<FloatType> getLaplacianMagnitude() {
		return L;
	}
	
	public Image<FloatType> getHessianDeterminant() {
		return H;
	}
	
	public Image<FloatType> getMask() {
		return M;
	}
	
	@Override
	public Image<T> getResult() {
		return target;
	}
	
	public void setParameters(double[] params) {
		gaussFilterSigma 	= params[0];
		nIterAnDiff 		= (int) params[1];
		kappa				= params[2];
		gaussGradSigma		= params[3];
		gamma 				= params[4];
		alpha				= params[5];
		beta				= params[6];
		epsilon				= params[7];
		delta				= params[8];
	}


	public boolean execStep1() {
		long top = System.currentTimeMillis();
		long dt = 0;
		boolean check;
		/*
		 * Step 1: Low pass filter.
		 * So as to damper the noise. We simply do a gaussian filtering.
		 */
		if (DEBUG) {
			System.out.print("Low pass filter... ");
		}
		check = execGaussianFiltering();
		if (!check) {
			return false;
		}
		dt =  (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}

		return check;
	}
	
	public boolean execStep2() {
		long top = System.currentTimeMillis();
		long dt = 0;
		/*
		 * Step 2a: Anisotropic diffusion
		 * To have nuclei of approximative constant intensity.
		 */
		if (DEBUG) {
			System.out.print("Anisotropic diffusion... ");
		}
		boolean check = execAnisotropicDiffusion();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}

		/*
		 * Step 2b: Intensity scaling
		 * Scale intensities in each plane to the range 0 - 1
		 */
		if (DEBUG) {
			System.out.print("Intensity scaling... ");
		}
		check = execIntensityScaling();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}
		
		return check;
	}
	
	public boolean execStep3() {
		long top = System.currentTimeMillis();
		long dt = 0;
		/*
		 * Step 3a: Gaussian gradient
		 */
		if (DEBUG) {
			System.out.print("Gaussian gradient... ");
		}
		boolean check = execComputeGradient();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}

		/*
		 * Step 3b: Laplacian
		 */
		if (DEBUG) {
			System.out.print("Laplacian... ");
		}
		check = execComputeLaplacian();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}

		/*
		 * Step 3c: Hessian
		 */
		if (DEBUG) {
			System.out.print("Hessian... ");
		}
		check = execComputeHessian();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}
		
		return check;
	}
	
	public boolean execStep4() {
		long top = System.currentTimeMillis();
		long dt = 0;
		/*
		 * Step 4a: Create masking function
		 */
		if (DEBUG) {
			System.out.print("Creating mask function... ");
		}
		boolean check = execCreateMask();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}
		
		/*
		 * Step 4b: Do masking, with the gaussian filtered image
		 */
		if (DEBUG) {
			System.out.print("Masking... ");
		}
		check = execMasking();
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}

		return check;
	}
	
	public boolean process() {
		boolean check;
		
		check = execStep1();
		if (!check) {
			return false;
		}
		
		check = execStep2();
		if (!check) {
			return false;
		}
		
		check = execStep3();
		if (!check) {
			return false;
		}
		
		check = execStep4();
		if (!check) {
			return false;
		}

		return true;

	}


	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	

	/*
	 * PRIVATE METHODS
	 */
	
	private boolean execMasking() {
		target = filtered.createNewImage("Masked "+filtered.getName());
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(target.getNumPixels(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - masking thread "+i) {
				public void run() {

					Chunk chunk = chunks.get(ai.getAndIncrement());
					Cursor<T> ct = target.createCursor();
					Cursor<T> cs = filtered.createCursor();
					Cursor<FloatType> cm = M.createCursor();
					
					cm.fwd(chunk.getStartPosition());
					ct.fwd(chunk.getStartPosition());
					cs.fwd(chunk.getStartPosition());

					for (int j = 0; j < chunk.getLoopSize(); j++) {

						cm.fwd();
						ct.fwd();
						cs.fwd();
						
						ct.getType().setReal( cs.getType().getRealDouble() * cm.getType().get());
					}
					
					ct.close();
					cm.close();
					cs.close();
				}
			};
		}
		
		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}

	private boolean execCreateMask() {

		M = Gnorm.createNewImage("Masking function");
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(M.getNumPixels(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - create mask thread "+i) {
				public void run() {

					Chunk chunk = chunks.get(ai.getAndIncrement());

					Cursor<FloatType> cm = M.createCursor();
					Cursor<FloatType> cg = Gnorm.createCursor();
					Cursor<FloatType> cl = L.createCursor();
					Cursor<FloatType> ch = H.createCursor();

					cm.fwd(chunk.getStartPosition());
					cg.fwd(chunk.getStartPosition());
					cl.fwd(chunk.getStartPosition());
					ch.fwd(chunk.getStartPosition());

					double m;
					for (int j = 0; j < chunk.getLoopSize(); j++) {

						cm.fwd();
						cg.fwd();
						cl.fwd();
						ch.fwd();

						m = 0.5 * ( Math.tanh(
								gamma 
								- (		alpha * cg.getType().get()
										+ beta * cl.getType().get()
										+ epsilon * ch.getType().get()
									)  / delta
								
							)	+ 1		);

						cm.getType().setReal(m);
					}

					cm.close();
					cg.close();
					cl.close();
					ch.close();

				};
			};
		}

		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}

	private boolean execComputeHessian() {

		H = Gxx.createNewImage("Negative part of Hessian");
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(H.getNumPixels(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - compute hessian thread "+i) {
				public void run() {

					Cursor<FloatType> cxx = Gxx.createCursor();
					Cursor<FloatType> cxy = Gxy.createCursor();
					Cursor<FloatType> cyx = Gyx.createCursor();
					Cursor<FloatType> cyy = Gyy.createCursor();
					Cursor<FloatType> ch = H.createCursor();

					Chunk chunk = chunks.get(ai.getAndIncrement());
					cxx.fwd(chunk.getStartPosition());
					cxy.fwd(chunk.getStartPosition());
					cyx.fwd(chunk.getStartPosition());
					cyy.fwd(chunk.getStartPosition());
					ch.fwd(chunk.getStartPosition());
					float h;

					for (int j = 0; j < chunk.getLoopSize(); j++) {

						ch.fwd();
						cxx.fwd();
						cxy.fwd();
						cyx.fwd();
						cyy.fwd();

						h = (cxx.getType().get() * cyy.getType().get()) - (cxy.getType().get() * cyx.getType().get());
						if ( h < 0) {
							ch.getType().set(-h);
						}
					}
					cxx.close();
					cxy.close();
					cyx.close();
					cyy.close();
					ch.close();
				}
			};
		}

		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}

	private boolean execComputeLaplacian() {

		GaussianGradient2D<FloatType> gradX = new GaussianGradient2D<FloatType>(Gx, gaussGradSigma);
		gradX.setNumThreads(numThreads);
		boolean check = gradX.checkInput() && gradX.process();
		if (check) {
			List<Image<FloatType>> gcX = gradX.getGradientComponents();
			Gxx = gcX.get(0);
			Gxy = gcX.get(1);
		} else {
			errorMessage = gradX.getErrorMessage();
			return false;
		}

		GaussianGradient2D<FloatType> gradY = new GaussianGradient2D<FloatType>(Gy, gaussGradSigma);
		gradY.setNumThreads(numThreads);
		check = gradY.checkInput() && gradY.process();
		if (check) {
			List<Image<FloatType>> gcY = gradY.getGradientComponents();
			Gyx = gcY.get(0);
			Gyy = gcY.get(1);
		} else {
			errorMessage = gradY.getErrorMessage();
			return false;
		}

		// Enucluated laplacian magnitude
		L = Gxx.createNewImage("Laplacian positive magnitude");
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(L.getNumPixels(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - compute laplacian thread "+i) {

				public void run() {

					Cursor<FloatType> cxx = Gxx.createCursor();
					Cursor<FloatType> cyy = Gyy.createCursor();
					Cursor<FloatType> cl = L.createCursor();

					Chunk chunk = chunks.get(ai.getAndIncrement());
					cxx.fwd(chunk.getStartPosition());
					cyy.fwd(chunk.getStartPosition());
					cl.fwd(chunk.getStartPosition());

					float lap;
					for (int j = 0; j < chunk.getLoopSize(); j++) {
						cl.fwd();
						cxx.fwd();
						cyy.fwd();
						lap = cxx.getType().get() + cyy.getType().get();
						if (lap > 0) {
							cl.getType().set(lap);
						}
					}
					cxx.close();
					cyy.close();
					cl.close();
				}
			};
		}
		
		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}



	private boolean execComputeGradient() {
		GaussianGradient2D<FloatType> grad = new GaussianGradient2D<FloatType>(scaled, gaussGradSigma);
		grad.setNumThreads(numThreads);
		boolean check = grad.checkInput() && grad.process();
		if (check) {
			List<Image<FloatType>> gc = grad.getGradientComponents();
			Gx = gc.get(0);
			Gy = gc.get(1);
			Gnorm = grad.getResult();
			return true;
		} else {
			errorMessage = grad.getErrorMessage();
			return false;
		}

	}

	private boolean execIntensityScaling() {

		ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), anDiffImage.getContainerFactory());
		scaled = factory.createImage(filtered.getDimensions(), "Scaled");

		final int width = scaled.getDimension(0);
		final int height = scaled.getDimension(1);
		final int nslices = scaled.getDimension(2);
		
		final AtomicInteger aj = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter intensity scaling thread "+i) {

				public void run() {

					LocalizableByDimCursor<T> cs = anDiffImage.createLocalizableByDimCursor();
					LocalizableByDimCursor<FloatType> ct = scaled.createLocalizableByDimCursor();

					float val;
					for (int z=aj.getAndIncrement(); z<nslices; z=aj.getAndIncrement()) {

						if (nslices > 1) { // If we get a 2D image
							cs.setPosition(z, 2);
							ct.setPosition(z, 2);
						}

						// Find min & max

						double val_min = anDiffImage.createType().getMaxValue();
						double val_max = anDiffImage.createType().getMinValue();
						T min = anDiffImage.createType();
						T max = anDiffImage.createType();
						min.setReal(val_min);
						max.setReal(val_max);

						for (int y = 0; y < height; y++) {
							cs.setPosition(y, 1);

							for (int x = 0; x < width; x++) {
								cs.setPosition(x, 0);

								if (cs.getType().compareTo(min) < 0) {
									min.set(cs.getType());
								}
								if (cs.getType().compareTo(max) > 0) {
									max.set(cs.getType());
								}

							}
						}

						// Scale
						for (int y = 0; y < height; y++) {
							cs.setPosition(y, 1);
							ct.setPosition(y, 1);

							for (int x = 0; x < width; x++) {
								cs.setPosition(x, 0);
								ct.setPosition(x, 0);

								val = (cs.getType().getRealFloat() - min.getRealFloat()) / (max.getRealFloat() - min.getRealFloat());
								ct.getType().set(val);


							}
						}

					}
					cs.close();
					ct.close();
				}

			};
		}
		
		SimpleMultiThreading.startAndJoin(threads);
		return true;
	}


	private boolean execAnisotropicDiffusion() {
		anDiffImage = filtered.clone();
		AnisotropicDiffusion<T> andiff = new AnisotropicDiffusion<T>(anDiffImage, 1, kappa);
		andiff.setDimensions(new int[] { 0, 1 }); // We only do it in 2D
		andiff.setNumThreads(numThreads);
		boolean check = andiff.checkInput();
		for (int i = 0; i < nIterAnDiff; i++) {
			check = check && andiff.process();
		}
		return check;
	}


	private boolean execGaussianFiltering() {
		double[] sigmas = new double[] { gaussFilterSigma, gaussFilterSigma  };
		GaussianConvolutionReal2D<T> gaussFilter = new GaussianConvolutionReal2D<T>(
				image, 
				new OutOfBoundsStrategyMirrorFactory<T>(), 
				sigmas );

		gaussFilter.setNumThreads(numThreads);
		boolean check = gaussFilter.checkInput() && gaussFilter.process();
		target =  gaussFilter.getResult();
		filtered = target; // Store for last step.
		return check;
	}


	
}
