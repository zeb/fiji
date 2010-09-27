package core.function;

import java.util.ArrayList;

import core.exception.EmptyImageException;
import core.exception.InvalidImageDimensionException;
import core.exception.InvalidImageTypeException;
import core.exception.MultipleChoiceOnSameRegionException;
import core.function.connectedcomponent.Choice;
import core.function.connectedcomponent.ChoiceRegionDetector;
import core.function.connectedcomponent.RegionAnalyser;
import core.function.connectedcomponent.RegionMasker;
import core.function.param.EGVDParams;
import core.function.param.RegionMaskerParams;
import core.function.param.ThresholderParams;
import core.image.IJImage;
import core.image.Image;
import core.image.ImageType;
import core.image.Region;
import core.image.iterator.Iterator;
import core.progress.ProgressEvent;
import core.progress.ProgressListener;
import core.progress.ProgressNotifier;
import core.progress.event.EGVDProgressEvent;
import core.util.ImageUtility;

public class EGVD {

	private EGVDParams params;
	private Image paddedCellImage;
	private Image paddedSeedImage;
	private Image choiceImage;
	private Image lastGVDLines;
	private Image lastEvolvedImage;
	private SimpleThresholder simpleThresholder;
	private ProgressNotifier progressNotifier;
	private int currentLevel;
	private boolean isCancelled = false;

	public EGVD(EGVDParams params) {
		this.params = params;
		simpleThresholder = new SimpleThresholder();
		prepareCellImage();
		prepareSeedImage();
		createProgressNotifier();

	}

	public void doEGVD() throws MultipleChoiceOnSameRegionException,
			EmptyImageException, InvalidImageTypeException,
			InvalidImageDimensionException {
		validate();

		Image thresholdedImage;
		createChoiceImage();
		lastGVDLines = getFirstGVDLines();
		Thresholder thresholder = createThresholder();
		currentLevel = 0;

		while (thresholder.hasNextThreshold() && !isCancelled()) {	
			thresholdedImage = thresholder.nextThreshold();
			unionThresholdedAndSeedImages(thresholdedImage);			
			lastEvolvedImage = evolveGVDLines(lastGVDLines, thresholdedImage);
			if (isCancelled()) {
				break;
			} else {
				notifyProgress();
			}
		}

		prepareResultImages();
	}

	private Thresholder createThresholder() {
		ThresholderParams cellParams = params.cellThresholdParams;
		cellParams.image = paddedCellImage;
		Thresholder thresholder = new Thresholder(cellParams);
		return thresholder;
	}

	private void prepareResultImages() {
		prepareRegionImage();
		prepareGVDLinesImage();
	}

	private void validate() throws EmptyImageException,
			InvalidImageTypeException, InvalidImageDimensionException {
		if (isImageEmpty() || isSeedEmpty()) {
			throw new EmptyImageException();
		}
		if (!isImageIJImage() || !isSeedIJImage()) {
			throw new InvalidImageTypeException();
		}
		if (!ImageUtility.isSameDimensions(params.cellImage, params.seedImage)) {
			throw new InvalidImageDimensionException();
		}
	}

	private void prepareGVDLinesImage() {
		if (isCancelled()) {
			lastGVDLines = null;
		} else {
			lastGVDLines = ImageUtility.convertToBinary(lastGVDLines);
			lastGVDLines = ImageUtility.removePadding(lastGVDLines);
		}
	}

	private void prepareRegionImage() {
		if (isCancelled()) {
			lastEvolvedImage = null;
		} else {
			lastEvolvedImage = ImageUtility.convertToBinary(lastEvolvedImage);
			lastEvolvedImage = ImageUtility.removePadding(lastEvolvedImage);
		}
	}

	private void unionThresholdedAndSeedImages(Image thresholdedImage) {
		Iterator itr = paddedSeedImage.getIterator();
		while (itr.hasNext()) {
			if (itr.get() != paddedSeedImage.getMinPossibleValue()) {
				thresholdedImage.set(itr.getPosition(),
						thresholdedImage.getMaxPossibleValue());
			}
			itr.next();
		}
	}

	private void prepareCellImage() {
		if (!isImageEmpty() && isImageIJImage()) {
			Image cellImage = convertImageToGrayScale(params.cellImage);
			paddedCellImage = ImageUtility.convertToArray(cellImage);
			paddedCellImage = ImageUtility.addPadding(paddedCellImage);
			params.cellThresholdParams.max = paddedCellImage.getMax();
		}
	}

	private void prepareSeedImage() {
		if (!isSeedEmpty()
				&& isSeedIJImage()
				&& ImageUtility.isSameDimensions(params.cellImage,
						params.seedImage)) {
			Image seedImage = convertImageToGrayScale(params.seedImage);
			paddedSeedImage = ImageUtility.convertToArray(seedImage);
			paddedSeedImage = ImageUtility.addPadding(paddedSeedImage);
			thresholdSeedImage();
			maskSeedImage();
		}
	}

