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

	private static final boolean DEBUG = true;
	/** The source image (left unchanged). */
	private Image<T> image;
	/** The target image for the pre-processing steps. */
	private Image<T> target;
	private Image<FloatType> floatTarget;
	/** The image containing the gradient components and norm. */
	private Image<FloatType> Gx;
	private Image<FloatType> Gy;
	private Image<FloatType> Gnorm;
	private Image<T> filtered;
	private Image<FloatType> scaled;

	// Step 1
	private double gaussFilterSigma = 0.5;

	// Step 2
	private double kappa = 50;
	private int nIterAnDiff = 5;

	// Step 3
	private double gaussGradSigma = 1;
	private Image<FloatType> Gxx;
	private Image<FloatType> Gxy;
	private Image<FloatType> Gyx;
	private Image<FloatType> Gyy;
	private Image<FloatType> H;
	private Image<FloatType> L;

	// Step 4
	private Image<FloatType> M;
	private int gamma = 1;
	private float beta = 14.9f;
	private float alpha = 2.7f;
	private float epsilon = 16.9f;
	private float delta = 0.5f;


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

	public boolean process() {
		long top = System.currentTimeMillis();
		long dt = 0;

		boolean check;
		System.out.println();
		/*
		 * Step 1: Low pass filter.
		 * So as to damper the noise. We simply do a gaussian filtering.
		 */
		if (DEBUG) {
			System.out.print("Low pass filter... ");
		}
		check = execGaussianFiltering(image);
		if (!check) {
			return false;
		}
		dt =  (System.currentTimeMillis()-top);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}

		/*
		 * Step 2a: Anisotropic diffusion
		 * To have nuclei of approximative constant intensity.
		 */
		if (DEBUG) {
			System.out.print("Anisotropic diffusion... ");
		}
		check = execAnisotropicDiffusion(target);
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
		check = execIntensityScaling(target);
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}

		/*
		 * Step 3a: Gaussian gradient
		 */
		if (DEBUG) {
			System.out.print("Gaussian gradient... ");
		}
		check = execComputeGradient(floatTarget);
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
		check = execComputeLaplacian(Gx, Gy);
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
		check = execComputeHessian(Gxx, Gxy, Gyx, Gyy);
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}

		/*
		 * Step 4a: Create masking function
		 */
		if (DEBUG) {
			System.out.print("Creating mask function... ");
		}
		check = execCreateMask(Gnorm, L, H);
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
		check = execMasking(filtered, M);
		if (!check) {
			return false;
		}
		dt = (System.currentTimeMillis()-top-processingTime);
		processingTime += dt;
		if (DEBUG) {
			System.out.println(dt/1e3+" s.");
		}


		return true;

	}


	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public Image<T> getResult() {
		return target;
	}

	public Image<FloatType> getFloatResult() {
		return floatTarget;
	}

	/*
	 * PRIVATE METHODS
	 */
	
	private boolean execMasking(final Image<T> source, final Image<FloatType> mask) {
		target = source.createNewImage("Masked "+source.getName());
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(target.getNumPixels(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - masking thread "+i) {
				public void run() {

					Chunk chunk = chunks.get(ai.getAndIncrement());
					Cursor<T> ct = target.createCursor();
					Cursor<T> cs = source.createCursor();
					Cursor<FloatType> cm = mask.createCursor();
					
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

	private boolean execCreateMask(final Image<FloatType> gradMag, final Image<FloatType> laplacianMag, final Image<FloatType> hessianDet) {

		M = gradMag.createNewImage("Masking function");
		final Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(M.getNumPixels(), numThreads);
		final AtomicInteger ai = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter - create mask thread "+i) {
				public void run() {

					Chunk chunk = chunks.get(ai.getAndIncrement());

					Cursor<FloatType> cm = M.createCursor();
					Cursor<FloatType> cg = gradMag.createCursor();
					Cursor<FloatType> cl = laplacianMag.createCursor();
					Cursor<FloatType> ch = hessianDet.createCursor();

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
								- (
										( 		alpha * cg.getType().get()
												+ beta * cl.getType().get()
												+ epsilon * ch.getType().get() / delta
										) 
								)
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
		floatTarget = M;
		return true;
	}

	private boolean execComputeHessian(final Image<FloatType> Dxx, final Image<FloatType> Dxy, final Image<FloatType> Dyx, final Image<FloatType> Dyy) {

		H = Dxx.createNewImage("Negative part of Hessian");
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
		floatTarget = H;
		return true;
	}

	private boolean execComputeLaplacian(final Image<FloatType> Dx, final Image<FloatType> Dy) {

		GaussianGradient2D<FloatType> gradX = new GaussianGradient2D<FloatType>(Dx, gaussGradSigma);
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

		GaussianGradient2D<FloatType> gradY = new GaussianGradient2D<FloatType>(Dy, gaussGradSigma);
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
		floatTarget = L;
		return true;
	}



	private boolean execComputeGradient(final Image<FloatType> source) {
		GaussianGradient2D<FloatType> grad = new GaussianGradient2D<FloatType>(source, gaussGradSigma);
		grad.setNumThreads(numThreads);
		boolean check = grad.checkInput() && grad.process();
		if (check) {
			List<Image<FloatType>> gc = grad.getGradientComponents();
			Gx = gc.get(0);
			Gy = gc.get(1);
			Gnorm = grad.getResult();
			floatTarget = Gnorm;
			return true;
		} else {
			errorMessage = grad.getErrorMessage();
			return false;
		}

	}

	private boolean execIntensityScaling(final Image<T> source) {

		ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), source.getContainerFactory());
		scaled = factory.createImage(source.getDimensions(), "Scaled");

		final int width = scaled.getDimension(0);
		final int height = scaled.getDimension(1);
		final int nslices = scaled.getDimension(2);

		final AtomicInteger aj = new AtomicInteger();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("NucleiSegmenter intensity scaling thread "+i) {

				public void run() {

					LocalizableByDimCursor<T> cs = source.createLocalizableByDimCursor();
					LocalizableByDimCursor<FloatType> ct = scaled.createLocalizableByDimCursor();

					float val;
					for (int z=aj.getAndIncrement(); z<nslices; z=aj.getAndIncrement()) {

						cs.setPosition(z, 2);
						ct.setPosition(z, 2);

						// Find min & max

						double val_min = source.createType().getMaxValue();
						double val_max = source.createType().getMinValue();
						T min = source.createType();
						T max = source.createType();
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
		floatTarget = scaled;

		return true;
	}


	private boolean execAnisotropicDiffusion(final Image<T> source) {
		AnisotropicDiffusion<T> andiff = new AnisotropicDiffusion<T>(source, 1, kappa);
		andiff.setDimensions(new int[] { 0, 1 }); // We only do it in 2D
		andiff.setNumThreads(numThreads);
		boolean check = andiff.checkInput();
		for (int i = 0; i < nIterAnDiff; i++) {
			check = check && andiff.process();
		}
		return check;
	}


	private boolean execGaussianFiltering(final Image<T> source) {
		double[] sigmas = new double[] { gaussFilterSigma, gaussFilterSigma  };
		GaussianConvolutionReal2D<T> gaussFilter = new GaussianConvolutionReal2D<T>(
				source, 
				new OutOfBoundsStrategyMirrorFactory<T>(), 
				sigmas );

		gaussFilter.setNumThreads(numThreads);
		boolean check = gaussFilter.checkInput() && gaussFilter.process();
		target =  gaussFilter.getResult();
		filtered = target; // Store for last step.
		return check;
	}

	
}
