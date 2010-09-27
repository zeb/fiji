package core.function;

import core.function.connectedcomponent.WatershedRegionLabeller;
import core.image.Image;
import core.image.ImageFactory;
import core.image.datastructure.FifoQueue;
import core.image.iterator.NeighbourIterator;
import core.util.ImageUtility;

public class BinaryWatershed {

	public static final int INIT_LABEL = 0;
	public static final int BOUNDARY_LABEL = -1;
	public static final int INQUEUE_LABEL = -2;
	public static final int WATERSHED_LABEL = -3;
	public static final int STARTING_LABEL = 1;

	private Image image;
	private Image result;
	private int[] labels;
	private int[] neighbourOffsets;
	private FifoQueue floodQueue;

	public BinaryWatershed(Image image) {
		this.image = image;
		initialise();
	}

	private void initialise() {
		labels = new int[image.getSize()];
		ImageUtility.setBoundary(labels, image.getWidth(), image.getHeight(),
				image.getDepth(), BOUNDARY_LABEL);
		neighbourOffsets = NeighbourIterator.getNeighbourOffsets(image);
		result = ImageFactory.createSparseBinaryImage();
		result.create(image.getWidth(), image.getHeight(), image
				.getDepth());
		floodQueue = new FifoQueue();
	}

	public int[] getLabels() {
		return labels;
	}

	public void initialiseRegions() {
		WatershedRegionLabeller labeller = new WatershedRegionLabeller(image,
				labels, floodQueue);
		labeller.doLabel();
	}

	private void addToFloodQueue(int position) {
		labels[position] = INQUEUE_LABEL;
		floodQueue.add(position);
	}

	public static boolean isInit(int label) {
		return (label == INIT_LABEL);
	}

	private boolean isInqueue(int label) {
		return (label == INQUEUE_LABEL);
	}

	private boolean isRegion(int label) {
		return (label > INIT_LABEL);
	}

	public void flood() {
		int center;
		int centerLabel;
		int neighbourLabel;

		while (!floodQueue.isEmpty()) {
			center = floodQueue.remove();
			for (int neighbourOffset : neighbourOffsets) {
				neighbourLabel = labels[neighbourOffset + center];
				centerLabel = labels[center];

				if (isInit(neighbourLabel)) {
					addToFloodQueue(neighbourOffset + center);
				}

				if (isRegion(neighbourLabel) && neighbourLabel != centerLabel) {
					if (isRegion(centerLabel)) {
						labelWatershed(center);
					}

					if (isInqueue(centerLabel)) {
						labels[center] = neighbourLabel;
					}
				}
			}
		}
	}

	private void labelWatershed(int position) {
		labels[position] = WATERSHED_LABEL;
		result.set(position, result.getMaxPossibleValue());
	}

	public void doWatershed() {
		initialiseRegions();		
		flood();
	}
	
	public Image getWatershedLines() {
		return result;
	}
}
