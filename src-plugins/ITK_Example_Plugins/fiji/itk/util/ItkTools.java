//
// ItkTools.java
//

/*
Fiji Java reflection wrapper for ITK.
Copyright (c) 2010, UW-Madison LOCI.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the UW-Madison LOCI nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY UW-MADISON LOCI ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL UW-MADISON LOCI BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package fiji.itk.util;

import fiji.FijiClassLoader;
import fiji.itk.ItkException;
import fiji.itk.base.ItkImage;
import fiji.itk.base.ItkImageRegion;
import fiji.itk.base.ItkIndex;
import fiji.itk.base.ItkSize;

import ij.IJ;

import java.io.File;
import java.io.IOException;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.NumericType;

/** A utility class with methods for transforming between imglib and ITK. */
public final class ItkTools {

	private ItkTools() { }
	
	public static final String NO_ITK_MSG =
		"ITK is not installed. Please visit " +
		"http://pacific.mpi-cbg.de/wiki/index.php/ITK " +
		"for instructions on installing ITK.";
	
	// -- General --
	
	/**
	 * Loads any ITK JARs present on the Java library path into the class path.
	 * Unfortunately, we cannot set the Java library path at runtime, but we
	 * can load the wrapped ITK JARs from the library path in this way.
	 *
	 * @throws ItkException if the class path cannot be updated
	 */
	public static void initITK() throws ItkException { 
		try {
			ClassLoader classLoader = IJ.getClassLoader();
			if (classLoader instanceof FijiClassLoader) {
				FijiClassLoader fijiLoader = (FijiClassLoader) classLoader;
				String libraryPath = System.getProperty("java.library.path");
				String sep = System.getProperty("path.separator");
				String[] libraryDirs = libraryPath.split(sep);
				for (String libraryDir : libraryDirs) {
					File dir = new File(libraryDir);
					if (dir == null || !dir.isDirectory()) continue;
					File[] files = dir.listFiles();
					for (File f : files) {
						String name = f.getName();
						if (name.startsWith("org.itk.") && name.endsWith(".jar")) {
							fijiLoader.addPath(f.getAbsolutePath());
						}
					}
				}
			}
		}
		catch (IOException exc) {
			throw new ItkException(exc);
		}
	}
	
	/** Creates an ITK image from an imglib image. */
	public static <T extends NumericType<T>> ItkImage createITKImage(Image<T> img,
		String type, int dim) throws ItkException
	{
		int[] dims = img.getDimensions();
		if (dims.length != dim) {
			throw new IllegalArgumentException("Dimensionality mismatch: " +
				"dim = " + dim + ", img = " + dims.length);
		}
		ItkImage itkImage = newImage(type, dims);
		copyToITK(img, itkImage);
		return itkImage;
	}

	/** Creates an imglib image from an ITK image. */
	public static <T extends NumericType<T>> Image<T> createImage(
		ItkImage itkImage, ImageFactory<T> imageFactory) throws ItkException
	{
		itkImage.update();
		int[] dims = getDims(itkImage.getLargestPossibleRegion());
		Image<T> img = imageFactory.createImage(dims);
		copyFromITK(itkImage, img);
		return img;
	}

	/**
	 * Copies the data from the given imglib image
	 * into the specified ITK image.
	 * @param src The imglib image source.
	 * @param dest The ITK image destination.
	 */
	public static <T extends NumericType<T>> void copyToITK(Image<T> src,
		ItkImage dest) throws ItkException
	{
		// NB: We could avoid the hard dependency on ITK here by reflecting this...
		ItkTranslator.copyToITK(src, dest);
	}

	/**
	 * Copies the data from the given ITK image into the specified imglib image.
	 * @param src The ITK image source.
	 * @param dest The imglib image destination.
	 */
	public static <T extends NumericType<T>> void copyFromITK(ItkImage src,
		Image<T> dest) throws ItkException
	{
		// NB: We could avoid the hard dependency on ITK here by reflecting this...
		ItkTranslator.copyFromITK(src, dest);
	}

	/** Creates an ITK image of the given type with the specified dimensions. */
	public static ItkImage newImage(String type, int[] dims) throws ItkException {
		ItkImage image = new ItkImage(type, dims.length);
		ItkImageRegion region = createRegion(dims);
		image.setRegions(region);
		image.allocate();
		return image;
	}

	/** Creates an ITK region from the given dimensions. */
	public static ItkImageRegion createRegion(int[] dims) throws ItkException {
		int[] pos = new int[dims.length];
		ItkIndex start = createIndex(pos);
		ItkSize size = createSize(dims);
		ItkImageRegion region = new ItkImageRegion(dims.length);
		region.setSize(size);
		region.setIndex(start);
		return region;
	}
	
	/** Gets the dimensions of an ITK region. */
	public static int[] getDims(ItkImageRegion region) throws ItkException {
		ItkSize size = region.getSize();
		int[] dims = new int[size.getDimension()];
		for (int i=0; i<dims.length; i++) {
			dims[i] = (int) size.getElement(i);
		}
		return dims;
	}

	/** Creates an ITK position index from the given coordinates. */
	private static ItkIndex createIndex(int[] pos) throws ItkException {
		ItkIndex index = new ItkIndex(pos.length);
		for (int i=0; i<pos.length; i++) {
			index.setElement(i, pos[i]);
		}
		return index;
	}

	/** Creates an ITK size container from the given dimensions. */
	public static ItkSize createSize(int[] dims) throws ItkException {
		ItkSize size = new ItkSize(dims.length);
		for (int i=0; i<dims.length; i++) {
			size.setElement(i, dims[i]);
		}
		return size;
	}

}