	private void maskSeedImage() {
		RegionAnalyser regionAnalyser = new RegionAnalyser(paddedSeedImage);
		ArrayList<Region> regions = regionAnalyser.doAnalysis();
		RegionSizeFilter regionSizeFilter = new RegionSizeFilter(regions);
		ArrayList<Region> regionsToMask = regionSizeFilter
				.getRegionsSmallerThan(params.particleSize);
		RegionMaskerParams seedMaskerParams = new RegionMaskerParams();
		seedMaskerParams.image = paddedSeedImage;
		seedMaskerParams.isEnabled = true;
		seedMaskerParams.regionsToMask = regionsToMask;
		RegionMasker masker = new RegionMasker(seedMaskerParams);
		paddedSeedImage = masker.doMask();
	}

	private Image convertImageToGrayScale(Image image) {
		IJImage ijImage = (IJImage) image;
		if (ijImage.getDataSize() == 24) {
			ijImage.convertToGrayScale();
		}
		return ijImage;
	}

	private void thresholdSeedImage() {
		paddedSeedImage = simpleThresholder.doThreshold(paddedSeedImage,
				params.seedThreshold);
	}

	private void createChoiceImage() {
		Choice choice = new Choice(paddedSeedImage);
		choiceImage = choice.doChoice();
	}

	private Image evolveGVDLines(Image gvdLines, Image thresholdedImage)
			throws MultipleChoiceOnSameRegionException {

		ImageSegmenter segmenter = new ImageSegmenter();
		Image segmentedImage = null;
		Image evolvedRegionImage = thresholdedImage;

		int count = 0;
		boolean done = false;
		while (!done) {
			count++;
			segmentedImage = getSegmentedImage(gvdLines, segmenter,
					segmentedImage, evolvedRegionImage);
			evolvedRegionImage = getChoiceRegions(segmentedImage);
			gvdLines = getGVDLines(evolvedRegionImage);
			if (isSameGVDLine(gvdLines)) {
				done = true;
			}
		}
		return evolvedRegionImage;
	}

	private Image getSegmentedImage(Image gvdLines, ImageSegmenter segmenter,
			Image segmentedImage, Image evolvedRegionImage) {
		try {
			segmentedImage = segmenter.segmentImageUsingWatershedLines(
					evolvedRegionImage, gvdLines);
		} catch (InvalidImageDimensionException e) {
			// there is no need to catch the exception here
			// as it has been catered at the start of doEGVD
		}
		return segmentedImage;
	}

	private boolean isSameGVDLine(Image gvdLines) {
		boolean isSame = false;

		isSame = (lastGVDLines != null && gvdLines.equals(lastGVDLines));
		if (!isSame) {
			lastGVDLines = gvdLines;
		}
		return isSame;
	}

	private Image getFirstGVDLines() {
		Image binarySeedImage = ImageUtility.convertToBinary(paddedSeedImage);
		return getGVDLines(binarySeedImage);
	}

	private Image getGVDLines(Image image) {
		BinaryWatershed ws = new BinaryWatershed(image);
		ws.doWatershed();
		return ws.getWatershedLines();
	}

	private Image getChoiceRegions(Image image)
			throws MultipleChoiceOnSameRegionException {
		ChoiceRegionDetector choiceRegionDetector = new ChoiceRegionDetector(
				choiceImage, image);
		Image choiceRegion = null;
		try {
			choiceRegion = choiceRegionDetector.doChoiceRegionDetection();
		} catch (InvalidImageDimensionException e) {
			// there is no need to catch the exception here
			// as it has been catered at the start of doEGVD
		}
		return choiceRegion;
	}

	private void notifyProgress() {
		currentLevel++;
		progressNotifier.notifyProgress();
	}

	private void createProgressNotifier() {
		progressNotifier = new ProgressNotifier() {

			@Override
			public ProgressEvent createProgressEvent() {
				EGVDProgressEvent event = new EGVDProgressEvent(this);
				event.currentLevel = currentLevel;
				event.totalLevels = params.cellThresholdParams.thresholdLevels;
				return event;
			}
		};
	}

	public Image getGVDLines() {
		return lastGVDLines;
	}

	public Image getRegionImage() {
		return lastEvolvedImage;
	}

	public void addProgressListener(ProgressListener progressListener) {
		progressNotifier.addProgressListener(progressListener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressNotifier.removeProgressListener(listener);
	}

	private boolean isImageEmpty() {
		return params.cellImage == null;
	}

	private boolean isSeedEmpty() {
		return params.seedImage == null;
	}

	private boolean isImageIJImage() {
		return params.cellImage.getImageType() == ImageType.IJ;
	}

	private boolean isSeedIJImage() {
		return params.seedImage.getImageType() == ImageType.IJ;
	}

	public void cancel() {
		isCancelled = true;
	}

	public boolean isCancelled() {
		return isCancelled;
	}
}
