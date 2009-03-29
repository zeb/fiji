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
 * Tikhonov image deblurring 3D using the FFT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatTikFFT_3D {
	private AbstractMatrix3D B;

	private AbstractMatrix3D PSF;

	private AbstractMatrix3D S;

	private FComplexMatrix3D ConjS;

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
	 * Constructs new FloatTikFFT_3D.
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
	public FloatTikFFT_3D(ImagePlus imB, ImagePlus imPSF, ResizingType resizing, OutputType output, boolean showPadded) {
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
		IJ.showStatus("Tikhonov: deblurring");
		S = FloatCommon_3D.circShift_3D((FloatMatrix3D) PSF, psfCenter);
		S = ((FloatMatrix3D) S).getFft3();
		B = ((FloatMatrix3D) B).getFft3();
		IJ.showStatus("Tikhonov: computing regularization parameter");
		float alpha = gcvTikFFT_3D((FComplexMatrix3D) S, (FComplexMatrix3D) B);
		IJ.showStatus("Tikhonov: deblurring");
		ConjS = ((FComplexMatrix3D) S).copy();
		ConjS.assign(FComplexFunctions.conj);
		PSF = ConjS.copy();
		((FComplexMatrix3D) PSF).assign((FComplexMatrix3D) S, FComplexFunctions.mult);
		S = ((FComplexMatrix3D) PSF).copy();
		((FComplexMatrix3D) PSF).assign(FComplexFunctions.plus(new float[] { alpha * alpha, 0 }));
		((FComplexMatrix3D) B).assign(ConjS, FComplexFunctions.mult);
		ConjS = ((FComplexMatrix3D) B).copy();
		ConjS.assign((FComplexMatrix3D) PSF, FComplexFunctions.div);
		ConjS.ifft3(true);
		IJ.showStatus("Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, ConjS, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, ConjS, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, ConjS, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, ConjS, cmY, threshold);
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
		S = FloatCommon_3D.circShift_3D((FloatMatrix3D) PSF, psfCenter);
		S = ((FloatMatrix3D) S).getFft3();
		B = ((FloatMatrix3D) B).getFft3();
		ConjS = ((FComplexMatrix3D) S).copy();
		ConjS.assign(FComplexFunctions.conj);
		PSF = ConjS.copy();
		((FComplexMatrix3D) PSF).assign((FComplexMatrix3D) S, FComplexFunctions.mult);
		S = ((FComplexMatrix3D) PSF).copy();
		((FComplexMatrix3D) PSF).assign(FComplexFunctions.plus(new float[] { alpha * alpha, 0 }));
		((FComplexMatrix3D) B).assign(ConjS, FComplexFunctions.mult);
		ConjS = ((FComplexMatrix3D) B).copy();
		ConjS.assign((FComplexMatrix3D) PSF, FComplexFunctions.div);
		ConjS.ifft3(true);
		IJ.showStatus("Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, ConjS, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, ConjS, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, ConjS, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, ConjS, cmY, threshold);
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
		PSF = ((FComplexMatrix3D) S).copy();
		((FComplexMatrix3D) PSF).assign(FComplexFunctions.plus(new float[] { alpha * alpha, 0 }));
		ConjS = ((FComplexMatrix3D) B).copy();
		((FComplexMatrix3D) ConjS).assign((FComplexMatrix3D) PSF, FComplexFunctions.div);
		((FComplexMatrix3D) ConjS).ifft3(true);
		IJ.showStatus("Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, ConjS, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, ConjS, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, ConjS, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, ConjS, cmY, threshold);
			}
		}
		imX.setStack(imX.getTitle(), stackOut);
		FloatCommon_3D.convertImage(imX, output);
	}

	private static float gcvTikFFT_3D(FComplexMatrix3D S, FComplexMatrix3D Bhat) {
		AbstractMatrix3D s = S.copy();
		AbstractMatrix3D bhat = Bhat.copy();
		s = ((FComplexMatrix3D) s).assign(FComplexFunctions.abs).getRealPart();
		bhat = ((FComplexMatrix3D) bhat).assign(FComplexFunctions.abs).getRealPart();
		float[] tmp = ((FloatMatrix3D) s).getMinLocation();
		float smin = tmp[0];
		tmp = ((FloatMatrix3D) s).getMaxLocation();
		float smax = tmp[0];
		((FloatMatrix3D) s).assign(FloatFunctions.square);
		TikFmin_3D fmin = new TikFmin_3D((FloatMatrix3D) s, (FloatMatrix3D) bhat);
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
