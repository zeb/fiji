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
import cern.colt.function.tint.IntComparator;
import cern.colt.matrix.tfloat.FloatMatrix1D;
import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.algo.FloatSorting;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix1D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Truncated SVD image deblurring 2D using the DCT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatTsvdDCT_2D {

	private FloatMatrix2D B;

	private FloatMatrix2D PSF;

	private FloatMatrix2D E1;

	private FloatMatrix2D S;

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
	 * Constructs new FloatTsvdDCT_2D.
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
	public FloatTsvdDCT_2D(ImagePlus imB, ImagePlus imPSF, ResizingType resizing, OutputType output, boolean showPadded) {
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
		IJ.showStatus("TSVD: initializing");
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
			B = FloatCommon_2D.padReflexive_2D(B, bColsPad, bRowsPad);
			bColsOff = (bColsPad - bCols + 1) / 2;
			bRowsOff = (bRowsPad - bRows + 1) / 2;
		}
		PSF = FloatCommon_2D.assignPixelsToMatrix_2D(ipPSF);
		float[] maxAndLoc = PSF.getMaxLocation();
		psfCenter = new int[] { (int) maxAndLoc[1], (int) maxAndLoc[2] };
		PSF.normalize();
		if ((kCols != bColsPad) || (kRows != bRowsPad)) {
			PSF = FloatCommon_2D.padZero_2D(PSF, bColsPad, bRowsPad);
		}
		psfCenter[0] += (bRowsPad - kRows + 1) / 2;
		psfCenter[1] += (bColsPad - kCols + 1) / 2;
		if (showPadded && isPadded) {
			FloatProcessor ipTemp = new FloatProcessor(bColsPad, bRowsPad);
			FloatCommon_2D.assignPixelsToProcessor(ipTemp, B, cmY);
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
		IJ.showStatus("TSVD: deblurring");
		E1 = new DenseFloatMatrix2D(bRowsPad, bColsPad);
		E1.setQuick(0, 0, 1);
		B.dct2(true);
		S = FloatCommon_2D.dctShift_2D(PSF, psfCenter);
		S.dct2(true);
		E1.dct2(true);
		S.assign(E1, FloatFunctions.div);
		IJ.showStatus("TSVD: computing regularization parameter");
		float alpha = gcvTsvdDCT_2D(S, B);
		IJ.showStatus("TSVD: deblurring");
		E1 = FloatCommon_2D.createFilter_2D(S, alpha);
		PSF = B.copy();
		PSF.assign(E1, FloatFunctions.mult);
		PSF.idct2(true);
		IJ.showStatus("TSVD: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, PSF, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, PSF, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, PSF, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, PSF, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred image", ip);
		DoubleCommon_2D.convertImage(imX, output);
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
		E1 = new DenseFloatMatrix2D(bRowsPad, bColsPad);
		E1.setQuick(0, 0, 1);
		B.dct2(true);
		S = FloatCommon_2D.dctShift_2D(PSF, psfCenter);
		S.dct2(true);
		E1.dct2(true);
		S.assign(E1, FloatFunctions.div);
		E1 = FloatCommon_2D.createFilter_2D(S, alpha);
		PSF = B.copy();
		PSF.assign(E1, FloatFunctions.mult);
		PSF.idct2(true);
		IJ.showStatus("TSVD: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, PSF, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, PSF, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, PSF, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, PSF, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred image", ip);
		DoubleCommon_2D.convertImage(imX, output);
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
		E1 = FloatCommon_2D.createFilter_2D(S, alpha);
		PSF = B.copy();
		PSF.assign(E1, FloatFunctions.mult);
		PSF.idct2(true);
		IJ.showStatus("TSVD: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, PSF, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, PSF, cmY);
			}
		} else {
			if (isPadded) {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, PSF, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, PSF, cmY, threshold);
			}
		}
		imX.setProcessor(imX.getTitle(), ip);
		DoubleCommon_2D.convertImage(imX, output);
	}

	private float gcvTsvdDCT_2D(FloatMatrix2D S, FloatMatrix2D Bhat) {
		int length = S.rows() * S.columns();
		FloatMatrix1D s = new DenseFloatMatrix1D(length);
		FloatMatrix1D bhat = new DenseFloatMatrix1D(length);
		System.arraycopy(((DenseFloatMatrix2D) S).elements(), 0, ((DenseFloatMatrix1D) s).elements(), 0, length);
		System.arraycopy(((DenseFloatMatrix2D) Bhat).elements(), 0, ((DenseFloatMatrix1D) bhat).elements(), 0, length);
		s.assign(FloatFunctions.abs);
		bhat.assign(FloatFunctions.abs);
		final float[] svalues = (float[]) ((DenseFloatMatrix1D) s).elements();

		IntComparator compDec = new IntComparator() {
			public int compare(int a, int b) {
				if (svalues[a] != svalues[a] || svalues[b] != svalues[b])
					return compareNaN(svalues[a], svalues[b]); // swap NaNs to
				// the end
				return svalues[a] < svalues[b] ? 1 : (svalues[a] == svalues[b] ? 0 : -1);
			}
		};
		int[] indices = FloatSorting.quickSort.sortIndex(s, compDec);
		s = s.viewSelection(indices);
		bhat = bhat.viewSelection(indices);
		int n = s.size();
		float[] rho = new float[n - 1];
		rho[n - 2] = bhat.getQuick(n - 1) * bhat.getQuick(n - 1);
		FloatMatrix1D G = new DenseFloatMatrix1D(n - 1);
		float[] elemsG = (float[]) G.elements();
		elemsG[n - 2] = rho[n - 2];
		float bhatel, temp1;
		for (int k = n - 2; k > 0; k--) {
			bhatel = bhat.getQuick(k);
			rho[k - 1] = rho[k] + bhatel * bhatel;
			temp1 = n - k;
			temp1 = temp1 * temp1;
			elemsG[k - 1] = rho[k - 1] / temp1;
		}
		for (int k = 0; k < n - 3; k++) {
			if (s.getQuick(k) == s.getQuick(k + 1)) {
				elemsG[k] = Float.POSITIVE_INFINITY;
			}
		}
		return s.getQuick((int) G.getMinLocation()[1]);
	}

	private final int compareNaN(float a, float b) {
		if (a != a) {
			if (b != b)
				return 0; // NaN equals NaN
			else
				return 1; // e.g. NaN > 5
		}
		return -1; // e.g. 5 < NaN
	}

}
