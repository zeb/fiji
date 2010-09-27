package core.util;

import core.image.Image;
import core.image.Pixel;

public class LinearMapper {

	private int width;
	private int area;
	private Pixel point;

	public LinearMapper(int width, int area) {
		this.width = width;
		this.area = area;
		point = new Pixel();
	}

	public LinearMapper(Image image) {
		this(image.getWidth(), image.getArea());
	}

	public int get(int x, int y, int z) {
		return x + (y * width) + (z * area);
	}

	public int get(int x, int y) {
		return get(x, y, 0);
	}

	public Pixel get(int position) {
		point.z = position / area;
		position -= point.z * area;
		point.y = position / width;
		point.x = position - (point.y * width);
		return point;
	}
}
