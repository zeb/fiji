package core.function.connectedcomponent;


import java.util.ArrayList;

import core.function.param.RegionMaskerParams;
import core.image.Image;
import core.image.ImageFactory;
import core.image.Region;
import core.image.datastructure.FifoQueue;

public class RegionMasker extends ConnectedComponent {

	private Image image;
	private RegionMaskerParams params;

	public RegionMasker(RegionMaskerParams params) {
		super(params.image);
		this.params = params;
		this.image = params.image;
		this.sourceImage = createSparseImage(params.regionsToMask);
	}

	private Image createSparseImage(ArrayList<Region> regionsToMask) {
		Image sparseImage = ImageFactory.createSparseBinaryImage();
		sparseImage.create(image.getWidth(), image.getHeight(),
				image.getDepth());
		for (Region region : regionsToMask) {
			sparseImage.set(region.topMostLeftPosition, sparseImage.getMaxPossibleValue());
		}
		return sparseImage;
	}

	@Override
	protected boolean isForeground(int position) {
		return (image.get(position) != image.getMinPossibleValue());
	}

	@Override
	protected void newRegionDetected(int position) {
		mask(position);
	}

	@Override
	protected void processNeighbour(int neighbour, FifoQueue queue) {
		if (isForeground(neighbour)) {
			mask(neighbour);
			queue.add(neighbour);
		}
	}

	private void mask(int position) {
		image.set(position, image.getMinPossibleValue());
	}

	public Image doMask() {
		if (params.isEnabled) {
			doRegionDetection();
		}
		return image;
	}

}
