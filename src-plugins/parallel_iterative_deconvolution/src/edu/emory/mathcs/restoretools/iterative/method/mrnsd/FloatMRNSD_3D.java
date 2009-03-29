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

package edu.emory.mathcs.restoretools.iterative.method.mrnsd;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.image.ColorModel;

import cern.colt.list.tfloat.FloatArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import cern.colt.matrix.tfloat.algo.FloatAlgebra;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.FloatCommon_2D;
import edu.emory.mathcs.restoretools.iterative.FloatCommon_3D;
import edu.emory.mathcs.restoretools.iterative.OutputType;
import edu.emory.mathcs.restoretools.iterative.ResizingType;
import edu.emory.mathcs.restoretools.iterative.psf.FloatPSFMatrix_3D;

/**
 * Modified Residual Norm Steepest Descent. This is a nonnegatively constrained
 * steepest descent method.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatMRNSD_3D {
	private static final FloatAlgebra alg = FloatAlgebra.DEFAULT;

	private FloatPSFMatrix_3D PSF;

	private FloatMatrix3D B;

	private ColorModel cmY;

	private int bdepth;

	private int bwidth;

	private int bheight;

	private int maxIts;

	private float tol;

	private float[] rnrm;

	private float[] xnrm;

	private boolean xnorm;

	boolean showIteration;
	
	private OutputType output;

	/**
	 * Creates new instance of FloatMRNSD_3D.
	 * 
	 * @param imB
	 *            blurred image
	 * @param imPSF
	 *            PSFs
	 * @param boundary
	 *            boundary conditions
	 * @param resizing
	 *            type of resizing
	 * @param output
	 *            type of an output
	 * @param maxIts
	 *            maximal number of iterations
	 * @param tol
	 *            stopping tolerance
	 * @param xnorm
	 *            if true then the norm of a solution is computed
	 * @param showIteration
	 *            if true then the restored image is shown after each iteration
	 */
	public FloatMRNSD_3D(ImagePlus imB, ImagePlus[][][] imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, int maxIts, float tol, boolean xnorm, boolean showIteration) {
		IJ.showStatus("MRNSD initialization...");
		ImageProcessor ipB = imB.getProcessor();
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
		ImageStack isB = imB.getStack();
		cmY = ipB.getColorModel();
		bdepth = isB.getSize();
		bwidth = ipB.getWidth();
		bheight = ipB.getHeight();
		PSF = new FloatPSFMatrix_3D(imPSF, boundary, resizing, new int[] { bdepth, bheight, bwidth });
		B = new DenseFloatMatrix3D(bdepth, bheight, bwidth);
		FloatCommon_3D.assignPixelsToMatrix_3D(isB, B);
		this.maxIts = maxIts;
		this.tol = tol;
		this.xnorm = xnorm;
		this.showIteration = showIteration;
	}

	/**
	 * 
	 * Performs deblurring operation.
	 * 
	 * @param threshold
	 *            the smallest positive value assigned to the restored image,
	 *            all the values less than the threshold are set to zero
	 * 
	 * @return deblurred image
	 */
	public ImagePlus deblur(float threshold) {
		float alpha;
		float gamma;
		float theta;
		float nrm_trAb;
		FloatMatrix3D r, s, v, w;
		IntArrayList sliceList, rowList, columnList;
		FloatArrayList valueList;
		float tau = FloatCommon_2D.sqrteps;
		float sigsq = tau;
		float[] minAndLoc = B.getMinLocation();
		float minX = minAndLoc[0];
		if (minX < 0) {
			B.assign(FloatFunctions.minus(Math.min(0, minX) + sigsq));
		}
		rnrm = new float[maxIts + 1];
		if (xnorm == true) {
			xnrm = new float[maxIts + 1];
		}
		nrm_trAb = alg.vectorNorm2(PSF.times(B, true));
		if (tol == -1.0) {
			tol = FloatCommon_2D.sqrteps * nrm_trAb;
		}

		r = PSF.times(B, false);
		r.assign(FloatFunctions.neg);
		r.assign(B, FloatFunctions.plus);
		r = PSF.times(r, true);
		r.assign(FloatFunctions.neg);
		gamma = B.aggregate(r, FloatFunctions.plus, FloatFunctions.multSquare);

		rnrm[0] = (float) Math.sqrt(gamma);
		if (xnorm == true) {
			xnrm[0] = alg.vectorNorm2(B);
		}
		ImagePlus imX = null;
		ImageStack is = new ImageStack(bwidth, bheight);
		if (showIteration == true) {
			FloatCommon_3D.assignPixelsToStack(is, B, cmY);
			imX = new ImagePlus("(deblurred)", is);
			imX.show();
		}
		int k;
		sliceList = new IntArrayList(B.size() / 2);
		rowList = new IntArrayList(B.size() / 2);
		columnList = new IntArrayList(B.size() / 2);
		valueList = new FloatArrayList(B.size() / 2);
		for (k = 0; k < maxIts; k++) {
			if (rnrm[k] <= tol) {
				break;
			}
			IJ.showStatus("MRNSD iteration: " + (k + 1) + "/" + maxIts);
			s = B.copy();
			s.assign(r, FloatFunctions.multNeg);
			v = PSF.times(s, false);
			theta = gamma / v.aggregate(FloatFunctions.plus, FloatFunctions.square);
			s.getNegativeValues(sliceList, rowList, columnList, valueList);
			w = B.copy();
			w.assign(s, FloatFunctions.divNeg, sliceList, rowList, columnList);
			alpha = Math.min(theta, w.aggregate(FloatFunctions.min, FloatFunctions.identity, sliceList, rowList, columnList));
			B.assign(s.assign(FloatFunctions.mult(alpha)), FloatFunctions.plus);
			w = PSF.times(v, true);
			r.assign(w.assign(FloatFunctions.mult(alpha)), FloatFunctions.plus);
			gamma = B.aggregate(r, FloatFunctions.plus, FloatFunctions.multSquare);
			rnrm[k + 1] = (float) Math.sqrt(gamma);
			if (xnorm == true) {
				xnrm[k + 1] = alg.vectorNorm2(B);
			}
			if (showIteration == true) {
				if (threshold == -1.0) {
					FloatCommon_3D.updatePixelsInStack(is, B, cmY);
				} else {
					FloatCommon_3D.updatePixelsInStack(is, B, cmY, threshold);
				}
				ImageProcessor ip1 = imX.getProcessor();
				ip1.setMinAndMax(0, 0);
				ip1.setColorModel(cmY);
				imX.updateAndDraw();
			}
		}
		for (int i = 0; i < k + 1; i++) {
			rnrm[i] = rnrm[i] / nrm_trAb;
		}
		if (showIteration == false) {
			if (threshold == -1.0) {
				FloatCommon_3D.assignPixelsToStack(is, B, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(is, B, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", is);
			;
		}
		FloatCommon_3D.convertImage(imX, output);
		return imX;

	}

	/**
	 * Returns the norm of the residual at each iteration.
	 * 
	 * @return the norm of the residual at each iteration
	 */
	public float[] getRnorm() {
		return rnrm;
	}

	/**
	 * Returns the norm of the solution at each iteration.
	 * 
	 * @return the norm of the solution at each iteration
	 */
	public float[] getXnorm() {
		return xnrm;
	}

}
