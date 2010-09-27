package core.function.connectedcomponent;

import core.image.Image;
import core.image.ImageFactory;
import core.image.datastructure.FifoQueue;
import core.util.ImageUtility;

public class Choice extends ConnectedComponent {

	private Image result;

	public Choice(Image sourceImage) {
		super(ImageUtility.convertToBinary(sourceImage));
		result = ImageFactory.createSparseBinaryImage();
		result.create(sourceImage.getWidth(), sourceImage.getHeight(),
				sourceImage.getDepth());
	}

	@Override
	protected void newRegionDetected(int position) {
		maskAsBackground(position);
		result.set(position, result.getMaxPossibleValue());
	}

	public Image doChoice() {
		doRegionDetection();
		return result;
	}

	private void maskAsBackground(int position) {
		sourceImage.set(position, sourceImage.getMinPossibleValue());
	}

	@Override
	protected boolean isForeground(int position) {
		return (sourceImage.get(position) != sourceImage.getMinPossibleValue());
	}

	@Override
	protected void processNeighbour(int neighbour, FifoQueue queue) {
		if (isForeground(neighbour)) {
			maskAsBackground(neighbour);
			queue.add(neighbour);
		}
	}
}