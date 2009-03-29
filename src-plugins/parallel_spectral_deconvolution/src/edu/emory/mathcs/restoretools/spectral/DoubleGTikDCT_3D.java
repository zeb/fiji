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
import optimization.DoubleFmin;
import optimization.DoubleFmin_methods;
import cern.colt.matrix.tdouble.DoubleMatrix3D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix3D;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Generalized Tikhonov image deblurring 3D using the DCT algorithm.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleGTikDCT_3D {
	private DoubleMatrix3D B;

	private DoubleMatrix3D PSF;

	private DoubleMatrix3D Pd;

	private DoubleMatrix3D E1;

	private DoubleMatrix3D Sa;

	private DoubleMatrix3D Sd;

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
	 * Constructs new DoubleGTikDCT_3D.
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
	public DoubleGTikDCT_3D(ImagePlus imB, ImagePlus imPSF, DoubleMatrix3D stencil, ResizingType resizing, OutputType output, boolean showPadded) {
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
		Pd = new DenseDoubleMatrix3D(bSlicesPad, bRowsPad, bColsPad);
		Pd.viewPart(0, 0, 0, 3, 3, 3).assign(stencil);
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
		IJ.showStatus("Generalized Tikhonov: deblurring");
		E1 = new DenseDoubleMatrix3D(bSlicesPad, bRowsPad, bColsPad);
		E1.setQuick(0, 0, 0, 1);
		E1.dct3(true);
		Sa = DoubleCommon_3D.dctShift_3D(PSF, psfCenter);
		Sa.dct3(true);
		Sa.assign(E1, DoubleFunctions.div);
		B.dct3(true);
		Sd = DoubleCommon_3D.dctShift_3D(Pd, new int[] { 1, 1, 1 });
		Sd.dct3(true);
		Sd.assign(E1, DoubleFunctions.div);
		IJ.showStatus("Generalized Tikhonov: computing regularization parameter");
		double alpha = gcvGTikDCT_3D(Sa, Sd, B);
		IJ.showStatus("Generalized Tikhonov: deblurring");
		Sd.assign(DoubleFunctions.square);
		PSF = Sa.copy();
		Pd = Sd.copy();
		Pd.assign(DoubleFunctions.mult(alpha * alpha));
		PSF.assign(DoubleFunctions.square);
		E1 = PSF.copy();
		PSF.assign(Pd, DoubleFunctions.plus);
		B.assign(Sa, DoubleFunctions.mult);
		Sa = B.copy();
		Sa.assign(PSF, DoubleFunctions.div);
		Sa.idct3(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, Sa, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, Sa, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred", stackOut);
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
		IJ.showStatus("Generalized Tikhonov: deblurring");
		E1 = new DenseDoubleMatrix3D(bSlicesPad, bRowsPad, bColsPad);
		E1.setQuick(0, 0, 0, 1);
		E1.dct3(true);
		Sa = DoubleCommon_3D.dctShift_3D(PSF, psfCenter);
		Sa.dct3(true);
		Sa.assign(E1, DoubleFunctions.div);
		B.dct3(true);
		Sd = DoubleCommon_3D.dctShift_3D(Pd, new int[] { 1, 1, 1 });
		Sd.dct3(true);
		Sd.assign(E1, DoubleFunctions.div);
		Sd.assign(DoubleFunctions.square);
		PSF = Sa.copy();
		Pd = Sd.copy();
		Pd.assign(DoubleFunctions.mult(alpha * alpha));
		PSF.assign(DoubleFunctions.square);
		E1 = PSF.copy();
		PSF.assign(Pd, DoubleFunctions.plus);
		B.assign(Sa, DoubleFunctions.mult);
		Sa = B.copy();
		Sa.assign(PSF, DoubleFunctions.div);
		Sa.idct3(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, Sa, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, Sa, cmY, threshold);
			}
		}
		ImagePlus imX = new ImagePlus("Deblurred", stackOut);
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
	 * @param imX
	 *            deblurred image
	 */
	public void update(double alpha, double threshold, ImagePlus imX) {
		IJ.showStatus("Generalized Tikhonov: updating");
		Pd = Sd.copy();
		Pd.assign(DoubleFunctions.mult(alpha * alpha));
		PSF = E1.copy();
		PSF.assign(Pd, DoubleFunctions.plus);
		Sa = B.copy();
		Sa.assign(PSF, DoubleFunctions.div);
		Sa.idct3(true);
		IJ.showStatus("Generalized Tikhonov: finalizing");
		ImageStack stackOut = new ImageStack(bCols, bRows);
		if (threshold == -1.0) {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, Sa, cmY);
			}
		} else {
			if (isPadded) {
				DoubleCommon_3D.assignPixelsToStackPadded(stackOut, Sa, bSlices, bRows, bCols, bSlicesOff, bRowsOff, bColsOff, cmY, threshold);
			} else {
				DoubleCommon_3D.assignPixelsToStack(stackOut, Sa, cmY, threshold);
			}
		}
		imX.setStack(imX.getTitle(), stackOut);
		DoubleCommon_3D.convertImage(imX, output);
	}

	private static double gcvGTikDCT_3D(DoubleMatrix3D Sa, DoubleMatrix3D Sd, DoubleMatrix3D Bhat) {
		DoubleMatrix3D sa = Sa.copy();
		DoubleMatrix3D sd = Sd.copy();
		DoubleMatrix3D bhat = Bhat.copy();
		sa.assign(DoubleFunctions.abs);
		bhat.assign(DoubleFunctions.abs);
		double[] tmp = sa.getMinLocation();
		double smin = tmp[0];
		tmp = sa.getMaxLocation();
		double smax = tmp[0];
		sa = sa.assign(DoubleFunctions.square);
		sd = sd.assign(DoubleFunctions.square);
		GTikFmin_3D fmin = new GTikFmin_3D(sa, sd, bhat);
		return (double) DoubleFmin.fmin(smin, smax, fmin, DoubleCommon_2D.FMIN_TOL);
	}

	private static class GTikFmin_3D implements DoubleFmin_methods {
		DoubleMatrix3D sasquare;

		DoubleMatrix3D sdsquare;

		DoubleMatrix3D bhat;

		public GTikFmin_3D(DoubleMatrix3D sasquare, DoubleMatrix3D sdsquare, DoubleMatrix3D bhat) {
			this.sasquare = sasquare;
			this.sdsquare = sdsquare;
			this.bhat = bhat;
		}

		public double f_to_minimize(double alpha) {
			DoubleMatrix3D sdloc = sdsquare.copy();
			DoubleMatrix3D denom = sdloc.copy();

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
