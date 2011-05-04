package fiji.plugin.flowmate;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.RGBALegacyType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * A collection of static methods, related to optic flow.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> May 4, 2011
 *
 */
public class OpticFlowUtils {

	
	/**
	 * Create a RGB image representing the flow amplitude and direction encoded by color. 
	 * <p>
	 * The hue of the color is used to represent the flow direction, and its saturation 
	 * is used to represent the flow magnitude.   
	 */
	public static final <T extends RealType<T>> Image<RGBALegacyType> createColorFlowImage(final Image<T> vx, final Image<T> vy) {

		List<Image<FloatType>> polar = convertToPolar(vx, vy);
		final Image<FloatType> norm = polar.get(0);
		final Image<FloatType> theta = polar.get(1);
		
		// Get max amplitude
		ComputeMinMaxWithNan minMax = new ComputeMinMaxWithNan(norm);
		minMax.process();
		final float maxNorm = minMax.getMax().get();
		final float minNorm = minMax.getMin().get();
		
		ImageFactory<RGBALegacyType> rgbFactory = new ImageFactory<RGBALegacyType>(new RGBALegacyType(), vx.getContainerFactory());
		final Image<RGBALegacyType> flowImage = rgbFactory.createImage(vx.getDimensions());
		flowImage.setName("Flow color");
		
		// Prepare for multi-threading
		final int numThreads = Runtime.getRuntime().availableProcessors(); 
		final long imageSize = vx.getNumPixels();
		final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
		final AtomicInteger ai = new AtomicInteger(0);					
		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );

		for (int ithread = 0; ithread < threads.length; ++ithread) {

			// Build Thread array
			threads[ithread] = new Thread(new Runnable() {

				public void run() {

					float hue, brightness, saturation;
					int color;

					// Thread ID
					final int myNumber = ai.getAndIncrement();

					// Get chunk of pixels to process
					final Chunk myChunk = threadChunks.get( myNumber );

					LocalizableByDimCursor<RGBALegacyType> cf = flowImage.createLocalizableByDimCursor();
					LocalizableByDimCursor<FloatType> cnorm = norm.createLocalizableByDimCursor();
					LocalizableByDimCursor<FloatType> ctheta = theta.createLocalizableByDimCursor();

					cnorm.fwd(myChunk.getStartPosition());
					for ( long j = 0; j < myChunk.getLoopSize(); ++j ) {

						cnorm.fwd();
						ctheta.setPosition(cnorm);
						cf.setPosition(cnorm);

						if (Float.isNaN(cnorm.getType().get()) || Float.isNaN(ctheta.getType().get())) {
							color = 0;
						} else {
							brightness 	= 1.0f; //cnorm.getType().get() / maxNorm;
							saturation  = (cnorm.getType().get() - minNorm) / (maxNorm - minNorm);
							hue 		= (float) ((ctheta.getType().get() + Math.PI) / (2*Math.PI));
							color = Color.HSBtoRGB(hue, saturation, brightness); //, brightness);
						}
						cf.getType().set(color);
					}
					cnorm.close();
					ctheta.close();
					cf.close();
				}
			});
		}
		
		SimpleMultiThreading.startAndJoin(threads);

