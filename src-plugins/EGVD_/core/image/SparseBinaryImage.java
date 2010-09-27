package core.image;

import java.util.Arrays;

import core.image.iterator.Iterator;
import core.image.iterator.SparseBinaryIterator;

//TODO flawed implementation if setting the same pixel to foreground twice
public class SparseBinaryImage extends Image {

	private int[] positions;
	private boolean isSorted = true;
	public static final int CAPACITY_INCREMENT = 32768;
	private int actualSize;

	public SparseBinaryImage() {
		isSorted = true;
	}

	@Override
	public int get(int x, int y, int z) {
		return get(mapper.get(x, y, z));
	}

	@Override
	public int get(int position) {
		if (!isSorted) {
			sort();
		}
		int foundPosition = Arrays.binarySearch(positions, position);
		return (foundPosition >= 0 ? getMaxPossibleValue()
				: getMinPossibleValue());
	}

	private void sort() {
		Arrays.sort(positions);
		isSorted = true;
	}

	@Override
	public ImageType getImageType() {
		return ImageType.SPARSE_BINARY;
	}

	@Override
	public void loadImage(String filename) {
		// TODO to implement loading of sparse image from file

	}

	@Override
	public void newImage() {
		positions = new int[0];
		incrementArray();
		actualSize = 0;
	}

	private void incrementArray() {
		int[] newPositions = new int[positions.length + CAPACITY_INCREMENT];
		initialiseArray(newPositions, positions.length);
		System.arraycopy(positions, 0, newPositions, 0, positions.length);
		positions = newPositions;
	}

	private void initialiseArray(int[] array, int startPosition) {
		for (int i = startPosition; i < array.length; i++) {
			array[i] = Integer.MAX_VALUE;
		}
	}

	@Override
	public void set(int x, int y, int z, int value) {
		set(mapper.get(x, y, z), value);
	}

	@Override
	public void set(int position, int value) {
		if (value == getMinPossibleValue()) {
			if (!isSorted) {
				sort();
			}
			int foundPosition = Arrays.binarySearch(positions, position);
			if (foundPosition >= 0) {
				removePositionAsBackground(foundPosition);
				sort();
			}
		} else {
			addPositionAsForeground(position);
		}
	}

	private void removePositionAsBackground(int position) {
		positions[position] = Integer.MAX_VALUE;
		actualSize--;
	}

	private void addPositionAsForeground(int position) {
		if (actualSize == positions.length) {
			incrementArray();
		}
		positions[actualSize] = position;
		actualSize++;
		isSorted = false;
	}

	@Override
	public int get(int x, int y, int z, int position) {
		return get(position);
	}

	@Override
	public int getIterationSize() {
		return actualSize;
	}

	@Override
	public void set(int x, int y, int z, int position, int value) {
		set(position, value);
	}

	@Override
	public Iterator getIterator() {
		return new SparseBinaryIterator(this);
	}

	public int[] getPositions() {
		if (!isSorted) {
			sort();
		}
		return positions;
	}

	@Override
	protected void duplicateData(Image image) {
		SparseBinaryImage sourceImage = (SparseBinaryImage) image;
		if (!sourceImage.isSorted) {
			sourceImage.sort();
		}
		System.arraycopy(sourceImage.positions, 0, positions, 0,
				sourceImage.positions.length);
		this.actualSize = sourceImage.actualSize;
	}

	@Override
	public int getMaxPossibleValue() {
		return 1;
	}
}
