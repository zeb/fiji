package core.image;

import core.image.iterator.Iterator;

public class ArrayImage extends Image {

	private int[] array;
	
	@Override
	public int get(int x, int y, int z) {
		return get(mapper.get(x, y, z));
	}

	@Override
	public int get(int position) {
		return array[position];
	}

	@Override
	public ImageType getImageType() {
		return ImageType.ARRAY;
	}

	@Override
	public void loadImage(String filename) {
		Image image = new IJImage();
		image.loadImage(filename);
		create(image);
		Iterator itr = image.getIterator();
		while (itr.hasNext()) {
			set(itr.getPosition(), itr.get());
			itr.next();
		}		
	}

	@Override
	public void newImage() {
		array = new int[width * height * depth];
	}

	@Override
	public void set(int x, int y, int z, int value) {
		set(mapper.get(x, y, z), value);
	}

	@Override
	public void set(int position, int value) {
		array[position] = value;
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
		ArrayImage source = (ArrayImage)image;
		System.arraycopy(source.array, 0, array, 0, source.array.length);
	}

	@Override
	public int getMaxPossibleValue() {
		return Integer.MAX_VALUE;
	}

}