		return flowImage;
	}
	
	
	/**
	 * Convert a pair of images representing respectively the X and Y coordinates of a 2D vector
	 * in Cartesian coordinates to a pair of images representing the same vector in polar coordinates.
	 * <p>
	 * Calculation are made in double precision, but casted to float for storage
	 *   
	 * @param <T>  the type of the source images, {@link FloatType} will be returned 
	 * @param X  the image of the X coordinate
	 * @param Y  the image of the Y coordinate 
	 * @return  a list made of 2 {@link FloatType} {@link Image}s, the first one being the norm of
	 * the 2D vector, the second one being its orientation, in radians, ranging from -π to π.
	 */
	public static final <T extends RealType<T>> List<Image<FloatType>> convertToPolar(final Image<T> X, final Image<T> Y) {
		ImageFactory<FloatType> floatFactory = new ImageFactory<FloatType>(new FloatType(), X.getContainerFactory());
		final Image<FloatType> norm 	= floatFactory.createImage(X.getDimensions());
		final Image<FloatType> theta 	= floatFactory.createImage(X.getDimensions());
		norm.setName("Amplitude");
		theta.setName("Orientation");

		// Prepare for multi-threading
		final int numThreads = Runtime.getRuntime().availableProcessors(); 
		final long imageSize = X.getNumPixels();
		final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
		final AtomicInteger ai = new AtomicInteger(0);					
		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );

		for (int ithread = 0; ithread < threads.length; ++ithread) {

			// Build Thread array
			threads[ithread] = new Thread(new Runnable() {

				public void run() {

					// Thread ID
					final int myNumber = ai.getAndIncrement();

					// Get chunk of pixels to process
					final Chunk myChunk = threadChunks.get( myNumber );

					LocalizableByDimCursor<T> cx = X.createLocalizableByDimCursor();
					LocalizableByDimCursor<T> cy = Y.createLocalizableByDimCursor();
					LocalizableByDimCursor<FloatType> cnorm = norm.createLocalizableByDimCursor();
					LocalizableByDimCursor<FloatType> ctheta = theta.createLocalizableByDimCursor();
					float ux, uy, fnorm, ftheta;
					float maxNorm = Float.NEGATIVE_INFINITY;

					cnorm.fwd(myChunk.getStartPosition());
					for ( long j = 0; j < myChunk.getLoopSize(); ++j ) {

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
					cnorm.close();
					ctheta.close();
				}
			});
		}

		SimpleMultiThreading.startAndJoin(threads);

		List<Image<FloatType>> polar = new ArrayList<Image<FloatType>>(2);
		polar.add(norm);
		polar.add(theta);
		return polar;
	}




	/**
	 * Generate an image that indicates how color encodes velocity. 
	 * Basically, hue encodes speed direction, and hue encodes its norm.
	 * @param size  the width and height of the desired image
	 */
	public static final Image<RGBALegacyType> createIndicatorImage(final int size) { 

		ImageFactory<RGBALegacyType> rgbFactory = new ImageFactory<RGBALegacyType>(new RGBALegacyType(), new PlanarContainerFactory());
		final Image<RGBALegacyType> indicatorImage = rgbFactory.createImage(new int[] { size, size } );
		indicatorImage.setName("Flow indicator");

		// Prepare for multi-threading
		final int numThreads = Runtime.getRuntime().availableProcessors(); 
		final long imageSize = indicatorImage.getNumPixels();
		final Vector<Chunk> threadChunks = SimpleMultiThreading.divideIntoChunks( imageSize, numThreads );
		final AtomicInteger ai = new AtomicInteger(0);					
		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );

		for (int ithread = 0; ithread < threads.length; ++ithread) {

			// Build Thread array
			threads[ithread] = new Thread(new Runnable() {

				public void run() {

					// Thread ID
					final int myNumber = ai.getAndIncrement();

					// Get chunk of pixels to process
					final Chunk myChunk = threadChunks.get( myNumber );

					LocalizableCursor<RGBALegacyType> cf = indicatorImage.createLocalizableCursor();

					int[] position = cf.createPositionArray();
					double norm, theta;
					float hue, brightness, saturation;
					int color;
					final float maxNorm = size / 2;

					cf.fwd(myChunk.getStartPosition()); 
					for ( long j = 0; j < myChunk.getLoopSize(); ++j ) {

						cf.fwd();
						cf.getPosition(position);

						norm = Math.sqrt( (position[0]-size/2)*(position[0]-size/2) + (position[1]-size/2)*(position[1]-size/2));
						if (norm > maxNorm)
							continue;
						theta = Math.atan2(position[1]-size/2, position[0]-size/2);

						brightness 	= 1.0f;
						saturation 	= (float) (norm / maxNorm);
						hue 		= (float) ((theta + Math.PI) / (2*Math.PI));
						color = Color.HSBtoRGB(hue, saturation, brightness);

						cf.getType().set(color);
					}
					cf.close();
				}
			});
		}
		SimpleMultiThreading.startAndJoin(threads);

		return indicatorImage;
	}
	
	

}
