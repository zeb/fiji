package fiji.plugin.cwnt.segmentation;

import static fiji.plugin.cwnt.segmentation.LabelToRGB.GLASBEY_LUT;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.LUT;

import java.util.List;
import java.util.Vector;

import mpicbg.imglib.algorithm.MultiThreadedBenchmarkAlgorithm;
import mpicbg.imglib.container.Container;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.Display;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.image.display.imagej.ImageJVirtualStack;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.labeling.LabelingType;
import mpicbg.imglib.multithreading.Chunk;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

public class LabelToGlasbey extends MultiThreadedBenchmarkAlgorithm {

	private final Labeling<Integer> labels;
	private ImagePlus imp;

	public LabelToGlasbey(Labeling<Integer> labels) {
		super();
		this.labels = labels;
	}
	
	
	public ImagePlus getImp() {
		return imp;
	}
	
	
	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();

		Container<UnsignedByteType> container = labels.getContainerFactory().createContainer(labels.getDimensions(), new UnsignedByteType());
		final Image<UnsignedByteType> output = new Image<UnsignedByteType>(container, new UnsignedByteType(), labels.getName()+"_color");
		
		Vector<Chunk> chunks = SimpleMultiThreading.divideIntoChunks(labels.getNumPixels(), numThreads);
		Thread[] threads = new Thread[numThreads];
		
		final int nColors = GLASBEY_LUT.size();
		
		for (int i = 0; i < threads.length; i++) {
			
			final Chunk chunk = chunks.get(i);
			threads[i] = new Thread("LabelToRGB thread "+i) {
				
				@Override
				public void run() {
					
					LocalizableCursor<LabelingType<Integer>> cursor = labels.createLocalizableCursor();
					LocalizableByDimCursor<UnsignedByteType> target = output.createLocalizableByDimCursor();
					cursor.fwd(chunk.getStartPosition());
					
					for (long j = 0; j < chunk.getLoopSize(); j++) {
						cursor.fwd();
						target.setPosition(cursor);
						
						List<Integer> labeling = cursor.getType().getLabeling();
						if (labeling.size() > 0) {
							int label = labeling .get(0);
							int colorIndex = label % nColors;
							target.getType().set(1+colorIndex); // leave label 0 to bg
						}
					}
					cursor.close();
					target.close();
				}
				
			};
			
		}
		
		SimpleMultiThreading.startAndJoin(threads);
		
		final Display<UnsignedByteType> display = output.getDisplay();

		int[] size = new int[ 3 ];
		size[ 0 ] = output.getDimension( 0 );
		size[ 1 ] = output.getDimension( 1 );
		size[ 2 ] = output.getDimension( 2 );

        final ImageStack stack = new ImageStack( size[ 0 ], size[ 1 ] );

        final int dimX = 0;
        final int dimY = 1;
        final int dimZ = 2;
        final int[] dimPos = output.createPositionArray();

        for (int z = 0; z < size[ 2 ]; z++) {
        	if ( dimZ < output.getNumDimensions() ) {
        		dimPos[ dimZ ] = z;
        	}
        	byte[] pixels = ImageJVirtualStack.extractSliceByte( output, display, dimX, dimY, dimPos ) ;
        	ByteProcessor bp = new ByteProcessor( size[ 0 ], size[ 1 ], pixels , getGlasbeyLUT() );

        	stack.addSlice(""+z, bp);
        }
		
        imp =  new ImagePlus( output.getName(), stack );
		Calibration cal = new Calibration();
		cal.pixelWidth = labels.getCalibration(0);
		cal.pixelHeight = labels.getCalibration(1);
		cal.pixelDepth = labels.getCalibration(2);
		imp.setCalibration(cal);
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;	}
	
	

	
	public static final LUT getGlasbeyLUT() {
		
		int nColors = GLASBEY_LUT.size() + 1;
		
		byte[] r = new byte[nColors];
		byte[] g = new byte[nColors];
		byte[] b = new byte[nColors];
		
		r[0] = 0;
		g[0] = 0;
		b[0] = 0; // label = 0 -> black
		int[] col;
		for (int i = 1; i < nColors-1; i++) {
			col = GLASBEY_LUT.get(i);
			r[i] = (byte) col[0];
			g[i] = (byte) col[1];
			b[i] = (byte) col[2];
		}
		LUT lut = new LUT(8, nColors, r, g, b);
		return lut;
		
	}

}

