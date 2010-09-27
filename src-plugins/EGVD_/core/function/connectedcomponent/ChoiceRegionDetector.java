package core.function.connectedcomponent;

import core.exception.InvalidImageDimensionException;
import core.exception.MultipleChoiceOnSameRegionException;
import core.image.Image;
import core.image.ImageFactory;
import core.image.datastructure.FifoQueue;
import core.util.ImageUtility;

public class ChoiceRegionDetector extends ConnectedComponent {

	private Image binaryImage;
	private Image result;
	private boolean isMultipleChoiceOnSameRegion;

	public ChoiceRegionDetector(Image choiceImage, Image binaryImage) {
		super(choiceImage);
		this.binaryImage = binaryImage;
		result = ImageFactory.createBinaryImage();
		result.create(binaryImage);
		isMultipleChoiceOnSameRegion = false;
	}

	@Override
	protected boolean isForeground(int position) {
		if (!isMultipleChoiceOnSameRegion) {
			isMultipleChoiceOnSameRegion = isProcessed(position);
		}
		return true;
	}

	@Override
	protected void newRegionDetected(int position) {
		setResult(position);
	}

	@Override
	protected void processNeighbour(int neighbour, FifoQueue queue) {
		if (isBinaryImageForeground(neighbour) && !isProcessed(neighbour)) {
			queue.add(neighbour);
			setResult(neighbour);
		}
	}

	private void setResult(int position) {
		result.set(position, result.getMaxPossibleValue());
	}

	public Image doChoiceRegionDetection()
			throws MultipleChoiceOnSameRegionException,
			InvalidImageDimensionException {
		validate();
		
		doRegionDetection();

		if (isMultipleChoiceOnSameRegion) {
			throw new MultipleChoiceOnSameRegionException();
		}

		return result;
	}

	private void validate() throws InvalidImageDimensionException {
		if (!ImageUtility.isSameDimensions(binaryImage, sourceImage)) {
			throw new InvalidImageDimensionException();
		}
	}
	
	private boolean isBinaryImageForeground(int position) {
		return (binaryImage.get(position) != binaryImage.getMinPossibleValue());
	}

	private boolean isProcessed(int position) {
		return (result.get(position) == result.getMaxPossibleValue());
	}

}
