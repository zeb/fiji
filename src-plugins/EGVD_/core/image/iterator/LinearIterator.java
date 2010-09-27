package core.image.iterator;

import core.image.Iterable;

public class LinearIterator implements Iterator {

	private Iterable iterable;
	private int x;
	private int y;
	private int z;
	private int position;
	private int size;
	private int width;
	private int height;

	public LinearIterator(Iterable iterable) {
		this.iterable = iterable;
		storeDimensions();
		initialise();
	}

	private void storeDimensions() {
		width = iterable.getWidth();
		height = iterable.getHeight();
		size = iterable.getIterationSize();
	}

	private void initialise() {
		x = 0;
		y = 0;
		z = 0;
		position = 0;
	}

	public boolean hasNext() {
		return (position < size);
	}

	public void next() {
		position++;
		moveCoordindates();
	}

	private void moveCoordindates() {
		x++;
		if (x == width) {
			y++;
			if (y == height) {
				y = 0;
				z++;
			}
			x = 0;
		}
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public int get() {
		return iterable.get(x, y, z, position);
	}

	@Override
	public int getPosition() {
		return position;
	}

	@Override
	public void set(int value) {
		iterable.set(x, y, z, position, value);
	}

}
