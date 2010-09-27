package core.util;

import ij.IJ;

import java.io.File;
import java.io.IOException;

import core.image.IJImage;
import core.image.Image;
import core.image.ImageFactory;
import core.image.ImageType;
import core.image.iterator.Iterator;

public class ImageUtility {

	public static final int PAD_OFFSET = 1;
	public static final int PAD = 2;

	public static int clamp(int input, int min, int max) {
		int result = input;

		if (result < min) {
			result = min;
		} else {
			if (result > max) {
				result = max;
			}
		}

		return result;
	}

	public static Image createPaddedImage(Image image, ImageType type) {
		int newWidth = image.getWidth() + PAD;
		int newHeight = image.getHeight() + PAD;
		int newDepth = image.getDepth();
		if (image.is3D()) {
			newDepth += PAD;
		}
		Image paddedImage = ImageFactory.createImage(type);
		paddedImage.create(newWidth, newHeight, newDepth);
		return paddedImage;
	}

	public static Image createUnpaddedImage(Image image, ImageType type) {
		int newWidth = image.getWidth() - PAD;
		int newHeight = image.getHeight() - PAD;
		int newDepth = image.getDepth();
		if (image.is3D()) {
			newDepth -= PAD;
		}
		Image unpaddedImage = ImageFactory.createImage(type);
		unpaddedImage.create(newWidth, newHeight, newDepth);
		return unpaddedImage;
	}

	public static Image convertToBinary(Image image) {
		Image binaryImage = ImageFactory.createBinaryImage();
		binaryImage.create(image);
		Iterator itr = image.getIterator();
		while (itr.hasNext()) {
			if (itr.get() != image.getMinPossibleValue()) {
				binaryImage.set(itr.getPosition(),
						binaryImage.getMaxPossibleValue());
			}
			itr.next();
		}
		return binaryImage;
	}

	public static Image convertToArray(Image image) {
		Image arrayImage = ImageFactory.createArrayImage();
		arrayImage.create(image);
		Iterator itr = image.getIterator();
		while (itr.hasNext()) {
			arrayImage.set(itr.getPosition(), itr.get());
			itr.next();
		}
		return arrayImage;
	}

	public static Image addPadding(Image image) {
		int depthOffset = 0;
		if (image.is3D()) {
			depthOffset = PAD_OFFSET;
		}
		Image paddedImage = createPaddedImage(image, image.getImageType());
		Iterator itr = image.getIterator();
		LinearMapper mapper = new LinearMapper(paddedImage);
		while (itr.hasNext()) {
			paddedImage.set(mapper.get(itr.getX() + PAD_OFFSET, itr.getY()
					+ PAD_OFFSET, itr.getZ() + depthOffset), itr.get());
			itr.next();
		}
		return paddedImage;
	}

	public static Image removePadding(Image image) {
		int depthOffset = 0;
		if (image.is3D()) {
			depthOffset = PAD_OFFSET;
		}
		Image unpaddedImage = createUnpaddedImage(image, image.getImageType());
		Iterator itr = unpaddedImage.getIterator();
		LinearMapper mapper = new LinearMapper(image);
		while (itr.hasNext()) {
			itr.set(image.get(mapper.get(itr.getX() + PAD_OFFSET, itr.getY()
					+ PAD_OFFSET, itr.getZ() + depthOffset)));
			itr.next();
		}
		return unpaddedImage;
	}

	public static void setBoundary(int[] array, int width, int height,
			int depth, int value) {
		LinearMapper mapper = new LinearMapper(width, width * height);
		if (depth > 1) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					array[mapper.get(x, y, 0)] = value;
					array[mapper.get(x, y, depth - 1)] = value;
				}
			}
		}

		for (int z = 0; z < depth; z++) {
			for (int x = 0; x < width; x++) {
				array[mapper.get(x, 0, z)] = value;
				array[mapper.get(x, height - 1, z)] = value;
			}
		}
		for (int z = 0; z < depth; z++) {
			for (int y = 0; y < height; y++) {
				array[mapper.get(0, y, z)] = value;
				array[mapper.get(width - 1, y, z)] = value;
			}
		}
	}

	public static void saveImage(Image image, String filename) {
		IJImage ijImage = (IJImage)convertToIJ(image);
		IJ.save(ijImage.getImagePlus(), filename);
	}

	public static void saveImage(Image binaryImage, Image sparseImage,
			String filename) {
		Iterator itr = sparseImage.getIterator();
		while (itr.hasNext()) {
			binaryImage.set(itr.getPosition(),
					binaryImage.getMaxPossibleValue());
			itr.next();
		}
		saveImage(binaryImage, filename);
	}

	public static String getDirectory() {
		String dir = "";
		try {
			dir = new File(".").getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dir;
	}

	public static String getTestImageDirectory() {
		return getDirectory() + "\\test\\images\\";
	}

	public static boolean isSameDimensions(Image image1, Image image2) {
		return (image1.getWidth() == image2.getWidth()
				&& image1.getHeight() == image2.getHeight() && image1
				.getDepth() == image2.getDepth());
	}

	public static Image convertToIJ(Image image) {
		Image ijImage = ImageFactory.createIJImage();
		ijImage.create(image);
		Iterator itr = image.getIterator();
		if (image.getImageType() == ImageType.BINARY
				|| image.getImageType() == ImageType.SPARSE_BINARY) {
			convertToIJForBinary(image, ijImage, itr);
		} else {
			convertToIJ(ijImage, itr);
		}
		return ijImage;
	}

	private static void convertToIJForBinary(Image image, Image ijImage,
			Iterator itr) {
		while (itr.hasNext()) {
			if (itr.get() == image.getMaxPossibleValue()) {
				ijImage.set(itr.getX(), itr.getY(), itr.getZ(),
						ijImage.getMaxPossibleValue());
			}
			itr.next();
		}
	}

	private static void convertToIJ(Image ijImage, Iterator itr) {
		while (itr.hasNext()) {
			ijImage.set(itr.getX(), itr.getY(), itr.getZ(), itr.get());
			itr.next();
		}
	}
}
