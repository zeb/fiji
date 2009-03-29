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
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix3D;
import cern.colt.matrix.tdouble.algo.DoubleSorting;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix3D;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Truncated SVD image deblurring 3D using the DCT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleTsvdDCT_3D {

	private DoubleMatrix3D B;

	private DoubleMatrix3D PSF;

	private DoubleMatrix3D E1;

	private DoubleMatrix3D S;

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
	 * Constructs new DoubleTsvdDCT_3D.
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
	public DoubleTsvdDCT_3D(ImagePlus imB, ImagePlus imPSF, ResizingType resizing, OutputType output, boolean showPadded) {
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
		B = new DenseDoubleMatrix3D(bSlices, bRows, bCols);
		DoubleCommon_3D.assignPixelsToMatrix_3D(isB, B);
		if (isPadded) {
			B = DoubleCommon_3D.padReflexive_3D(B, bSlicesPad, bRowsPad, bColsPad);
			bSlicesOff = (bSlicesPad - bSlices + 1) / 2;
			bRowsOff = (bRowsPad - bRows + 1) / 2;
			bColsOff = (bColsPad - bCols + 1) / 2;
		}
		PSF = new DenseDoubleMatrix3D(kSlices, kRows, kCols);
		DoubleCommon_3D.assignPixelsToMatrix_3D(isPSF, PSF);
		double[] maxAndLoc = PSF.getMaxLocation();
		psfCenter = new int[] { (int) maxAndLoc[1], (int) maxAndLoc[2], (int) maxAndLoc[3] };
		PSF.normalize();
		if (kSlices != bSlicesPad || kRows != bRowsPad || kCols != bColsPad) {
			PSF = DoubleCommon_3D.padZero_3D(PSF, bSlicesPad, bRowsPad, bColsPad);
		}
		psfCenter[0] += (bSlicesPad - kSlices + 1) / 2;
		psfCenter[1] += (bRowsPad - kRows + 1) / 2;
		psfCenter[2] += (bColsPad - kCols + 1) / 2;
		if (showPadded && isPadded) {
			ImageStack stackTemp = new ImageStack(bColsPad, bRowsPad);
			DoubleCommon_3D.assignPixelsToStack(stackTemp, B, cmY);
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
	public ImagePlus deblur(double threshold) {
		IJ.showStatus("TSVD: deblurring");
		E1 = new DenseDoubleMatrix3D(bSlicesPad, bRowsPad, bColsPad);
		E1.setQuick(0, 0, 0, 1);
		E1.dct3(true);
		B.dct3(true);
		S = DoubleCommon_3D.dctShift_3D(PSF, psfCenter);
		S.dct3(true);
		S.assign(E1, DoubleFunctions.div);
		IJ.showStatus("TSVD: computing regularization parameter");
		double alpha = gcvTsvdDCT_3D(S, B);
		IJ.showStatus("TSVD: deblurring");
		E1 = DoubleCommon_3D.createFilter_3D(S, alpha);
		PSF = B.copy();
		PSF.assign(E1, DoubleFunctions.mult);
		PSF.idct3(true);
		IJ.showStatus("TSVD: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, PSF, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, PSF, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred image", stackOut);
		DoubleCommon_3D.convertImage(imX, output);
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
	public ImagePlus deblur(double alpha, double threshold) {
		IJ.showStatus("TSVD: deblurring");
		E1 = new DenseDoubleMatrix3D(bSlicesPad, bRowsPad, bColsPad);
		E1.setQuick(0, 0, 0, 1);
		E1.dct3(true);
		B.dct3(true);
		S = DoubleCommon_3D.dctShift_3D(PSF, psfCenter);
		S.dct3(true);
		S.assign(E1, DoubleFunctions.div);
		E1 = DoubleCommon_3D.createFilter_3D(S, alpha);
		PSF = B.copy();
		PSF.assign(E1, DoubleFunctions.mult);
		PSF.idct3(true);
		IJ.showStatus("TSVD: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, PSF, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, PSF, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred image", stackOut);
		DoubleCommon_3D.convertImage(imX, output);
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
	public void update(double alpha, double threshold, ImagePlus imX) {
		IJ.showStatus("TSVD: updating");
		E1 = DoubleCommon_3D.createFilter_3D(S, alpha);
		PSF = B.copy();
		PSF.assign(E1, DoubleFunctions.mult);
		PSF.idct3(true);
		IJ.showStatus("TSVD: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, PSF, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, PSF, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, PSF, cmY, threshold);
			}
		}
		imX.setStack(imX.getTitle(), stackOut);
		DoubleCommon_3D.convertImage(imX, output);
	}

	private static double gcvTsvdDCT_3D(DoubleMatrix3D S, DoubleMatrix3D Bhat) {
		int length = S.slices() * S.rows() * S.columns();
		DoubleMatrix1D s = new DenseDoubleMatrix1D(length);
		DoubleMatrix1D bhat = new DenseDoubleMatrix1D(length);
		System.arraycopy(((DenseDoubleMatrix3D) S).elements(), 0, ((DenseDoubleMatrix1D) s).elements(), 0, length);
		System.arraycopy(((DenseDoubleMatrix3D) Bhat).elements(), 0, ((DenseDoubleMatrix1D) bhat).elements(), 0, length);
		s.assign(DoubleFunctions.abs);
		bhat.assign(DoubleFunctions.abs);
		final double[] svalues = (double[]) ((DenseDoubleMatrix1D) s).elements();

		IntComparator compDec = new IntComparator() {
			public int compare(int a, int b) {
				if (svalues[a] != svalues[a] || svalues[b] != svalues[b])
					return compareNaN(svalues[a], svalues[b]); // swap NaNs to
				// the end
				return svalues[a] < svalues[b] ? 1 : (svalues[a] == svalues[b] ? 0 : -1);
			}
		};
		int[] indices = DoubleSorting.quickSort.sortIndex(s, compDec);
		s = s.viewSelection(indices);
		bhat = bhat.viewSelection(indices);
		int n = s.size();
		double[] rho = new double[n - 1];
		rho[n - 2] = bhat.getQuick(n - 1) * bhat.getQuick(n - 1);
		DoubleMatrix1D G = new DenseDoubleMatrix1D(n - 1);
		double[] elemsG = (double[]) G.elements();
		elemsG[n - 2] = rho[n - 2];
		double bhatel, temp1;
		for (int k = n - 2; k > 0; k--) {
			bhatel = bhat.getQuick(k);
			rho[k - 1] = rho[k] + bhatel * bhatel;
			temp1 = n - k;
			temp1 = temp1 * temp1;
			elemsG[k - 1] = rho[k - 1] / temp1;
		}
		for (int k = 0; k < n - 3; k++) {
			if (s.getQuick(k) == s.getQuick(k + 1)) {
				elemsG[k] = Double.POSITIVE_INFINITY;
			}
		}
		return s.getQuick((int) G.getMinLocation()[1]);
	}

	private static final int compareNaN(double a, double b) {
		if (a != a) {
			if (b != b)
				return 0; // NaN equals NaN
			else
				return 1; // e.g. NaN > 5
		}
		return -1; // e.g. 5 < NaN
	}
}
