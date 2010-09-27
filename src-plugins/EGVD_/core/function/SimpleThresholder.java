package core.function;

import core.image.Image;
import core.image.ImageFactory;
import core.image.iterator.Iterator;

public class SimpleThresholder {

	public SimpleThresholder() {
	}

	public void doThreshold(Image image, int thresholdValue,
			Image thresholdedImage) {
		Iterator imageItr = image.getIterator();
		Iterator thresholdedImageItr = thresholdedImage.getIterator();
		while (imageItr.hasNext()) {
			if (imageItr.get() >= thresholdValue) {
				thresholdedImageItr.set(thresholdedImage.getMaxPossibleValue());
			} else {
				thresholdedImageItr.set(thresholdedImage.getMinPossibleValue());
			}
			imageItr.next();
			thresholdedImageItr.next();
		}
	}

	public Image doThreshold(Image image, int thresholdValue) {
		Image thresholdedImage = ImageFactory.createBinaryImage();
		thresholdedImage.create(image);
		doThreshold(image, thresholdValue, thresholdedImage);
		return thresholdedImage;
	}
}
