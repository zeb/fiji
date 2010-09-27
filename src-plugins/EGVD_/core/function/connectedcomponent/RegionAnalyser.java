package core.function.connectedcomponent;

import java.util.ArrayList;

import core.image.Image;
import core.image.ImageFactory;
import core.image.Region;
import core.image.datastructure.FifoQueue;

public class RegionAnalyser extends ConnectedComponent {

	protected Image checkImage;
	protected ArrayList<Region> regions;
	protected Region currentRegion;
	private Image currentBoundary;
	private Image currentRegionImage;
	private boolean toGenerateBoundary;
	private boolean toGenerateRegionImage;
	private boolean isBoundary;

	public RegionAnalyser(Image sourceImage) {
		super(sourceImage);
		toGenerateBoundary = false;
		toGenerateRegionImage = false;
		checkImage = ImageFactory.createBinaryImage();
		checkImage.create(sourceImage);
		regions = new ArrayList<Region>();
	}

	@Override
	protected boolean isForeground(int position) {
		return (isForegroundPixel(position) && !isProcessed(position));
	}

	protected boolean isForegroundPixel(int position) {
		return (sourceImage.get(position) != sourceImage.getMinPossibleValue());
	}

	@Override
	protected void newRegionDetected(int position) {	
		addRegion();
		
		if (toGenerateBoundary) {
			currentRegion.boundary = createSparseBinaryImage();
			currentBoundary = currentRegion.boundary;
		}

		if (toGenerateRegionImage) {
			currentRegion.regionImage = createSparseBinaryImage();
			currentRegionImage = currentRegion.regionImage;
		}
		
		currentRegion.topMostLeftPosition = position;
		markAsRegion(position);
	}

	private Image createSparseBinaryImage() {
		Image image = ImageFactory.createSparseBinaryImage();
		image.create(sourceImage);
		return image;
	}

	private void addRegion() {
		currentRegion = new Region();
		regions.add(currentRegion);
	}

	private boolean isProcessed(int position) {
		return (checkImage.get(position) == checkImage.getMaxPossibleValue());
	}

	private void markAsRegion(int position) {
		checkImage.set(position, checkImage.getMaxPossibleValue());
		currentRegion.size++;

		if (toGenerateRegionImage) {
			currentRegionImage.set(position,
					currentRegionImage.getMaxPossibleValue());
		}
	}

	@Override
	protected void processNeighbour(int neighbour, FifoQueue queue) {
		if (isForeground(neighbour)) {
			markAsRegion(neighbour);
			queue.add(neighbour);
		}

		if (toGenerateBoundary && !isForegroundPixel(neighbour)) {
			isBoundary = true;
		}
	}

	@Override
	protected void preNeighbourProcessing(int center) {
		isBoundary = false;
	}

	@Override
	protected void postNeighbourProcessing(int center) {
		if (toGenerateBoundary && isBoundary) {
			currentBoundary.set(center, currentBoundary.getMaxPossibleValue());
		}
	}

	public ArrayList<Region> doAnalysis() {
		doRegionDetection();
		return regions;
	}

	public void setToGenerateBoundary(boolean toGenerateBoundary) {
		this.toGenerateBoundary = toGenerateBoundary;
	}

	public void setToGenerateRegionImage(boolean toGenerateRegionImage) {
		this.toGenerateRegionImage = toGenerateRegionImage;
	}

}
