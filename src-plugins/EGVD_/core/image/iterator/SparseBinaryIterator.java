package core.image.iterator;

import core.image.Image;
import core.image.Pixel;
import core.image.SparseBinaryImage;
import core.util.LinearMapper;

public class SparseBinaryIterator implements Iterator {

	private Image image;
	private int size;
	private int position;
	private int[] positions;
	private LinearMapper mapper;
	private Pixel point;

	public SparseBinaryIterator(Image image) {
		this.image = image;
		size = image.getIterationSize();
		position = 0;
		positions = ((SparseBinaryImage) image).getPositions();
		mapper = new LinearMapper(image.getWidth(), image.getWidth()
				* image.getHeight());
		point = null;
	}

	@Override
	public int get() {
		return image.get(positions[position]);
	}

	@Override
	public int getPosition() {
		return positions[position];
	}

	@Override
	public int getX() {
		computeXYZ();
		return point.x;
	}

	@Override
	public int getY() {
		computeXYZ();
		return point.y;
	}

	@Override
	public int getZ() {
		computeXYZ();
		return point.z;
	}

	@Override
	public boolean hasNext() {
		return (position != size);
	}

	@Override
	public void next() {
		position++;
		point = null;
	}

	@Override
	public void set(int value) {
		// TODO not implemented, to throw not supported exception
	}

	private void computeXYZ() {
		if (point == null) {
			point = mapper.get(positions[position]);
		}
	}
}
