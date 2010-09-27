package core.function.connectedcomponent;

import core.function.BinaryWatershed;
import core.image.Image;
import core.image.datastructure.FifoQueue;

public class WatershedRegionLabeller extends ConnectedComponent {

	private int[] labels;
	private FifoQueue floodQueue;
	private int currentLabel;

	public WatershedRegionLabeller(Image sourceImage, int[] labels,
			FifoQueue floodQueue) {
		super(sourceImage);
		this.labels = labels;
		this.floodQueue = floodQueue;
		currentLabel = 0;
	}

	@Override
	protected void newRegionDetected(int position) {
		currentLabel++;
		labels[position] = currentLabel;
	}

	@Override
	protected boolean isForeground(int position) {
		return (sourceImage.get(position) != sourceImage.getMinPossibleValue() && BinaryWatershed
				.isInit(labels[position]));
	}

	@Override
	protected void processNeighbour(int neighbour, FifoQueue queue) {
		if (isBackground(neighbour)) {
			labels[neighbour] = BinaryWatershed.INQUEUE_LABEL;
			floodQueue.add(neighbour);
		}
		if (isForeground(neighbour)) {
			labels[neighbour] = currentLabel;
			queue.add(neighbour);
		}
	}

	private boolean isBackground(int position) {
		return (sourceImage.get(position) == sourceImage.getMinPossibleValue() && BinaryWatershed
				.isInit(labels[position]));
	}

	public void doLabel() {
		doRegionDetection();
	}
}
