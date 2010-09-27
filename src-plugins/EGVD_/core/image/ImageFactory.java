package core.image;

public final class ImageFactory {

	public static Image createIJImage() {
		return new IJImage();
	}

	public static Image createArrayImage() {
		return new ArrayImage();
	}

	public static Image createBinaryImage() {
		return new BinaryImage();
	}

	public static Image createSparseBinaryImage() {
		return new SparseBinaryImage();
	}

	public static Image createImage(ImageType type) {
		switch (type) {
		case IJ:
			return createIJImage();
		case ARRAY:
			return createArrayImage();
		case BINARY:
			return createBinaryImage();
		case SPARSE_BINARY:
			return createSparseBinaryImage();
		}
		return null;
	}
}
