package fiji.plugin.cwnt.test;

import java.util.Iterator;

import fiji.plugin.cwnt.segmentation.CrownWearingSegmenter;

import mpicbg.imglib.algorithm.labeling.AllConnectedComponents;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.labeling.LabelingType;
import mpicbg.imglib.type.logic.BitType;

public class NucleiSplitter_TestDrive {

	public static void main(String[] args) {
		
		// First we create a blanck image
		int[] dim = new int[] { 50, 40, 40 };
		Image<BitType> img = new ImageFactory<BitType>(new BitType(), new ArrayContainerFactory()).createImage(dim);

		// Then we add 2 blobs, touching in the middle to make 8-like shape.
		float radius = 11;
		float[] center = new float[] { 15, 20, 20 };
		SphereCursor<BitType> cursor = new SphereCursor<BitType>(img, center , radius);
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().set(true);
		}
		
		center = new float[] {35, 20, 20 };
		cursor.moveCenterToCoordinates(center);
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.getType().set(true);
		}

		cursor.close();
		
		// Labeling
		Iterator<Integer> labelGenerator = AllConnectedComponents.getIntegerNames(0);

		PlanarContainerFactory containerFactory = new PlanarContainerFactory();
		ImageFactory<LabelingType<Integer>> imageFactory = new ImageFactory<LabelingType<Integer>>(new LabelingType<Integer>(), containerFactory);
		Labeling<Integer> labeling = new Labeling<Integer>(imageFactory, img.getDimensions(), "Labels");
		labeling.setCalibration(img.getCalibration());

		// 6-connected structuring element
		int[][] structuringElement = new int[][] { {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1} };
		CrownWearingSegmenter.labelAllConnectedComponents(labeling , img, labelGenerator, structuringElement);
		
		ImageJFunctions.copyToImagePlus(labeling).show();

		//
		
	}
	
}
