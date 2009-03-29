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

package edu.emory.mathcs.restoretools.iterative.method.cgls;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import cern.colt.matrix.tfloat.algo.FloatAlgebra;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.FloatCommon_2D;
import edu.emory.mathcs.restoretools.iterative.FloatCommon_3D;
import edu.emory.mathcs.restoretools.iterative.OutputType;
import edu.emory.mathcs.restoretools.iterative.ResizingType;
import edu.emory.mathcs.restoretools.iterative.preconditioner.FloatFFTPreconditioner_3D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.FloatPreconditioner_3D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.PreconditionerType;
import edu.emory.mathcs.restoretools.iterative.psf.FloatPSFMatrix_3D;

/**
 * Preconditioned Conjugate Gradient for Least Squares.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatPCGLS_3D {
	private static final FloatAlgebra alg = FloatAlgebra.DEFAULT;

	private FloatPSFMatrix_3D PSF;

	private FloatMatrix3D B;

	private java.awt.image.ColorModel cmY;

	private int bdepth;

	private int bwidth;

	private int bheight;

	private FloatPreconditioner_3D P;

	private int maxIts;

	private float tol;

	private float[] rnrm;

	private float[] xnrm;

	private boolean xnorm;

	private boolean showIteration;
	
	private OutputType output;

	/**
	 * Creates new instance of FloatPCGLS_3D
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
	 * @param Prec
	 *            type of a preconditioner
	 * @param precTol
	 *            tolerance for a preconditioner
	 * @param maxIts
	 *            maximal number of iterations
	 * @param tol
	 *            stopping tolerance
	 * @param xnorm
	 *            if true then the norm of the solution is computed
	 * @param showIteration
	 *            if true then the restored image is shown after each iteration
	 */
	public FloatPCGLS_3D(ImagePlus imB, ImagePlus[][][] imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, PreconditionerType Prec, float precTol, int maxIts, float tol, boolean xnorm, boolean showIteration) {
		IJ.showStatus("CGLS initialization...");
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
		switch (Prec) {
		case FFT:
			this.P = new FloatFFTPreconditioner_3D(PSF, B, precTol);
			break;
		}
		IJ.showStatus("CGLS initialization...");
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
		FloatMatrix3D p, q, r, s, tr;
		float alpha;
		float beta;
		float gamma;
		float oldgamma = 0;
		float nq;

		rnrm = new float[maxIts + 1];
		if (xnorm == true) {
			xnrm = new float[maxIts + 1];
		}
		float nrm_trAb = alg.vectorNorm2(PSF.times(B, true));

		if (tol == -1.0)
			tol = FloatCommon_2D.sqrteps * nrm_trAb;

		s = PSF.times(B, false);
		s.assign(FloatFunctions.neg);
		s.assign(B, FloatFunctions.plus);
		tr = PSF.times(s, true);
		r = P.solve(tr, true);

		p = r;

		gamma = alg.vectorNorm2(r);
		gamma = gamma * gamma;
		rnrm[0] = alg.vectorNorm2(tr);
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
		for (k = 0; k < maxIts; k++) {
			if (rnrm[k] <= tol) {
				break;
			}
			IJ.showStatus("CGLS iteration: " + (k + 1) + "/" + maxIts);
			if (k >= 1) {
				beta = gamma / oldgamma;
				p.assign(FloatFunctions.mult(beta));
				p.assign(r, FloatFunctions.plus);
			}
			tr = P.solve(p, false);
			q = PSF.times(tr, false);
			nq = alg.vectorNorm2(q);
			nq = nq * nq;
			alpha = gamma / nq;
			B.assign(tr.assign(FloatFunctions.mult(alpha)), FloatFunctions.plus);
			s.assign(q.assign(FloatFunctions.mult(alpha)), FloatFunctions.minus);
			tr = PSF.times(s, true);
			r = P.solve(tr, true);
			oldgamma = gamma;
			gamma = alg.vectorNorm2(r);
			gamma = gamma * gamma;
			rnrm[k + 1] = alg.vectorNorm2(tr);
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
		for (int i = 0; i < k + 1; i++)
			rnrm[i] /= nrm_trAb;
		if (showIteration == false) {
			if (threshold == -1.0) {
				FloatCommon_3D.assignPixelsToStack(is, B, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStack(is, B, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", is);
		}
		FloatCommon_3D.convertImage(imX, output);
		return imX;

	}

	/**
	 * Returns the tolerance for a preconditioner.
	 * 
	 * @return the tolerance for a preconditioner
	 */
	public float getPreconditionerTolerance() {
		return P.getTolerance();
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
