package core.image;

import core.image.iterator.Iterator;
import core.util.ImageUtility;

public class BinaryImage extends Image {

	private boolean[] binaryData;

	@Override
	public int get(int x, int y, int z) {
		return get(mapper.get(x, y, z));
	}

	@Override
	public int get(int position) {
		return binaryData[position] ? 1 : 0;
	}

	@Override
	public ImageType getImageType() {
		return ImageType.BINARY;
	}

	@Override
	public void loadImage(String filename) {
		Image image = new IJImage();
		image.loadImage(filename);
		create(image);
		Image binaryImage = ImageUtility.convertToBinary(image);
		Iterator itr = binaryImage.getIterator();
		while (itr.hasNext()) {
			set(itr.getPosition(), itr.get());
			itr.next();
		}
	}

	@Override
	public void newImage() {
		binaryData = new boolean[width * height * depth];
	}

	@Override
	public void set(int x, int y, int z, int value) {
		set(mapper.get(x, y, z), value);
	}

	@Override
	public void set(int position, int value) {
		binaryData[position] = (value == 0) ? false : true;
	}

	@Override
	public int get(int x, int y, int z, int position) {
		return get(position);
	}

	@Override
	public int getIterationSize() {
		return size;
	}

	@Override
	public void set(int x, int y, int z, int position, int value) {
		set(position, value);
	}

	@Override
	protected void duplicateData(Image image) {
		BinaryImage sourceImage = (BinaryImage) image;
		System.arraycopy(sourceImage.binaryData, 0, binaryData, 0,
				sourceImage.binaryData.length);
	}

	@Override
	public int getMaxPossibleValue() {
		return 1;
	}

}
