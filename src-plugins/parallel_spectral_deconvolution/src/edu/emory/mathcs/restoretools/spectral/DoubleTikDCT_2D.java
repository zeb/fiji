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
import optimization.DoubleFmin;
import optimization.DoubleFmin_methods;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Tikhonov image deblurring 2D using the DCT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleTikDCT_2D {
	private DoubleMatrix2D B;

	private DoubleMatrix2D PSF;

	private DoubleMatrix2D E1;

	private DoubleMatrix2D S;

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
	 * Constructs new DoubleTikDCT_2D.
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
	public DoubleTikDCT_2D(ImagePlus imB, ImagePlus imPSF, ResizingType resizing, OutputType output, boolean showPadded) {
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
		IJ.showStatus("Tikhonov: initializing");
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
		B = new DenseDoubleMatrix2D(bRows, bCols);
		DoubleCommon_2D.assignPixelsToMatrix_2D(B, ipB);
		if (isPadded) {
			B = DoubleCommon_2D.padReflexive_2D(B, bColsPad, bRowsPad);
			bColsOff = (bColsPad - bCols + 1) / 2;
			bRowsOff = (bRowsPad - bRows + 1) / 2;
		}
		PSF = new DenseDoubleMatrix2D(kRows, kCols);
		DoubleCommon_2D.assignPixelsToMatrix_2D(PSF, ipPSF);
		double[] maxAndLoc = PSF.getMaxLocation();
		psfCenter = new int[] { (int) maxAndLoc[1], (int) maxAndLoc[2] };
		PSF.normalize();
		if ((kCols != bColsPad) || (kRows != bRowsPad)) {
			PSF = DoubleCommon_2D.padZero_2D(PSF, bColsPad, bRowsPad);
		}
		psfCenter[0] += (bRowsPad - kRows + 1) / 2;
		psfCenter[1] += (bColsPad - kCols + 1) / 2;
		if (showPadded && isPadded) {
			FloatProcessor ipTemp = new FloatProcessor(bColsPad, bRowsPad);
			DoubleCommon_2D.assignPixelsToProcessor(ipTemp, B, cmY);
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
	public ImagePlus deblur(double threshold) {
		IJ.showStatus("Tikhonov: deblurring");
		E1 = new DenseDoubleMatrix2D(bRowsPad, bColsPad);
		E1.setQuick(0, 0, 1);
		B.dct2(true);
		S = DoubleCommon_2D.dctShift_2D(PSF, psfCenter);
		S.dct2(true);
		E1.dct2(true);
		S.assign(E1, DoubleFunctions.div);
		IJ.showStatus("Tikhonov: computing regularization parameter");
		double alpha = gcvTikDCT_2D(S, B);
		IJ.showStatus("Tikhonov: deblurring");
		PSF = S.copy();
		PSF.assign(DoubleFunctions.square);
		E1 = PSF.copy();
		PSF.assign(DoubleFunctions.plus(alpha * alpha));
		B.assign(S, DoubleFunctions.mult);
		S = B.copy();
		S.assign(PSF, DoubleFunctions.div);
		S.idct2(true);
		IJ.showStatus("Tikhonov: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, S, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, S, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, S, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, S, cmY, threshold);
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
	public ImagePlus deblur(double alpha, double threshold) {
		IJ.showStatus("Tikhonov: deblurring");
		E1 = new DenseDoubleMatrix2D(bRowsPad, bColsPad);
		E1.setQuick(0, 0, 1);
		B.dct2(true);
		S = DoubleCommon_2D.dctShift_2D(PSF, psfCenter);
		S.dct2(true);
		E1.dct2(true);
		S.assign(E1, DoubleFunctions.div);
		PSF = S.copy();
		PSF.assign(DoubleFunctions.square);
		E1 = PSF.copy();
		PSF.assign(DoubleFunctions.plus(alpha * alpha));
		B.assign(S, DoubleFunctions.mult);
		S = B.copy();
		S.assign(PSF, DoubleFunctions.div);
		S.idct2(true);
		IJ.showStatus("Tikhonov: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, S, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, S, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, S, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, S, cmY, threshold);
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
	public void update(double alpha, double threshold, ImagePlus imX) {
		IJ.showStatus("Tikhonov: updating");
		PSF = E1.copy();
		PSF.assign(DoubleFunctions.plus(alpha * alpha));
		S = B.copy();
		S.assign(PSF, DoubleFunctions.div);
		S.idct2(true);
		IJ.showStatus("Tikhonov: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, S, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, S, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, S, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, S, cmY, threshold);
			}
		}
		imX.setProcessor(imX.getTitle(), ip);
		DoubleCommon_2D.convertImage(imX, output);

	}

	private static double gcvTikDCT_2D(DoubleMatrix2D S, DoubleMatrix2D Bhat) {
		DoubleMatrix2D s = S.copy();
		DoubleMatrix2D bhat = Bhat.copy();
		s.assign(DoubleFunctions.abs);
		bhat.assign(DoubleFunctions.abs);
		double[] tmp = s.getMinLocation();
		double smin = tmp[0];
		tmp = s.getMaxLocation();
		double smax = tmp[0];
		s.assign(DoubleFunctions.square);
		TikFmin_2D fmin = new TikFmin_2D(s, bhat);
		return (double) DoubleFmin.fmin(smin, smax, fmin, DoubleCommon_2D.FMIN_TOL);
	}

	private static class TikFmin_2D implements DoubleFmin_methods {
		DoubleMatrix2D ssquare;

		DoubleMatrix2D bhat;

		public TikFmin_2D(DoubleMatrix2D ssquare, DoubleMatrix2D bhat) {
			this.ssquare = ssquare;
			this.bhat = bhat;
		}

		public double f_to_minimize(double alpha) {
			DoubleMatrix2D sloc = ssquare.copy();
			DoubleMatrix2D bhatloc = bhat.copy();

			sloc.assign(DoubleFunctions.plus(alpha * alpha));
			sloc.assign(DoubleFunctions.inv);
			bhatloc.assign(sloc, DoubleFunctions.mult);
			bhatloc.assign(DoubleFunctions.square);
			double ss = sloc.zSum();
			return bhatloc.zSum() / (ss * ss);
		}

	}

}
