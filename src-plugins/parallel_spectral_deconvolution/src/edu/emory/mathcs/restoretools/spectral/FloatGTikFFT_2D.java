/*
 *  Copyright 2008 Piotr Wendykier
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.emory.mathcs.restoretools.spectral;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import optimization.FloatFmin;
import optimization.FloatFmin_methods;
import cern.colt.matrix.AbstractMatrix2D;
import cern.colt.matrix.tfcomplex.FComplexMatrix2D;
import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import cern.jet.math.tfcomplex.FComplexFunctions;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Generalized Tikhonov image deblurring 2D using the FFT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatGTikFFT_2D {

	private AbstractMatrix2D B;

	private AbstractMatrix2D PSF;

	private AbstractMatrix2D Pd;

	private AbstractMatrix2D Sa;

	private AbstractMatrix2D Sd;

	private FComplexMatrix2D ConjSa;

	private FComplexMatrix2D ConjSd;

	private java.awt.image.ColorModel cmY;

	private int bColsPad;

	private int bRowsPad;

	private int bCols;

	private int bRows;

	private int bColsOff;

	private int bRowsOff;

	private int[] psfCenter;

	private boolean isPadded = false;

	private OutputType output;

	/**
	 * Constructs new FloatGTikFFT_2D.
	 * 
	 * @param imB
	 *            blurred image
	 * @param imPSF
	 *            point spread function
	 * @param stencil
	 *            3-by-3 stencil for regularization operator
	 * @param resizing
	 *            type of resizing
	 * @param output
	 *            type of an output
	 * @param showPadded
	 *            if true then padded image is displayed
	 */
	public FloatGTikFFT_2D(ImagePlus imB, ImagePlus imPSF, FloatMatrix2D stencil, ResizingType resizing, OutputType output, boolean showPadded) {
		if ((stencil.rows() != 3) || (stencil.columns() != 3)) {
			throw new IllegalArgumentException("Illegal stencil for regularization operator");
		}
		ImageProcessor ipB = imB.getProcessor();
		ImageProcessor ipPSF = imPSF.getProcessor();
		if (output == OutputType.SAME_AS_SOURCE) {
			if (ipB instanceof ByteProcessor) {
				this.output = OutputType.BYTE;
			} else if (ipB instanceof ShortProcessor) {
				this.output = OutputType.SHORT;
			} else if (ipB instanceof FloatProcessor) {
				this.output = OutputType.FLOAT;
			} else {
				throw new IllegalArgumentException("Unsupported image type.");
			}
		} else {
			this.output = output;
		}
		cmY = ipB.getColorModel();
		int kCols = ipPSF.getWidth();
		int kRows = ipPSF.getHeight();
		bCols = ipB.getWidth();
		bRows = ipB.getHeight();
		if ((kRows > bRows) || (kCols > bCols)) {
			throw new IllegalArgumentException("The PSF image cannot be larger than the blurred image.");
		}
		IJ.showStatus("Generalized Tikhonov: initializing");
		if (resizing == ResizingType.NEXT_POWER_OF_TWO) {
			if (ConcurrencyUtils.isPowerOf2(bRows)) {
				bRowsPad = bRows;
			} else {
				isPadded = true;
				bRowsPad = ConcurrencyUtils.nextPow2(bRows);
			}
			if (ConcurrencyUtils.isPowerOf2(bCols)) {
				bColsPad = bCols;
			} else {
				isPadded = true;
				bColsPad = ConcurrencyUtils.nextPow2(bCols);
			}
		} else {
			bColsPad = bCols;
			bRowsPad = bRows;
		}
		B = FloatCommon_2D.assignPixelsToMatrix_2D(ipB);
		if (isPadded) {
			B = FloatCommon_2D.padPeriodic_2D((FloatMatrix2D) B, bColsPad, bRowsPad);
			bColsOff = (bColsPad - bCols + 1) / 2;
			bRowsOff = (bRowsPad - bRows + 1) / 2;
		}
		PSF = FloatCommon_2D.assignPixelsToMatrix_2D(ipPSF);
		float[] maxAndLoc = ((FloatMatrix2D) PSF).getMaxLocation();
		psfCenter = new int[] { (int) maxAndLoc[1], (int) maxAndLoc[2] };
		((FloatMatrix2D) PSF).normalize();
		if ((kCols != bColsPad) || (kRows != bRowsPad)) {
			PSF = FloatCommon_2D.padZero_2D((FloatMatrix2D) PSF, bColsPad, bRowsPad);
		}
		psfCenter[0] += (bRowsPad - kRows + 1) / 2;
		psfCenter[1] += (bColsPad - kCols + 1) / 2;
		Pd = new DenseFloatMatrix2D(bRowsPad, bColsPad);
		((FloatMatrix2D) Pd).viewPart(0, 0, 3, 3).assign(stencil);
		if (showPadded && isPadded) {
			FloatProcessor ipTemp = new FloatProcessor(bColsPad, bRowsPad);
			FloatCommon_2D.assignPixelsToProcessor(ipTemp, (FloatMatrix2D) B, cmY);
			ImagePlus imTemp = new ImagePlus("", ipTemp);
			imTemp.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + " (padded)"));
			imTemp.show();
			imTemp.setRoi(bColsOff, bRowsOff, bCols, bRows);
		}
	}

	/**
	 * Returns deblurred image. Uses Generalized Cross-Validation (GCV) to
	 * compute regularization parameter.
	 * 
	 * @param threshold
	 *            the smallest positive value assigned to the restored image,
	 *            all the values less than the threshold are set to zero
	 * 
	 * @return deblurred image
	 */
	public ImagePlus deblur(float threshold) {
		IJ.showStatus("Generalized Tikhonov: deblurring");
		Sa = FloatCommon_2D.circShift_2D((FloatMatrix2D) PSF, psfCenter);
		Sa = ((FloatMatrix2D) Sa).getFft2();
		B = ((FloatMatrix2D) B).getFft2();
		Sd = FloatCommon_2D.circShift_2D((FloatMatrix2D) Pd, new int[] { 1, 1 });
		Sd = ((FloatMatrix2D) Sd).getFft2();
		IJ.showStatus("Generalized Tikhonov: computing regularization parameter");
		float alpha = gcvGTikFFT_2D((FComplexMatrix2D) Sa, (FComplexMatrix2D) Sd, (FComplexMatrix2D) B);
		IJ.showStatus("Generalized Tikhonov: deblurring");
		ConjSa = ((FComplexMatrix2D) Sa).copy();
		ConjSa.assign(FComplexFunctions.conj);
		ConjSd = ((FComplexMatrix2D) Sd).copy();
		ConjSd.assign(FComplexFunctions.conj);
		ConjSd.assign((FComplexMatrix2D) Sd, FComplexFunctions.mult);
		Pd = ConjSd.copy();
		((FComplexMatrix2D) Pd).assign(FComplexFunctions.mult(alpha * alpha));
		PSF = ConjSa.copy();
		((FComplexMatrix2D) PSF).assign((FComplexMatrix2D) Sa, FComplexFunctions.mult);
		Sd = ((FComplexMatrix2D) PSF).copy();
		((FComplexMatrix2D) PSF).assign((FComplexMatrix2D) Pd, FComplexFunctions.plus);
		((FComplexMatrix2D) B).assign(ConjSa, FComplexFunctions.mult);
		Sa = ((FComplexMatrix2D) B).copy();
		((FComplexMatrix2D) Sa).assign((FComplexMatrix2D) PSF, FComplexFunctions.div);
		((FComplexMatrix2D) Sa).ifft2(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, (FComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, (FComplexMatrix2D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, (FComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, (FComplexMatrix2D) Sa, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred", ip);
		FloatCommon_2D.convertImage(imX, output);
		imX.setProperty("alpha", alpha);
		return imX;
	}

	/**
	 * Returns deblurred image.
	 * 
	 * @param alpha
	 *            regularization parameter
	 * @param threshold
	 *            the smallest positive value assigned to the restored image,
	 *            all the values less than the threshold are set to zero
	 * 
	 * @return deblurred image
	 */
	public ImagePlus deblur(float alpha, float threshold) {
		IJ.showStatus("Generalized Tikhonov: deblurring");
		Sa = FloatCommon_2D.circShift_2D((FloatMatrix2D) PSF, psfCenter);
		Sa = ((FloatMatrix2D) Sa).getFft2();
		B = ((FloatMatrix2D) B).getFft2();
		Sd = FloatCommon_2D.circShift_2D((FloatMatrix2D) Pd, new int[] { 1, 1 });
		Sd = ((FloatMatrix2D) Sd).getFft2();
		ConjSa = ((FComplexMatrix2D) Sa).copy();
		ConjSa.assign(FComplexFunctions.conj);
		ConjSd = ((FComplexMatrix2D) Sd).copy();
		ConjSd.assign(FComplexFunctions.conj);
		ConjSd.assign((FComplexMatrix2D) Sd, FComplexFunctions.mult);
		Pd = ConjSd.copy();
		((FComplexMatrix2D) Pd).assign(FComplexFunctions.mult(alpha * alpha));
		PSF = ConjSa.copy();
		((FComplexMatrix2D) PSF).assign((FComplexMatrix2D) Sa, FComplexFunctions.mult);
		Sd = ((FComplexMatrix2D) PSF).copy();
		((FComplexMatrix2D) PSF).assign((FComplexMatrix2D) Pd, FComplexFunctions.plus);
		((FComplexMatrix2D) B).assign(ConjSa, FComplexFunctions.mult);
		Sa = ((FComplexMatrix2D) B).copy();
		((FComplexMatrix2D) Sa).assign((FComplexMatrix2D) PSF, FComplexFunctions.div);
		((FComplexMatrix2D) Sa).ifft2(true);
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, (FComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, (FComplexMatrix2D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, (FComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, (FComplexMatrix2D) Sa, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred", ip);
		FloatCommon_2D.convertImage(imX, output);
		return imX;
	}

	/**
	 * Updates deblurred image <code>imX</code> with new regularization
	 * parameter <code>alpha</code>.
	 * 
	 * @param alpha
	 *            regularization parameter
	 * @param threshold
	 *            the smallest positive value assigned to the restored image,
	 *            all the values less than the threshold are set to zero
	 * 
	 * @param imX
	 *            deblurred image
	 */
	public void update(float alpha, float threshold, ImagePlus imX) {
		IJ.showStatus("Generalized Tikhonov: updating");
		PSF = ConjSd.copy();
		((FComplexMatrix2D) PSF).assign(FComplexFunctions.mult(alpha * alpha));
		Pd = ((FComplexMatrix2D) Sd).copy();
		((FComplexMatrix2D) Pd).assign((FComplexMatrix2D) PSF, FComplexFunctions.plus);
		Sa = ((FComplexMatrix2D) B).copy();
		((FComplexMatrix2D) Sa).assign((FComplexMatrix2D) Pd, FComplexFunctions.div);
		((FComplexMatrix2D) Sa).ifft2(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, (FComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, (FComplexMatrix2D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, (FComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, (FComplexMatrix2D) Sa, cmY, threshold);
			}
		}
		imX.setProcessor(imX.getTitle(), ip);
		FloatCommon_2D.convertImage(imX, output);
	}

	private float gcvGTikFFT_2D(FComplexMatrix2D Sa, FComplexMatrix2D Sd, FComplexMatrix2D Bhat) {
		AbstractMatrix2D sa = Sa.copy();
		sa = ((FComplexMatrix2D) sa).assign(FComplexFunctions.abs).getRealPart();
		AbstractMatrix2D sd = Sd.copy();
		sd = ((FComplexMatrix2D) sd).assign(FComplexFunctions.abs).getRealPart();
		AbstractMatrix2D bhat = Bhat.copy();
		bhat = ((FComplexMatrix2D) bhat).assign(FComplexFunctions.abs).getRealPart();
		float[] tmp = ((FloatMatrix2D) sa).getMinLocation();
		float smin = tmp[0];
		tmp = ((FloatMatrix2D) sa).getMaxLocation();
		float smax = tmp[0];
		((FloatMatrix2D) sa).assign(FloatFunctions.square);
		((FloatMatrix2D) sd).assign(FloatFunctions.square);
		GTikFmin_2D fmin = new GTikFmin_2D((FloatMatrix2D) sa, (FloatMatrix2D) sd, (FloatMatrix2D) bhat);
		return (float) FloatFmin.fmin(smin, smax, fmin, FloatCommon_2D.FMIN_TOL);
	}

	private class GTikFmin_2D implements FloatFmin_methods {
		FloatMatrix2D sasquare;

		FloatMatrix2D sdsquare;

		FloatMatrix2D bhat;

		public GTikFmin_2D(FloatMatrix2D sasquare, FloatMatrix2D sdsquare, FloatMatrix2D bhat) {
			this.sasquare = sasquare;
			this.sdsquare = sdsquare;
			this.bhat = bhat;
		}

		public float f_to_minimize(float alpha) {
			FloatMatrix2D sdloc = sdsquare.copy();
			FloatMatrix2D denom = sdloc.copy();

			denom.assign(FloatFunctions.mult(alpha * alpha));
			denom.assign(sasquare, FloatFunctions.plus);
			sdloc.assign(denom, FloatFunctions.div);
			denom = bhat.copy();
			denom.assign(sdloc, FloatFunctions.mult);
			denom.assign(FloatFunctions.square);
			float sphi_d = sdloc.zSum();
			return denom.zSum() / (sphi_d * sphi_d);
		}

	}

}
