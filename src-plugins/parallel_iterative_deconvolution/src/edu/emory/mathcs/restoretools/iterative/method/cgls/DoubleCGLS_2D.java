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
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DoubleAlgebra;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.DoubleCommon_2D;
import edu.emory.mathcs.restoretools.iterative.OutputType;
import edu.emory.mathcs.restoretools.iterative.ResizingType;
import edu.emory.mathcs.restoretools.iterative.psf.DoublePSFMatrix_2D;

/**
 * Conjugate Gradient for Least Squares.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleCGLS_2D {

	private static final DoubleAlgebra alg = DoubleAlgebra.DEFAULT;

	private DoublePSFMatrix_2D PSF;

	private DoubleMatrix2D B;

	private java.awt.image.ColorModel cmY;

	private int bwidth;

	private int bheight;

	private int maxIts;

	private double tol;

	private double[] rnrm;

	private double[] xnrm;

	private boolean xnorm;

	private boolean showIteration;
	
	private OutputType output;

	/**
	 * Creates new instance of DoubleCGLS_2D
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
	public DoubleCGLS_2D(ImagePlus imB, ImagePlus[][] imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, int maxIts, double tol, boolean xnorm, boolean showIteration) {
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
		cmY = ipB.getColorModel();
		bwidth = ipB.getWidth();
		bheight = ipB.getHeight();
		PSF = new DoublePSFMatrix_2D(imPSF, boundary, resizing, new int[] { bheight, bwidth });

		B = new DenseDoubleMatrix2D(bheight, bwidth);
		DoubleCommon_2D.assignPixelsToMatrix_2D(B, ipB);
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
	public ImagePlus deblur(double threshold) {
		DoubleMatrix2D p, q, r, s;
		double alpha;
		double beta;
		double gamma;
		double oldgamma = 0;
		double nq;

		rnrm = new double[maxIts + 1];
		if (xnorm == true) {
			xnrm = new double[maxIts + 1];
		}
		double nrm_trAb = alg.vectorNorm2(PSF.times(B, true));
		if (tol == -1.0)
			tol = DoubleCommon_2D.sqrteps * nrm_trAb;

		s = PSF.times(B, false);
		s.assign(DoubleFunctions.neg);
		s.assign(B, DoubleFunctions.plus);
		r = PSF.times(s, true);
		p = r;
		gamma = alg.vectorNorm2(r);
		rnrm[0] = gamma;
		gamma = gamma * gamma;
		if (xnorm == true) {
			xnrm[0] = alg.vectorNorm2(B);
		}
		ImagePlus imX = null;
		FloatProcessor ip = new FloatProcessor(bwidth, bheight);
		if (showIteration == true) {
			DoubleCommon_2D.assignPixelsToProcessor(ip, B, cmY);
			imX = new ImagePlus("(deblurred)", ip);
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
				p.assign(DoubleFunctions.mult(beta));
				p.assign(r, DoubleFunctions.plus);
			}
			q = PSF.times(p, false);
			nq = alg.vectorNorm2(q);
			nq = nq * nq;
			alpha = gamma / nq;
			B.assign(p.copy().assign(DoubleFunctions.mult(alpha)), DoubleFunctions.plus);
			s.assign(q.assign(DoubleFunctions.mult(alpha)), DoubleFunctions.minus);
			r = PSF.times(s, true);
			oldgamma = gamma;
			gamma = alg.vectorNorm2(r);
			rnrm[k + 1] = gamma;
			gamma = gamma * gamma;
			if (xnorm == true) {
				xnrm[k + 1] = alg.vectorNorm2(B);
			}
			if (showIteration == true) {
				if (threshold == -1.0) {
					DoubleCommon_2D.assignPixelsToProcessor(ip, B, cmY);
				} else {
					DoubleCommon_2D.assignPixelsToProcessor(ip, B, cmY, threshold);
				}
				imX.updateAndDraw();
			}
		}
		for (int i = 0; i < k + 1; i++)
			rnrm[i] /= nrm_trAb;
		if (showIteration == false) {
			if (threshold == -1.0) {
				DoubleCommon_2D.assignPixelsToProcessor(ip, B, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, B, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", ip);
		}
		DoubleCommon_2D.convertImage(imX, output);
		return imX;
	}

	/**
	 * Returns the norm of the residual at each iteration.
	 * 
	 * @return the norm of the residual at each iteration
	 */
	public double[] getRnorm() {
		return rnrm;
	}

	/**
	 * Returns the norm of the solution at each iteration.
	 * 
	 * @return the norm of the solution at each iteration
	 */
	public double[] getXnorm() {
		return xnrm;
	}
}
