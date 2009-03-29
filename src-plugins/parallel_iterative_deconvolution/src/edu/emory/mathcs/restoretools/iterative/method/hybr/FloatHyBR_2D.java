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

package edu.emory.mathcs.restoretools.iterative.method.hybr;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import optimization.FloatFmin;
import optimization.FloatFmin_methods;
import cern.colt.list.tfloat.FloatArrayList;
import cern.colt.matrix.tfloat.FloatMatrix1D;
import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix1D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import cern.colt.matrix.tfloat.algo.FloatAlgebra;
import cern.colt.matrix.tfloat.algo.FloatSingularValueDecompositionDC;
import cern.jet.math.tfloat.FloatFunctions;
import cern.jet.stat.tfloat.FloatDescriptive;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.FloatCommon_2D;
import edu.emory.mathcs.restoretools.iterative.OutputType;
import edu.emory.mathcs.restoretools.iterative.ResizingType;
import edu.emory.mathcs.restoretools.iterative.method.hybr.lbd.FloatLBD_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.lbd.FloatPLBD_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.lbd.FloatSimpleLBD_2D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.FloatFFTPreconditioner_2D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.FloatPreconditioner_2D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.PreconditionerType;
import edu.emory.mathcs.restoretools.iterative.psf.FloatPSFMatrix_2D;

