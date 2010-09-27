package core.function.connectedcomponent;

import core.image.Image;
import core.image.datastructure.FifoQueue;
import core.image.iterator.Iterator;
import core.image.iterator.NeighbourIterator;

public abstract class ConnectedComponent {

	protected Image sourceImage;
	protected int[] neighbourOffsets;

	public ConnectedComponent(Image sourceImage) {
		this.sourceImage = sourceImage;
		neighbourOffsets = NeighbourIterator.getNeighbourOffsets(sourceImage);
	}

	protected void doRegionDetection() {
		int position;
		Iterator itr = sourceImage.getIterator();
		while (itr.hasNext()) {
			position = itr.getPosition();
			if (isForeground(position)) {
				newRegionDetected(position);
				growBlob(position);
			}
			itr.next();
		}
	}

	private void growBlob(int position) {
		int center;
		int neighbour;
		FifoQueue queue = new FifoQueue();
		queue.add(position);
		while (!queue.isEmpty()) {
			center = queue.remove();
			preNeighbourProcessing(center);
			for (int neighbourOffset : neighbourOffsets) {
				neighbour = center + neighbourOffset;
				processNeighbour(neighbour, queue);
			}
			postNeighbourProcessing(center);
		}
	}

	protected void preNeighbourProcessing(int center) {
	}

	protected void postNeighbourProcessing(int center) {
	}

	protected abstract boolean isForeground(int position);

	protected abstract void newRegionDetected(int position);

	protected abstract void processNeighbour(int neighbour, FifoQueue queue);
}
