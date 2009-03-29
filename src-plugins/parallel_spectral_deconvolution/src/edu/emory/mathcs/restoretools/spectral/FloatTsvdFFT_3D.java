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
import cern.colt.function.tint.IntComparator;
import cern.colt.matrix.AbstractMatrix1D;
import cern.colt.matrix.AbstractMatrix3D;
import cern.colt.matrix.tfcomplex.FComplexMatrix1D;
import cern.colt.matrix.tfcomplex.FComplexMatrix3D;
import cern.colt.matrix.tfcomplex.impl.DenseFComplexMatrix1D;
import cern.colt.matrix.tfcomplex.impl.DenseFComplexMatrix3D;
import cern.colt.matrix.tfloat.FloatMatrix1D;
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.algo.FloatSorting;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix1D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import cern.jet.math.tfcomplex.FComplexFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Truncated SVD image deblurring 3D using the FFT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatTsvdFFT_3D {
	private AbstractMatrix3D B;

	private AbstractMatrix3D PSF;

	private AbstractMatrix3D S;

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
	 * Constructs new FloatTsvdFFT_3D.
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
	public FloatTsvdFFT_3D(ImagePlus imB, ImagePlus imPSF, ResizingType resizing, OutputType output, boolean showPadded) {
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
		IJ.showStatus("TSVD: initializing");
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
		IJ.showStatus("TSVD: deblurring");
		S = FloatCommon_3D.circShift_3D((FloatMatrix3D) PSF, psfCenter);
		S = ((FloatMatrix3D) S).getFft3();
		B = ((FloatMatrix3D) B).getFft3();
		IJ.showStatus("TSVD: computing regularization parameter");
		float alpha = gcvTsvdFFT_3D((FComplexMatrix3D) S, (FComplexMatrix3D) B);
		IJ.showStatus("TSVD: deblurring");
		FComplexMatrix3D Sfilt = FloatCommon_3D.createFilter_3D((FComplexMatrix3D) S, alpha);
		PSF = ((FComplexMatrix3D) B).copy();
		((FComplexMatrix3D) PSF).assign(Sfilt, FComplexFunctions.mult);
		((FComplexMatrix3D) PSF).ifft3(true);
		IJ.showStatus("TSVD: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) PSF, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) PSF, cmY, threshold);
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
		IJ.showStatus("TSVD: deblurring");
		S = FloatCommon_3D.circShift_3D((FloatMatrix3D) PSF, psfCenter);
		S = ((FloatMatrix3D) S).getFft3();
		B = ((FloatMatrix3D) B).getFft3();
		FComplexMatrix3D Sfilt = FloatCommon_3D.createFilter_3D((FComplexMatrix3D) S, alpha);
		PSF = ((FComplexMatrix3D) B).copy();
		((FComplexMatrix3D) PSF).assign(Sfilt, FComplexFunctions.mult);
		((FComplexMatrix3D) PSF).ifft3(true);
		IJ.showStatus("TSVD: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) PSF, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) PSF, cmY, threshold);
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
		IJ.showStatus("TSVD: updating");
		FComplexMatrix3D Sfilt = FloatCommon_3D.createFilter_3D((FComplexMatrix3D) S, alpha);
		PSF = ((FComplexMatrix3D) B).copy();
		((FComplexMatrix3D) PSF).assign(Sfilt, FComplexFunctions.mult);
		((FComplexMatrix3D) PSF).ifft3(true);
		IJ.showStatus("TSVD: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) PSF, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_3D.assignPixelsToStackPadded(stackOut, (FComplexMatrix3D) PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_3D.assignPixelsToStack(stackOut, (FComplexMatrix3D) PSF, cmY, threshold);
			}
		}
		imX.setStack(imX.getTitle(), stackOut);
		FloatCommon_3D.convertImage(imX, output);
	}

	private static float gcvTsvdFFT_3D(FComplexMatrix3D S, FComplexMatrix3D Bhat) {
		int length = S.slices() * S.rows() * S.columns();
		AbstractMatrix1D s = new DenseFComplexMatrix1D(length);
		AbstractMatrix1D bhat = new DenseFComplexMatrix1D(length);
		System.arraycopy(((DenseFComplexMatrix3D) S).elements(), 0, ((DenseFComplexMatrix1D) s).elements(), 0, 2 * length);
		System.arraycopy(((DenseFComplexMatrix3D) Bhat).elements(), 0, ((DenseFComplexMatrix1D) bhat).elements(), 0, 2 * length);
		s = ((FComplexMatrix1D) s).assign(FComplexFunctions.abs).getRealPart();
		bhat = ((FComplexMatrix1D) bhat).assign(FComplexFunctions.abs).getRealPart();
		final float[] svalues = (float[]) ((DenseFloatMatrix1D) s).elements();
		IntComparator compDec = new IntComparator() {
			public int compare(int a, int b) {
				if (svalues[a] != svalues[a] || svalues[b] != svalues[b])
					return compareNaN(svalues[a], svalues[b]); // swap NaNs to
				// the end
				return svalues[a] < svalues[b] ? 1 : (svalues[a] == svalues[b] ? 0 : -1);
			}
		};
		int[] indices = FloatSorting.quickSort.sortIndex((FloatMatrix1D) s, compDec);
		s = ((FloatMatrix1D) s).viewSelection(indices);
		bhat = ((FloatMatrix1D) bhat).viewSelection(indices);
		int n = s.size();
		float[] rho = new float[n - 1];
		rho[n - 2] = ((FloatMatrix1D) bhat).getQuick(n - 1) * ((FloatMatrix1D) bhat).getQuick(n - 1);
		FloatMatrix1D G = new DenseFloatMatrix1D(n - 1);
		float[] elemsG = (float[]) G.elements();
		elemsG[n - 2] = rho[n - 2];
		float bhatel, temp1;
		for (int k = n - 2; k > 0; k--) {
			bhatel = ((FloatMatrix1D) bhat).getQuick(k);
			rho[k - 1] = rho[k] + bhatel * bhatel;
			temp1 = n - k;
			temp1 = temp1 * temp1;
			elemsG[k - 1] = rho[k - 1] / temp1;
		}
		for (int k = 0; k < n - 3; k++) {
			if (((FloatMatrix1D) s).getQuick(k) == ((FloatMatrix1D) s).getQuick(k + 1)) {
				elemsG[k] = Float.POSITIVE_INFINITY;
			}
		}
		return ((FloatMatrix1D) s).getQuick((int) G.getMinLocation()[1]);
	}

	private static final int compareNaN(float a, float b) {
		if (a != a) {
			if (b != b)
				return 0; // NaN equals NaN
			else
				return 1; // e.g. NaN > 5
		}
		return -1; // e.g. 5 < NaN
	}
}