/**
 * Hybrid Bidiagonalization Regularization.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatHyBR_2D {

	private FloatHyBROptions options;

	private FloatPSFMatrix_2D PSF;

	private FloatMatrix2D b;

	private java.awt.image.ColorModel cmY;

	private int bwidth;

	private int bheight;

	private int maxIts;

	private FloatPreconditioner_2D P;

	private static final FloatAlgebra alg = FloatAlgebra.DEFAULT;

	private int n;

	private boolean showIteration;
	
	private OutputType output;

	/**
	 * Creates new instance of FloatHyBR_2D
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
	 * @param options
	 *            options for HyBR
	 * @param showIteration
	 *            if true then the restored image is shown after each iteration
	 */
	public FloatHyBR_2D(ImagePlus imB, ImagePlus[][] imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, PreconditionerType Prec, float precTol, int maxIts, FloatHyBROptions options, boolean showIteration) {
		IJ.showStatus("HyBR initialization...");
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
		PSF = new FloatPSFMatrix_2D(imPSF, boundary, resizing, new int[] { bheight, bwidth });
		b = FloatCommon_2D.assignPixelsToMatrix_2D(ipB);
		switch (Prec) {
		case FFT:
			this.P = new FloatFFTPreconditioner_2D(PSF, b, precTol);
			break;
		case NONE:
			this.P = null;
			break;
		}
		IJ.showStatus("HyBR initialization...");
		this.maxIts = maxIts;
		int[] psfSize = PSF.getSize();
		n = psfSize[1];
		if (options == null) {
			this.options = new FloatHyBROptions(InSolvType.TIKHONOV, RegMethodType.ADAPTWGCV, -1, 0, false, 2, 1e-4f);
		} else {
			this.options = options;
		}
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
		int i;
		boolean bump = false;
		boolean terminate = true;
		boolean warning = false;
		int iterations_save = 0;
		float alpha, beta;
		InSolvType insolve = InSolvType.NONE;
		FloatLBD_2D lbd;
		FloatMatrix2D V = null;
		FloatMatrix2D B = null;
		FloatMatrix1D vector;
		FloatMatrix1D work_vec;
		FloatMatrix2D work_mat;
		FloatMatrix2D Ub, Vb;
		FloatMatrix1D f = null;
		FloatMatrix1D x_out = null;
		FloatMatrix1D x_save = null;
		FloatMatrix1D x = null;
		float[] sv;
		FloatMatrix2D[] lbdresult;
		FloatArrayList omega = new FloatArrayList();
		FloatArrayList GCV = null;
		FloatMatrix1D b_local = b.vectorize();
		FloatMatrix2D U = new DenseFloatMatrix2D(b_local.size(), 1);
		if (P == null) {
			beta = alg.norm2(b_local);
			U.viewColumn(0).assign(b_local);
			U.assign(FloatFunctions.div(beta));
			lbd = new FloatSimpleLBD_2D();
		} else {
			U.viewColumn(0).assign(P.solve(b_local, false));
			beta = alg.vectorNorm2(U);
			U.assign(FloatFunctions.div(beta));
			lbd = new FloatPLBD_2D(P);
		}
		ImagePlus imX = null;
		FloatProcessor ip = new FloatProcessor(bwidth, bheight);
		if (showIteration == true) {
			FloatCommon_2D.assignPixelsToProcessor(ip, b, cmY);
			imX = new ImagePlus("(deblurred)", ip);
			imX.show();
			IJ.showStatus("HyBR initialization...");
		}
		for (i = 0; i <= maxIts; i++) {
			lbdresult = lbd.perform(PSF, U, B, V, options);
			U = lbdresult[0];
			B = lbdresult[1];
			V = lbdresult[2];
			vector = new DenseFloatMatrix1D(U.columns());
			vector.setQuick(0, beta);
			if (i >= 1) {
				IJ.showStatus("HyBR iteration: " + i + "/" + maxIts);
				if (i >= options.begReg - 1) {
					insolve = options.inSolv;
				}
				switch (insolve) {
				case TIKHONOV:
					FloatSingularValueDecompositionDC svd = alg.svdDC(B);
					Ub = svd.getU();
					sv = svd.getSingularValues();
					Vb = svd.getV();
					if (options.regMethod == RegMethodType.ADAPTWGCV) {
						work_vec = new DenseFloatMatrix1D(Ub.rows());
						Ub.zMult(vector, work_vec, 1, 0, true);
						omega.add(Math.min(1, findOmega(work_vec, sv)));
						options.omega = FloatDescriptive.mean(omega);
					}
					f = tikhonovSolver(Ub, sv, Vb, vector, options);
					alpha = options.regPar;
					if (GCV == null) {
						GCV = new FloatArrayList(new float[options.begReg]);
					}
					GCV.add(GCVstopfun(alpha, Ub.viewRow(0), sv, beta, n));
					if ((i > 1) && (terminate == true)) {
						if (Math.abs((GCV.get(i) - GCV.get(i - 1))) / GCV.get(options.begReg) < options.flatTol) {
							x_out = new DenseFloatMatrix1D(V.rows());
							V.zMult(f, x_out);
							terminate = false;
							if (threshold == -1.0) {
								FloatCommon_2D.assignPixelsToProcessor(ip, x_out, cmY);
							} else {
								FloatCommon_2D.assignPixelsToProcessor(ip, x_out, cmY, threshold);
							}
							if (showIteration == false) {
								imX = new ImagePlus("(deblurred)", ip);
							} else {
								imX.updateAndDraw();
							}
							FloatCommon_2D.convertImage(imX, output);
							return imX;
						} else if ((warning == true) && (GCV.size() > iterations_save + 3)) {
							for (int j = iterations_save; j < GCV.size() - 1; j++) {
								if (GCV.get(iterations_save) > GCV.get(j + 1)) {
									bump = true;
								}
							}
							if (bump == false) {
								x_out = x_save;
								terminate = false;
								if (threshold == -1.0) {
									FloatCommon_2D.assignPixelsToProcessor(ip, x_out, cmY);
								} else {
									FloatCommon_2D.assignPixelsToProcessor(ip, x_out, cmY, threshold);
								}
								if (showIteration == false) {
									imX = new ImagePlus("(deblurred)", ip);
								} else {
									imX.updateAndDraw();
								}
								FloatCommon_2D.convertImage(imX, output);
								return imX;

							} else {
								bump = false;
								warning = false;
								x_out = null;
								iterations_save = maxIts;
							}
						} else if (warning == false) {
							if (GCV.get(i - 1) < GCV.get(i)) {
								warning = true;
								x_save = new DenseFloatMatrix1D(V.rows());
								V.zMult(f, x_save);
								iterations_save = i;
							}
						}
					}
					break;
				case NONE:
					work_mat = new DenseFloatMatrix2D(vector.size(), 1);
					work_mat = alg.solve(B, work_mat);
					f = work_mat.viewColumn(0);
					break;
				}
				x = new DenseFloatMatrix1D(V.rows());
				V.zMult(f, x);
				if (showIteration == true) {
					if (threshold == -1.0) {
						FloatCommon_2D.assignPixelsToProcessor(ip, x, cmY);
					} else {
						FloatCommon_2D.assignPixelsToProcessor(ip, x, cmY, threshold);
					}
					imX.updateAndDraw();
				}
			}
		}
		if (x_out == null) {
			x_out = x;
		}
		if (showIteration == false) {
			if (threshold == -1.0) {
				FloatCommon_2D.assignPixelsToProcessor(ip, x_out, cmY);
			} else {
				FloatCommon_2D.assignPixelsToProcessor(ip, x_out, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", ip);
		}
		FloatCommon_2D.convertImage(imX, output);
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

	private float findOmega(FloatMatrix1D bhat, float[] s) {
		int m = bhat.size();
		int n = s.length;
		float alpha = s[n - 1];
		float t0 = bhat.viewPart(n, m - n).aggregate(FloatFunctions.plus, FloatFunctions.square);
		FloatMatrix1D s2 = new DenseFloatMatrix1D(s);
		s2.assign(FloatFunctions.square);
		float alpha2 = alpha * alpha;
		FloatMatrix1D tt = s2.copy();
		tt.assign(FloatFunctions.plus(alpha2));
		tt.assign(FloatFunctions.inv);
		float t1 = s2.aggregate(tt, FloatFunctions.plus, FloatFunctions.mult);
		s2 = new DenseFloatMatrix1D(s);
		s2.assign(FloatFunctions.mult(alpha));
		s2.assign(bhat.viewPart(0, n), FloatFunctions.mult);
		s2.assign(FloatFunctions.square);
		FloatMatrix1D work_vec = tt.copy();
		work_vec.assign(FloatFunctions.pow(3));
		work_vec.assign(FloatFunctions.abs);
		float t3 = work_vec.aggregate(s2, FloatFunctions.plus, FloatFunctions.mult);
		work_vec = new DenseFloatMatrix1D(s);
		work_vec.assign(tt, FloatFunctions.mult);
		float t4 = work_vec.aggregate(FloatFunctions.plus, FloatFunctions.square);
		work_vec = tt.copy();
		work_vec.assign(bhat.viewPart(0, n), FloatFunctions.mult);
		work_vec.assign(FloatFunctions.mult(alpha2));
		float t5 = work_vec.aggregate(FloatFunctions.plus, FloatFunctions.square);
		s2 = new DenseFloatMatrix1D(s);
		s2.assign(bhat.viewPart(0, n), FloatFunctions.mult);
		s2.assign(FloatFunctions.square);
		tt.assign(FloatFunctions.pow(3));
		tt.assign(FloatFunctions.abs);
		float v2 = tt.aggregate(s2, FloatFunctions.plus, FloatFunctions.mult);
		return (m * alpha2 * v2) / (t1 * t3 + t4 * (t5 + t0));
	}

	private FloatMatrix1D tikhonovSolver(FloatMatrix2D U, float[] s, FloatMatrix2D V, FloatMatrix1D b, FloatHyBROptions options) {
		TikFmin_2D fmin;
		FloatMatrix1D bhat = new DenseFloatMatrix1D(U.rows());
		U.zMult(b, bhat, 1, 0, true);
		float alpha = 0;
		switch (options.regMethod) {
		case GCV:
			fmin = new TikFmin_2D(bhat, s, 1);
			alpha = FloatFmin.fmin(0, 1, fmin, FloatCommon_2D.FMIN_TOL);
			break;
		case WGCV:
			fmin = new TikFmin_2D(bhat, s, options.omega);
			alpha = FloatFmin.fmin(0, 1, fmin, FloatCommon_2D.FMIN_TOL);
			break;
		case ADAPTWGCV:
			fmin = new TikFmin_2D(bhat, s, options.omega);
			alpha = FloatFmin.fmin(0, 1, fmin, FloatCommon_2D.FMIN_TOL);
			break;
		case NONE: // regularization parameter is given
			alpha = options.regPar;
			break;
		}
		FloatMatrix1D d = new DenseFloatMatrix1D(s);
		d.assign(FloatFunctions.square);
		d.assign(FloatFunctions.plus(alpha * alpha));
		bhat = bhat.viewPart(0, s.length);
		FloatMatrix1D S = new DenseFloatMatrix1D(s);
		bhat.assign(S, FloatFunctions.mult);
		bhat.assign(d, FloatFunctions.div);
		FloatMatrix1D x = new DenseFloatMatrix1D(V.rows());
		V.zMult(bhat, x);
		options.regPar = alpha;
		return x;
	}

	private static class TikFmin_2D implements FloatFmin_methods {
		FloatMatrix1D bhat;

		float[] s;

		float omega;

		public TikFmin_2D(FloatMatrix1D bhat, float[] s, float omega) {
			this.bhat = bhat;
			this.s = s;
			this.omega = omega;
		}

		public float f_to_minimize(float alpha) {
			int m = bhat.size();
			int n = s.length;
			float t0 = bhat.viewPart(n, m - n).aggregate(FloatFunctions.plus, FloatFunctions.square);
			FloatMatrix1D s2 = new DenseFloatMatrix1D(s);
			s2.assign(FloatFunctions.square);
			float alpha2 = alpha * alpha;
			FloatMatrix1D work_vec = s2.copy();
			work_vec.assign(FloatFunctions.plus(alpha2));
			work_vec.assign(FloatFunctions.inv);
			FloatMatrix1D t1 = work_vec.copy();
			t1.assign(FloatFunctions.mult(alpha2));
			FloatMatrix1D t2 = t1.copy();
			t2.assign(bhat.viewPart(0, n), FloatFunctions.mult);
			FloatMatrix1D t3 = work_vec.copy();
			t3.assign(s2, FloatFunctions.mult);
			t3.assign(FloatFunctions.mult(1 - omega));
			float denom = t3.aggregate(t1, FloatFunctions.plus, FloatFunctions.plus) + m - n;
			return n * (t2.aggregate(FloatFunctions.plus, FloatFunctions.square) + t0) / (denom * denom);
		}

	}

	private float GCVstopfun(float alpha, FloatMatrix1D u, float[] s, float beta, int n) {
		int k = s.length;
		float beta2 = beta * beta;
		FloatMatrix1D s2 = new DenseFloatMatrix1D(s);
		s2.assign(FloatFunctions.square);
		float alpha2 = alpha * alpha;
		FloatMatrix1D t1 = s2.copy();
		t1.assign(FloatFunctions.plus(alpha2));
		t1.assign(FloatFunctions.inv);
		FloatMatrix1D t2 = t1.copy();
		t2.assign(u.viewPart(0, k), FloatFunctions.mult);
		t2.assign(FloatFunctions.mult(alpha2));
		float num = beta2 * (t2.aggregate(FloatFunctions.plus, FloatFunctions.square) + (float) Math.pow(Math.abs(u.getQuick(k)), 2)) / (float) n;
		float den = (n - t1.aggregate(s2, FloatFunctions.plus, FloatFunctions.mult)) / (float) n;
		den = den * den;
		return num / den;
	}
}
