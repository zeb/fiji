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
import cern.colt.matrix.AbstractMatrix2D;
import cern.colt.matrix.tdcomplex.DComplexMatrix2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdcomplex.DComplexFunctions;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Generalized Tikhonov image deblurring 2D using the FFT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleGTikFFT_2D {

	private AbstractMatrix2D B;

	private AbstractMatrix2D PSF;

	private AbstractMatrix2D Pd;

	private AbstractMatrix2D Sa;

	private AbstractMatrix2D Sd;

	private DComplexMatrix2D ConjSa;

	private DComplexMatrix2D ConjSd;

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
	 * Constructs new DoubleGTikFFT_2D.
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
	public DoubleGTikFFT_2D(ImagePlus imB, ImagePlus imPSF, DoubleMatrix2D stencil, ResizingType resizing, OutputType output, boolean showPadded) {
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
		B = new DenseDoubleMatrix2D(bRows, bCols);
		DoubleCommon_2D.assignPixelsToMatrix_2D((DoubleMatrix2D) B, ipB);
		if (isPadded) {
			B = DoubleCommon_2D.padPeriodic_2D((DoubleMatrix2D) B, bColsPad, bRowsPad);
			bColsOff = (bColsPad - bCols + 1) / 2;
			bRowsOff = (bRowsPad - bRows + 1) / 2;
		}
		PSF = new DenseDoubleMatrix2D(kRows, kCols);
		DoubleCommon_2D.assignPixelsToMatrix_2D((DoubleMatrix2D) PSF, ipPSF);
		double[] maxAndLoc = ((DoubleMatrix2D) PSF).getMaxLocation();
		psfCenter = new int[] { (int) maxAndLoc[1], (int) maxAndLoc[2] };
		((DoubleMatrix2D) PSF).normalize();
		if ((kCols != bColsPad) || (kRows != bRowsPad)) {
			PSF = DoubleCommon_2D.padZero_2D((DoubleMatrix2D) PSF, bColsPad, bRowsPad);
		}
		psfCenter[0] += (bRowsPad - kRows + 1) / 2;
		psfCenter[1] += (bColsPad - kCols + 1) / 2;
		Pd = new DenseDoubleMatrix2D(bRowsPad, bColsPad);
		((DoubleMatrix2D) Pd).viewPart(0, 0, 3, 3).assign(stencil);
		if (showPadded && isPadded) {
			FloatProcessor ipTemp = new FloatProcessor(bColsPad, bRowsPad);
			DoubleCommon_2D.assignPixelsToProcessor(ipTemp, (DoubleMatrix2D) B, cmY);
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
		IJ.showStatus("Generalized Tikhonov: deblurring");
		Sa = DoubleCommon_2D.circShift_2D((DoubleMatrix2D) PSF, psfCenter);
		Sa = ((DoubleMatrix2D) Sa).getFft2();
		B = ((DoubleMatrix2D) B).getFft2();
		Sd = DoubleCommon_2D.circShift_2D((DoubleMatrix2D) Pd, new int[] { 1, 1 });
		Sd = ((DoubleMatrix2D) Sd).getFft2();
		IJ.showStatus("Generalized Tikhonov: computing regularization parameter");
		double alpha = gcvGTikFFT_2D((DComplexMatrix2D) Sa, (DComplexMatrix2D) Sd, (DComplexMatrix2D) B);
		IJ.showStatus("Generalized Tikhonov: deblurring");
		ConjSa = ((DComplexMatrix2D) Sa).copy();
		ConjSa.assign(DComplexFunctions.conj);
		ConjSd = ((DComplexMatrix2D) Sd).copy();
		ConjSd.assign(DComplexFunctions.conj);
		ConjSd.assign((DComplexMatrix2D) Sd, DComplexFunctions.mult);
		Pd = ConjSd.copy();
		((DComplexMatrix2D) Pd).assign(DComplexFunctions.mult(alpha * alpha));
		PSF = ConjSa.copy();
		((DComplexMatrix2D) PSF).assign((DComplexMatrix2D) Sa, DComplexFunctions.mult);
		Sd = ((DComplexMatrix2D) PSF).copy();
		((DComplexMatrix2D) PSF).assign((DComplexMatrix2D) Pd, DComplexFunctions.plus);
		((DComplexMatrix2D) B).assign(ConjSa, DComplexFunctions.mult);
		Sa = ((DComplexMatrix2D) B).copy();
		((DComplexMatrix2D) Sa).assign((DComplexMatrix2D) PSF, DComplexFunctions.div);
		((DComplexMatrix2D) Sa).ifft2(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) Sa, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred", ip);
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
		IJ.showStatus("Generalized Tikhonov: deblurring");
		Sa = DoubleCommon_2D.circShift_2D((DoubleMatrix2D) PSF, psfCenter);
		Sa = ((DoubleMatrix2D) Sa).getFft2();
		B = ((DoubleMatrix2D) B).getFft2();
		Sd = DoubleCommon_2D.circShift_2D((DoubleMatrix2D) Pd, new int[] { 1, 1 });
		Sd = ((DoubleMatrix2D) Sd).getFft2();
		ConjSa = ((DComplexMatrix2D) Sa).copy();
		ConjSa.assign(DComplexFunctions.conj);
		ConjSd = ((DComplexMatrix2D) Sd).copy();
		ConjSd.assign(DComplexFunctions.conj);
		ConjSd.assign((DComplexMatrix2D) Sd, DComplexFunctions.mult);
		Pd = ConjSd.copy();
		((DComplexMatrix2D) Pd).assign(DComplexFunctions.mult(alpha * alpha));
		PSF = ConjSa.copy();
		((DComplexMatrix2D) PSF).assign((DComplexMatrix2D) Sa, DComplexFunctions.mult);
		Sd = ((DComplexMatrix2D) PSF).copy();
		((DComplexMatrix2D) PSF).assign((DComplexMatrix2D) Pd, DComplexFunctions.plus);
		((DComplexMatrix2D) B).assign(ConjSa, DComplexFunctions.mult);
		Sa = ((DComplexMatrix2D) B).copy();
		((DComplexMatrix2D) Sa).assign((DComplexMatrix2D) PSF, DComplexFunctions.div);
		((DComplexMatrix2D) Sa).ifft2(true);
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) Sa, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred", ip);
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
		IJ.showStatus("Generalized Tikhonov: updating");
		PSF = ConjSd.copy();
		((DComplexMatrix2D) PSF).assign(DComplexFunctions.mult(alpha * alpha));
		Pd = ((DComplexMatrix2D) Sd).copy();
		((DComplexMatrix2D) Pd).assign((DComplexMatrix2D) PSF, DComplexFunctions.plus);
		Sa = ((DComplexMatrix2D) B).copy();
		((DComplexMatrix2D) Sa).assign((DComplexMatrix2D) Pd, DComplexFunctions.div);
		((DComplexMatrix2D) Sa).ifft2(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		FloatProcessor ip = new FloatProcessor(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) Sa, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, (DComplexMatrix2D) Sa, bRows, bCols, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, (DComplexMatrix2D) Sa, cmY, threshold);
			}
		}
		imX.setProcessor(imX.getTitle(), ip);
		DoubleCommon_2D.convertImage(imX, output);
	}

	private double gcvGTikFFT_2D(DComplexMatrix2D Sa, DComplexMatrix2D Sd, DComplexMatrix2D Bhat) {
		AbstractMatrix2D sa = Sa.copy();
		sa = ((DComplexMatrix2D) sa).assign(DComplexFunctions.abs).getRealPart();
		AbstractMatrix2D sd = Sd.copy();
		sd = ((DComplexMatrix2D) sd).assign(DComplexFunctions.abs).getRealPart();
		AbstractMatrix2D bhat = Bhat.copy();
		bhat = ((DComplexMatrix2D) bhat).assign(DComplexFunctions.abs).getRealPart();
		double[] tmp = ((DoubleMatrix2D) sa).getMinLocation();
		double smin = tmp[0];
		tmp = ((DoubleMatrix2D) sa).getMaxLocation();
		double smax = tmp[0];
		((DoubleMatrix2D) sa).assign(DoubleFunctions.square);
		((DoubleMatrix2D) sd).assign(DoubleFunctions.square);
		GTikFmin_2D fmin = new GTikFmin_2D((DoubleMatrix2D) sa, (DoubleMatrix2D) sd, (DoubleMatrix2D) bhat);
		return (double) DoubleFmin.fmin(smin, smax, fmin, DoubleCommon_2D.FMIN_TOL);
	}

	private class GTikFmin_2D implements DoubleFmin_methods {
		DoubleMatrix2D sasquare;

		DoubleMatrix2D sdsquare;

		DoubleMatrix2D bhat;

		public GTikFmin_2D(DoubleMatrix2D sasquare, DoubleMatrix2D sdsquare, DoubleMatrix2D bhat) {
			this.sasquare = sasquare;
			this.sdsquare = sdsquare;
			this.bhat = bhat;
		}

		public double f_to_minimize(double alpha) {
			DoubleMatrix2D sdloc = sdsquare.copy();
			DoubleMatrix2D denom = sdloc.copy();

			denom.assign(DoubleFunctions.mult(alpha * alpha));
			denom.assign(sasquare, DoubleFunctions.plus);
			sdloc.assign(denom, DoubleFunctions.div);
			denom = bhat.copy();
			denom.assign(sdloc, DoubleFunctions.mult);
			denom.assign(DoubleFunctions.square);
			double sphi_d = sdloc.zSum();
			return denom.zSum() / (sphi_d * sphi_d);
		}

	}

}
