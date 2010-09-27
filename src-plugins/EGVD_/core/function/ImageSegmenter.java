package core.function;

import core.exception.InvalidImageDimensionException;
import core.image.Image;
import core.image.iterator.Iterator;
import core.util.ImageUtility;

public class ImageSegmenter {

	private Image result;

	public Image segmentImageUsingWatershedLines(Image image,
			Image watershedLines) throws InvalidImageDimensionException {
		validate(image, watershedLines);

		result = image.duplicate();

		Iterator itr = watershedLines.getIterator();
		while (itr.hasNext()) {
			if (itr.get() != watershedLines.getMinPossibleValue()) {
				result.set(itr.getPosition(), result.getMinPossibleValue());
			}
			itr.next();
		}

		return result;
	}

	private void validate(Image image, Image watershedLines)
			throws InvalidImageDimensionException {
		if (!ImageUtility.isSameDimensions(image, watershedLines)) {
			throw new InvalidImageDimensionException();
		}
	}
}
