package core.image.iterator;

import core.image.Image;

public class NeighbourIterator {

	public final static int NEIGHBOUR_SIZE_3D = 26;
	public final static int NEIGHBOUR_SIZE_2D = 8;

	public static int[] getNeighbourOffsets(Image image) {
		return getNeighbourOffsets(image.getWidth(), image.getHeight(), image
				.getDepth());
	}

	public static int[] getNeighbourOffsets(int width, int height, int depth) {
		int[] neighbours;
		int startDepth;
		int endDepth;
		if (depth == 1) {
			neighbours = new int[NEIGHBOUR_SIZE_2D];
			startDepth = endDepth = 0;
		} else {
			neighbours = new int[NEIGHBOUR_SIZE_3D];
			startDepth = (width * height) * -1;
			endDepth = (width * height);
		}

		int count = 0;
		for (int k = startDepth; k <= endDepth; k += (width * height)) {
			for (int j = -width; j <= width; j += width) {
				for (int i = -1; i <= 1; i++) {
					if (i + j + k == 0) {
						continue;
					}
					neighbours[count] = i + j + k;
					count++;
				}
			}
		}
		return neighbours;
	}
}
