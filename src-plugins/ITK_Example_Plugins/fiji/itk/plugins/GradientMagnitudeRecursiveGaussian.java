//
// GradientMagnitudeRecursiveGaussian.java
//

/*
Example ImageJ plugins to call ITK routines via imglib.
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

package fiji.itk.plugins;

import fiji.itk.ItkException;
import fiji.itk.base.ItkImage;
import fiji.itk.filtering.ItkGradientMagnitudeRecursiveGaussianImageFilter;
import fiji.itk.intensityfilters.ItkRescaleIntensityImageFilter;
import fiji.itk.util.ImgLibTools;
import fiji.itk.util.ItkTools;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.FloatType;
import mpicbg.imglib.type.numeric.UnsignedByteType;

/**
 * An example plugin that calls ITK via imglib to perform a
 * gradient magnitude recursive Gaussian image filter.
 * 
 * It is based on one of the ITK examples,
 * Filtering/GradientMagnitudeRecursiveGaussianImageFilter.cxx,
 * which is documented in the ITK Software Guide in section 6.4.2,
 * Gradient Magnitude With Smoothing.
 * 
 * @author Curtis Rueden ctrueden at wisc.edu
 */
public class GradientMagnitudeRecursiveGaussian implements PlugInFilter {

	private ImagePlus image;

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_32;
	}

	public void run(ImageProcessor ip) {
		run(image);
		image.updateAndDraw();
	}

	public void run(ImagePlus image) {
		// prompt for parameters
		float sigma = 5;
		GenericDialog gd = new GenericDialog("Set Parameters");
		gd.addNumericField("Sigma", sigma, 0);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		sigma = (float) gd.getNextNumber();

		// process image
		Image<FloatType> img = ImagePlusAdapter.wrapFloat(image);
		try {
			Image<UnsignedByteType> result = process(img, sigma);
			ImgLibTools.display(result);
		}
		catch (ItkException exc) {
			IJ.showMessage(ItkTools.NO_ITK_MSG);
			IJ.handleException(exc);
		}
	}
	
	public Image<UnsignedByteType> process(Image<FloatType> img, float sigma)
		throws ItkException
	{
		// convert from imglib to ITK image
		IJ.showStatus("Creating ITK image");
		ItkImage inputImage = ItkTools.createITKImage(img, ItkImage.TYPE_FLOAT, 2);

		// apply gradient magnitude recursive Gaussian filter
		IJ.showStatus("Applying filters");
		ItkGradientMagnitudeRecursiveGaussianImageFilter filter =
			new ItkGradientMagnitudeRecursiveGaussianImageFilter(
			ItkImage.TYPE_FLOAT, 2, ItkImage.TYPE_FLOAT, 2);
		filter.setInput(inputImage);
		filter.setSigma(sigma);
		filter.update();
		ItkImage filteredImage = filter.getOutput();

		// apply intensity rescaling filter
		ItkRescaleIntensityImageFilter rescaler =
			new ItkRescaleIntensityImageFilter(
			ItkImage.TYPE_FLOAT, 2, ItkImage.TYPE_UINT8, 2);
		rescaler.setInput(filteredImage);
		rescaler.setOutputMinimum((short) 0);
		rescaler.setOutputMaximum((short) 255);
		ItkImage rescaledImage = rescaler.getOutput();

		// convert from ITK to imglib image
		IJ.showStatus("Generating output image");
		ArrayContainerFactory acf = new ArrayContainerFactory();
		ImageFactory<UnsignedByteType> imageFactory =
			new ImageFactory<UnsignedByteType>(new UnsignedByteType(), acf);
		Image<UnsignedByteType> imgOut =
			ItkTools.createImage(rescaledImage, imageFactory);
		return imgOut;
	}
	
}
