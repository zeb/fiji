package core.image;

import core.image.iterator.Iterator;
import core.image.iterator.LinearIterator;
import core.util.LinearMapper;

public abstract class Image implements Iterable {

	protected int width;
	protected int height;
	protected int depth;
	protected int area;
	protected int size;
	protected LinearMapper mapper;

	public void create(int width, int height, int depth) {
		storeDimensions(width, height, depth);
		createLinearMapper();
		newImage();
	}

	public void create(Image image) {
		create(image.getWidth(), image.getHeight(), image.getDepth());
	}

	public Image duplicate() {
		Image duplicated = ImageFactory.createImage(getImageType());
		duplicated.create(this);
		duplicated.duplicateData(this);
		return duplicated;
	}

	protected void storeDimensions(int width, int height, int depth) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		area = width * height;
		size = width * height * depth;
	}

	protected void createLinearMapper() {
		mapper = new LinearMapper(width, width * height);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getDepth() {
		return depth;
	}

	public int getArea() {
		return area;
	}

	public int getSize() {
		return size;
	}

	public boolean is2D() {
		return (depth == 1);
	}

	public boolean is3D() {
		return (depth > 1);
	}

	public int get(int x, int y) {
		return get(x, y, 0);
	}

	public void set(int x, int y, int value) {
		set(x, y, 0, value);
	}

	public Iterator getIterator() {
		return new LinearIterator(this);
	}

	public boolean equals(Image image) {
		if (width != image.getWidth() || height != image.getHeight()
				|| depth != image.getDepth()) {
			return false;
		}
		if (getImageType() != image.getImageType()) {
			return false;
		}
		Iterator itr = getIterator();
		while (itr.hasNext()) {
			if (itr.get() != image.get(itr.getX(), itr.getY(), itr.getZ(),
					itr.getPosition())) {
				return false;
			}
			itr.next();
		}

		return true;
	}

	public int getMax() {
		int max = 0;

		Iterator itr = getIterator();
		while (itr.hasNext()) {
			if (itr.get() > max) {
				max = itr.get();
			}
			itr.next();
		}

		return max;
	}

	public int getMin() {
		int min = Integer.MAX_VALUE;

		Iterator itr = getIterator();
		if (itr.hasNext()) {
			while (itr.hasNext()) {
				if (itr.get() < min) {
					min = itr.get();
				}
				itr.next();
			}
		} else {
			min = 0;
		}

		return min;
	}

	public abstract void loadImage(String filename);

	protected abstract void newImage();

	public abstract ImageType getImageType();

	public abstract void set(int x, int y, int z, int value);

	public abstract void set(int position, int value);

	public abstract int get(int x, int y, int z);

	public abstract int get(int position);

	protected abstract void duplicateData(Image image);
	
	public abstract int getMaxPossibleValue();
	
	public int getMinPossibleValue() {
		return 0;
	}
}
