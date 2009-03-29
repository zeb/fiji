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
import ij.ImageStack;
import ij.WindowManager;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import optimization.FloatFmin;
import optimization.FloatFmin_methods;
import cern.colt.matrix.AbstractMatrix3D;
import cern.colt.matrix.tfcomplex.FComplexMatrix3D;
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import cern.jet.math.tfcomplex.FComplexFunctions;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Generalized Tikhonov image deblurring 3D using the FFT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatGTikFFT_3D {
	private AbstractMatrix3D B;

	private AbstractMatrix3D PSF;

	private AbstractMatrix3D Pd;

	private AbstractMatrix3D Sa;

	private AbstractMatrix3D Sd;

	private FComplexMatrix3D ConjSa;

	private FComplexMatrix3D ConjSd;

	private java.awt.image.ColorModel cmY;

	private int bSlicesPad;

	private int bColsPad;

	private int bRowsPad;

	private int bSlices;

	private int bCols;

	private int bRows;

	private int bSlicesOff;

	private int bRowsOff;

	private int bColsOff;

	private int[] psfCenter;

	private boolean isPadded = false;

	private OutputType output;

	/**
	 * Constructs new FloatGTikFFT_3D.
	 * 
	 * @param imB
	 *            blurred image
	 * @param imPSF
	 *            point spread function
	 * @param stencil
	 *            3-by-3-by-3 stencil for regularization operator
	 * @param resizing
	 *            type of resizing
	 * @param output
	 *            type of an output
	 * @param showPadded
	 *            if true then padded image is displayed
	 */
	public FloatGTikFFT_3D(ImagePlus imB, ImagePlus imPSF, FloatMatrix3D stencil, ResizingType resizing, OutputType output, boolean showPadded) {
		if ((stencil.slices() != 3) || (stencil.rows() != 3) || (stencil.columns() != 3)) {
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
		ImageStack isB = imB.getStack();
		ImageStack isPSF = imPSF.getStack();
		int kSlices = isPSF.getSize();
		int kCols = ipPSF.getWidth();
		int kRows = ipPSF.getHeight();
		bSlices = isB.getSize();
		bCols = ipB.getWidth();
		bRows = ipB.getHeight();
		if ((kSlices > bSlices) || (kRows > bRows) || (kCols > bCols)) {
			throw new IllegalArgumentException("The PSF image cannot be larger than the blurred image.");
		}
		IJ.showStatus("Generalized Tikhonov: initializing");
		if (resizing == ResizingType.NEXT_POWER_OF_TWO) {
			if (ConcurrencyUtils.isPowerOf2(bSlices)) {
				bSlicesPad = bSlices;
			} else {
				isPadded = true;
				bSlicesPad = ConcurrencyUtils.nextPow2(bSlices);
			}
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
			bSlicesPad = bSlices;
			bRowsPad = bRows;
			bColsPad = bCols;
		}
		B = new DenseFloatMatrix3D(bSlices, bRows, bCols);
		FloatCommon_3D.assignPixelsToMatrix_3D(isB, (FloatMatrix3D) B);
		if (isPadded) {
			B = FloatCommon_3D.padPeriodic_3D((FloatMatrix3D) B, bSlicesPad, bRowsPad, bColsPad);
			bSlicesOff = (bSlicesPad - bSlices + 1) / 2;
			bRowsOff = (bRowsPad - bRows + 1) / 2;
			bColsOff = (bColsPad - bCols + 1) / 2;
		}
		PSF = new DenseFloatMatrix3D(kSlices, kRows, kCols);
		FloatCommon_3D.assignPixelsToMatrix_3D(isPSF, (FloatMatrix3D) PSF);
		float[] maxAndLoc = ((FloatMatrix3D) PSF).getMaxLocation();
		psfCenter = new int[] { (int) maxAndLoc[1], (int) maxAndLoc[2], (int) maxAndLoc[3] };
		((FloatMatrix3D) PSF).normalize();
		if (kSlices != bSlicesPad || kRows != bRowsPad || kCols != bColsPad) {
			PSF = FloatCommon_3D.padZero_3D((FloatMatrix3D) PSF, bSlicesPad, bRowsPad, bColsPad);
		}
		psfCenter[0] += (bSlicesPad - kSlices + 1) / 2;
		psfCenter[1] += (bRowsPad - kRows + 1) / 2;
		psfCenter[2] += (bColsPad - kCols + 1) / 2;
		Pd = new DenseFloatMatrix3D(bSlicesPad, bRowsPad, bColsPad);
		((FloatMatrix3D) Pd).viewPart(0, 0, 0, 3, 3, 3).assign(stencil);
		if (showPadded && isPadded) {
			ImageStack stackTemp = new ImageStack(bColsPad, bRowsPad);
			FloatCommon_3D.assignPixelsToStack(stackTemp, (FloatMatrix3D) B, cmY);
			ImagePlus impTemp = new ImagePlus("", stackTemp);
			impTemp.setTitle(WindowManager.makeUniqueName(imB.getShortTitle() + " (padded)"));
			impTemp.show();
			impTemp.setRoi(bColsOff, bRowsOff, bCols, bRows);
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
		Sa = FloatCommon_3D.circShift_3D((FloatMatrix3D) PSF, psfCenter);
		Sd = FloatCommon_3D.circShift_3D((FloatMatrix3D) Pd, psfCenter);
		Sa = ((FloatMatrix3D) Sa).getFft3();
		Sd = ((FloatMatrix3D) Sd).getFft3();
		B = ((FloatMatrix3D) B).getFft3();
		IJ.showStatus("Generalized Tikhonov: computing regularization parameter");
		float alpha = gcvGTikFFT_3D((FComplexMatrix3D) Sa, (FComplexMatrix3D) Sd, (FComplexMatrix3D) B);
		IJ.showStatus("Generalized Tikhonov: deblurring");
		ConjSa = ((FComplexMatrix3D) Sa).copy();
		ConjSa.assign(FComplexFunctions.conj);
		ConjSd = ((FComplexMatrix3D) Sd).copy();
		ConjSd.assign(FComplexFunctions.conj);
		ConjSd.assign((FComplexMatrix3D) Sd, FComplexFunctions.mult);
		Pd = ConjSd.copy();
		((FComplexMatrix3D) Pd).assign(FComplexFunctions.mult(alpha * alpha));
		PSF = ConjSa.copy();
		((FComplexMatrix3D) PSF).assign((FComplexMatrix3D) Sa, FComplexFunctions.mult);
		Sd = ((FComplexMatrix3D) PSF).copy();
		((FComplexMatrix3D) PSF).assign((FComplexMatrix3D) Pd, FComplexFunctions.plus);
		((FComplexMatrix3D) B).assign(ConjSa, FComplexFunctions.mult);
		Sa = ((FComplexMatrix3D) B).copy();
		((FComplexMatrix3D) Sa).assign((FComplexMatrix3D) PSF, FComplexFunctions.div);
		((FComplexMatrix3D) Sa).ifft3(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) Sa, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred image", stackOut);
		FloatCommon_3D.convertImage(imX, output);
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
		Sa = FloatCommon_3D.circShift_3D((FloatMatrix3D) PSF, psfCenter);
		Sd = FloatCommon_3D.circShift_3D((FloatMatrix3D) Pd, psfCenter);
		Sa = ((FloatMatrix3D) Sa).getFft3();
		Sd = ((FloatMatrix3D) Sd).getFft3();
		B = ((FloatMatrix3D) B).getFft3();
		ConjSa = ((FComplexMatrix3D) Sa).copy();
		ConjSa.assign(FComplexFunctions.conj);
		ConjSd = ((FComplexMatrix3D) Sd).copy();
		ConjSd.assign(FComplexFunctions.conj);
		ConjSd.assign((FComplexMatrix3D) Sd, FComplexFunctions.mult);
		Pd = ConjSd.copy();
		((FComplexMatrix3D) Pd).assign(FComplexFunctions.mult(alpha * alpha));
		PSF = ConjSa.copy();
		((FComplexMatrix3D) PSF).assign((FComplexMatrix3D) Sa, FComplexFunctions.mult);
		Sd = ((FComplexMatrix3D) PSF).copy();
		((FComplexMatrix3D) PSF).assign((FComplexMatrix3D) Pd, FComplexFunctions.plus);
		((FComplexMatrix3D) B).assign(ConjSa, FComplexFunctions.mult);
		Sa = ((FComplexMatrix3D) B).copy();
		((FComplexMatrix3D) Sa).assign((FComplexMatrix3D) PSF, FComplexFunctions.div);
		((FComplexMatrix3D) Sa).ifft3(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) Sa, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred image", stackOut);
		FloatCommon_3D.convertImage(imX, output);
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
		((FComplexMatrix3D) PSF).assign(FComplexFunctions.mult(alpha * alpha));
		Pd = ((FComplexMatrix3D) Sd).copy();
		((FComplexMatrix3D) Pd).assign((FComplexMatrix3D) PSF, FComplexFunctions.plus);
		Sa = ((FComplexMatrix3D) B).copy();
		((FComplexMatrix3D) Sa).assign((FComplexMatrix3D) Pd, FComplexFunctions.div);
		((FComplexMatrix3D) Sa).ifft3(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) Sa, cmY, threshold);
			}
		}
		imX.setStack(imX.getTitle(), stackOut);
		FloatCommon_3D.convertImage(imX, output);
	}

	private static float gcvGTikFFT_3D(FComplexMatrix3D Sa, FComplexMatrix3D Sd, FComplexMatrix3D Bhat) {
		AbstractMatrix3D sa = Sa.copy();
		AbstractMatrix3D sd = Sd.copy();
		AbstractMatrix3D bhat = Bhat.copy();
		sa = ((FComplexMatrix3D) sa).assign(FComplexFunctions.abs).getRealPart();
		sd = ((FComplexMatrix3D) sd).assign(FComplexFunctions.abs).getRealPart();
		bhat = ((FComplexMatrix3D) bhat).assign(FComplexFunctions.abs).getRealPart();

		float[] tmp = ((FloatMatrix3D) sa).getMinLocation();
		float smin = tmp[0];
		tmp = ((FloatMatrix3D) sa).getMaxLocation();
		float smax = tmp[0];
		((FloatMatrix3D) sa).assign(FloatFunctions.square);
		((FloatMatrix3D) sd).assign(FloatFunctions.square);
		GTikFmin_3D fmin = new GTikFmin_3D((FloatMatrix3D) sa, (FloatMatrix3D) sd, (FloatMatrix3D) bhat);
		return (float) FloatFmin.fmin(smin, smax, fmin, FloatCommon_2D.FMIN_TOL);
	}

	private static class GTikFmin_3D implements FloatFmin_methods {
		FloatMatrix3D sasquare;

		FloatMatrix3D sdsquare;

		FloatMatrix3D bhat;

		public GTikFmin_3D(FloatMatrix3D sasquare, FloatMatrix3D sdsquare, FloatMatrix3D bhat) {
			this.sasquare = sasquare;
			this.sdsquare = sdsquare;
			this.bhat = bhat;
		}

		public float f_to_minimize(float alpha) {
			FloatMatrix3D sdloc = sdsquare.copy();
			FloatMatrix3D denom = sdloc.copy();

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
