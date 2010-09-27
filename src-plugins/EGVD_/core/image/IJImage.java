package core.image;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;

public class IJImage extends Image {

	private ImagePlus imp;
	private ImageProcessor[] ips;
	
	@Override
	public int get(int x, int y, int z) {
		return ips[z].get(x, y);
	}

	@Override
	public int get(int position) {
		Pixel point = mapper.get(position);
		return get(point.x, point.y, point.z);
	}

	@Override
	public ImageType getImageType() {
		return ImageType.IJ;
	}

	@Override
	public void loadImage(String filename) {
		imp = new ImagePlus(filename);
		storeDimensions(imp.getWidth(), imp.getHeight(), imp.getImageStack()
				.getSize());
		referenceImageProcessors();
		createLinearMapper();
	}

	private void referenceImageProcessors() {
		ImageStack is = imp.getImageStack();
		ips = new ImageProcessor[depth];
		for (int i = 0; i < depth; i++) {
			ips[i] = is.getProcessor(i + 1);
		}
	}

	@Override
	public void newImage() {
		imp = NewImage.createByteImage(null, width, height, depth,
				NewImage.FILL_BLACK);
		referenceImageProcessors();
	}

	@Override
	public void set(int x, int y, int z, int value) {
		ips[z].set(x, y, value);
	}

	@Override
	public void set(int position, int value) {
		Pixel point = mapper.get(position);
		set(point.x, point.y, point.z, value);
	}

	@Override
	public int get(int x, int y, int z, int position) {
		return get(x, y, z);
	}

	@Override
	public int getIterationSize() {
		return size;
	}

	@Override
	public void set(int x, int y, int z, int position, int value) {
		set(x, y, z, value);
	}

	@Override
	protected void duplicateData(Image image) {
		IJImage sourceImage = (IJImage) image;
		Duplicator dup = new Duplicator();
		this.imp = dup.run(sourceImage.imp);
		referenceImageProcessors();
	}

	public void convertToGrayScale() {
		ImageStack is = new ImageStack(width, height);
		
		for (int i = 0; i < ips.length; i++) {
			is.addSlice("", ips[i].convertToByte(false));
		}
		
		imp.setStack("", is);
		referenceImageProcessors();
	}

	public void show() {
		imp.show();
	}

	public int getDataSize() {
		return imp.getBitDepth();
	}
	
	public ImagePlus getImagePlus() {
		return imp;
	}

	@Override
	public int getMaxPossibleValue() {
		int max = Integer.MAX_VALUE;
		if (getDataSize() == 8) {
			max = 255;
		}
		if (getDataSize() == 16) {
			max = 65535;
		}
		return max;
	}

}
