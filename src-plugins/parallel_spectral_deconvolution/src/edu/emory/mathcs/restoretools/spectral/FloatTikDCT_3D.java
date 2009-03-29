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
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Tikhonov image deblurring 3D using the DCT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatTikDCT_3D {
	private FloatMatrix3D B;

	private FloatMatrix3D PSF;

	private FloatMatrix3D E1;

	private FloatMatrix3D S;

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
	 * Constructs new FloatTikDCT_3D.
	 * 
	 * @param imB
	 *            blurred image
	 * @param imPSF
	 *            point spread function
	 * @param resizing
	 *            type of resizing
	 * @param output
	 *            type of an output
	 * @param showPadded
	 *            if true then padded image is displayed
	 */
	public FloatTikDCT_3D(ImagePlus imB, ImagePlus imPSF, ResizingType resizing, OutputType output, boolean showPadded) {
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
		IJ.showStatus("Tikhonov: initializing");
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
		FloatCommon_3D.assignPixelsToMatrix_3D(isB, B);
		if (isPadded) {
			B = FloatCommon_3D.padReflexive_3D(B, bSlicesPad, bRowsPad, bColsPad);
			bSlicesOff = (bSlicesPad - bSlices + 1) / 2;
			bRowsOff = (bRowsPad - bRows + 1) / 2;
			bColsOff = (bColsPad - bCols + 1) / 2;
		}
		PSF = new DenseFloatMatrix3D(kSlices, kRows, kCols);
		FloatCommon_3D.assignPixelsToMatrix_3D(isPSF, PSF);
		float[] maxAndLoc = PSF.getMaxLocation();
		psfCenter = new int[] { (int) maxAndLoc[1], (int) maxAndLoc[2], (int) maxAndLoc[3] };
		PSF.normalize();
		if (kSlices != bSlicesPad || kRows != bRowsPad || kCols != bColsPad) {
			PSF = FloatCommon_3D.padZero_3D(PSF, bSlicesPad, bRowsPad, bColsPad);
		}
		psfCenter[0] += (bSlicesPad - kSlices + 1) / 2;
		psfCenter[1] += (bRowsPad - kRows + 1) / 2;
		psfCenter[2] += (bColsPad - kCols + 1) / 2;
		if (showPadded && isPadded) {
			ImageStack stackTemp = new ImageStack(bColsPad, bRowsPad);
			FloatCommon_3D.assignPixelsToStack(stackTemp, B, cmY);
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
		IJ.showStatus("Tikhonov: deblurring");
		E1 = new DenseFloatMatrix3D(bSlicesPad, bRowsPad, bColsPad);
		E1.setQuick(0, 0, 0, 1);
		E1.dct3(true);
		B.dct3(true);
		S = FloatCommon_3D.dctShift_3D(PSF, psfCenter);
		S.dct3(true);
		S.assign(E1, FloatFunctions.div);
		IJ.showStatus("Tikhonov: computing regularization parameter");
		float alpha = gcvTikDCT_3D(S, B);
		IJ.showStatus("Tikhonov: deblurring");
		PSF = S.copy();
		PSF.assign(FloatFunctions.square);
		E1 = PSF.copy();
		PSF.assign(FloatFunctions.plus(alpha * alpha));
		B.assign(S, FloatFunctions.mult);
		S = B.copy();
		S.assign(PSF, FloatFunctions.div);
		S.idct3(true);
		IJ.showStatus("Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, S, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, S, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, S, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, S, cmY, threshold);
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
		IJ.showStatus("Tikhonov: deblurring");
		E1 = new DenseFloatMatrix3D(bSlicesPad, bRowsPad, bColsPad);
		E1.setQuick(0, 0, 0, 1);
		E1.dct3(true);
		B.dct3(true);
		S = FloatCommon_3D.dctShift_3D(PSF, psfCenter);
		S.dct3(true);
		S.assign(E1, FloatFunctions.div);
		PSF = S.copy();
		PSF.assign(FloatFunctions.square);
		E1 = PSF.copy();
		PSF.assign(FloatFunctions.plus(alpha * alpha));
		B.assign(S, FloatFunctions.mult);
		S = B.copy();
		S.assign(PSF, FloatFunctions.div);
		S.idct3(true);
		IJ.showStatus("Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, S, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, S, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, S, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, S, cmY, threshold);
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
		IJ.showStatus("Tikhonov: updating");
		PSF = E1.copy();
		PSF.assign(FloatFunctions.plus(alpha * alpha));
		S = B.copy();
		S.assign(PSF, FloatFunctions.div);
		S.idct3(true);
		IJ.showStatus("Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, S, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, S, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, S, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, S, cmY, threshold);
			}
		}
		imX.setStack(imX.getTitle(), stackOut);
		FloatCommon_3D.convertImage(imX, output);
	}

	private static float gcvTikDCT_3D(FloatMatrix3D S, FloatMatrix3D Bhat) {
		FloatMatrix3D s = S.copy();
		FloatMatrix3D bhat = Bhat.copy();
		s = s.assign(FloatFunctions.abs);
		bhat = bhat.assign(FloatFunctions.abs);
		float[] tmp = s.getMinLocation();
		float smin = tmp[0];
		tmp = s.getMaxLocation();
		float smax = tmp[0];
		s = s.assign(FloatFunctions.square);
		TikFmin_3D fmin = new TikFmin_3D(s, bhat);
		return (float) FloatFmin.fmin(smin, smax, fmin, FloatCommon_2D.FMIN_TOL);
	}

	private static class TikFmin_3D implements FloatFmin_methods {
		FloatMatrix3D ssquare;

		FloatMatrix3D bhat;

		public TikFmin_3D(FloatMatrix3D ssquare, FloatMatrix3D bhat) {
			this.ssquare = ssquare;
			this.bhat = bhat;
		}

		public float f_to_minimize(float alpha) {
			FloatMatrix3D sloc = ssquare.copy();
			FloatMatrix3D bhatloc = bhat.copy();
			sloc.assign(FloatFunctions.plus(alpha * alpha));
			sloc.assign(FloatFunctions.inv);
			bhatloc.assign(sloc, FloatFunctions.mult);
			bhatloc.assign(FloatFunctions.square);
			float ss = sloc.zSum();
			return bhatloc.zSum() / (ss * ss);
		}

	}
}
