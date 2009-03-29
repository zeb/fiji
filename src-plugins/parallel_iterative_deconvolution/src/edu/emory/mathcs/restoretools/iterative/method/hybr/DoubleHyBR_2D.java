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
import optimization.DoubleFmin;
import optimization.DoubleFmin_methods;
import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DoubleAlgebra;
import cern.colt.matrix.tdouble.algo.DoubleSingularValueDecompositionDC;
import cern.jet.math.tdouble.DoubleFunctions;
import cern.jet.stat.tdouble.DoubleDescriptive;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.DoubleCommon_2D;
import edu.emory.mathcs.restoretools.iterative.OutputType;
import edu.emory.mathcs.restoretools.iterative.ResizingType;
import edu.emory.mathcs.restoretools.iterative.method.hybr.lbd.DoubleLBD_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.lbd.DoublePLBD_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.lbd.DoubleSimpleLBD_2D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.DoubleFFTPreconditioner_2D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.DoublePreconditioner_2D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.PreconditionerType;
import edu.emory.mathcs.restoretools.iterative.psf.DoublePSFMatrix_2D;

/**
 * Hybrid Bidiagonalization Regularization.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleHyBR_2D {

	private DoubleHyBROptions options;

	private DoublePSFMatrix_2D PSF;

	private DoubleMatrix2D b;

	private java.awt.image.ColorModel cmY;

	private int bwidth;

	private int bheight;

	private int maxIts;

	private DoublePreconditioner_2D P;

	private static final DoubleAlgebra alg = DoubleAlgebra.DEFAULT;

	private int n;

	private boolean showIteration;

	private OutputType output;

	/**
	 * Creates new instance of DoubleHyBR_2D
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
	public DoubleHyBR_2D(ImagePlus imB, ImagePlus[][] imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, PreconditionerType Prec, double precTol, int maxIts, DoubleHyBROptions options, boolean showIteration) {
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
		PSF = new DoublePSFMatrix_2D(imPSF, boundary, resizing, new int[] { bheight, bwidth });
		b = new DenseDoubleMatrix2D(bheight, bwidth);
		DoubleCommon_2D.assignPixelsToMatrix_2D(b, ipB);
		switch (Prec) {
		case FFT:
			this.P = new DoubleFFTPreconditioner_2D(PSF, b, precTol);
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
			this.options = new DoubleHyBROptions(InSolvType.TIKHONOV, RegMethodType.ADAPTWGCV, -1, 0, false, 2, 1e-6);
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
	public ImagePlus deblur(double threshold) {
		int i;
		boolean bump = false;
		boolean terminate = true;
		boolean warning = false;
		int iterations_save = 0;
		double alpha, beta;
		InSolvType insolve = InSolvType.NONE;
		DoubleLBD_2D lbd;
		DoubleMatrix2D V = null;
		DoubleMatrix2D B = null;
		DoubleMatrix1D vector;
		DoubleMatrix1D work_vec;
		DoubleMatrix2D work_mat;
		DoubleMatrix2D Ub, Vb;
		DoubleMatrix1D f = null;
		DoubleMatrix1D x_out = null;
		DoubleMatrix1D x_save = null;
		DoubleMatrix1D x = null;
		double[] sv;
		DoubleMatrix2D[] lbdresult;
		DoubleArrayList omega = new DoubleArrayList();
		DoubleArrayList GCV = null;
		DoubleMatrix1D b_local = b.vectorize();
		DoubleMatrix2D U = new DenseDoubleMatrix2D(b_local.size(), 1);
		if (P == null) {
			beta = alg.norm2(b_local);
			U.viewColumn(0).assign(b_local);
			U.assign(DoubleFunctions.div(beta));
			lbd = new DoubleSimpleLBD_2D();
		} else {
			U.viewColumn(0).assign(P.solve(b_local, false));
			beta = alg.vectorNorm2(U);
			U.assign(DoubleFunctions.div(beta));
			lbd = new DoublePLBD_2D(P);
		}
		ImagePlus imX = null;
		FloatProcessor ip = new FloatProcessor(bwidth, bheight);
		if (showIteration == true) {
			DoubleCommon_2D.assignPixelsToProcessor(ip, b, cmY);
			imX = new ImagePlus("(deblurred)", ip);
			imX.show();
			IJ.showStatus("HyBR initialization...");
		}
		for (i = 0; i <= maxIts; i++) {
			lbdresult = lbd.perform(PSF, U, B, V, options);
			U = lbdresult[0];
			B = lbdresult[1];
			V = lbdresult[2];
			vector = new DenseDoubleMatrix1D(U.columns());
			vector.setQuick(0, beta);
			if (i >= 1) {
				IJ.showStatus("HyBR iteration: " + i + "/" + maxIts);
				if (i >= options.begReg - 1) {
					insolve = options.inSolv;
				}
				switch (insolve) {
				case TIKHONOV:
					DoubleSingularValueDecompositionDC svd = alg.svdDC(B);
					Ub = svd.getU();
					sv = svd.getSingularValues();
					Vb = svd.getV();
					if (options.regMethod == RegMethodType.ADAPTWGCV) {
						work_vec = new DenseDoubleMatrix1D(Ub.rows());
						Ub.zMult(vector, work_vec, 1, 0, true);
						omega.add(Math.min(1, findOmega(work_vec, sv)));
						options.omega = DoubleDescriptive.mean(omega);
					}
					f = tikhonovSolver(Ub, sv, Vb, vector, options);
					alpha = options.regPar;
					if (GCV == null) {
						GCV = new DoubleArrayList(new double[options.begReg]);
					}
					GCV.add(GCVstopfun(alpha, Ub.viewRow(0), sv, beta, n));
					if ((i > 1) && (terminate == true)) {
						if (Math.abs((GCV.get(i) - GCV.get(i - 1))) / GCV.get(options.begReg) < options.flatTol) {
							x_out = new DenseDoubleMatrix1D(V.rows());
							V.zMult(f, x_out);
							terminate = false;
							if (threshold == -1.0) {
								DoubleCommon_2D.assignPixelsToProcessor(ip, x_out, cmY);
							} else {
								DoubleCommon_2D.assignPixelsToProcessor(ip, x_out, cmY, threshold);
							}
							if (showIteration == false) {
								imX = new ImagePlus("(deblurred)", ip);
							} else {
								imX.updateAndDraw();
							}
							DoubleCommon_2D.convertImage(imX, output);
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
									DoubleCommon_2D.assignPixelsToProcessor(ip, x_out, cmY);
								} else {
									DoubleCommon_2D.assignPixelsToProcessor(ip, x_out, cmY, threshold);
								}
								if (showIteration == false) {
									imX = new ImagePlus("(deblurred)", ip);
								} else {
									imX.updateAndDraw();
								}
								DoubleCommon_2D.convertImage(imX, output);
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
								x_save = new DenseDoubleMatrix1D(V.rows());
								V.zMult(f, x_save);
								iterations_save = i;
							}
						}
					}
					break;
				case NONE:
					work_mat = new DenseDoubleMatrix2D(vector.size(), 1);
					work_mat = alg.solve(B, work_mat);
					f = work_mat.viewColumn(0);
					break;
				}
				x = new DenseDoubleMatrix1D(V.rows());
				V.zMult(f, x);
				if (showIteration == true) {
					if (threshold == -1.0) {
						DoubleCommon_2D.assignPixelsToProcessor(ip, x, cmY);
					} else {
						DoubleCommon_2D.assignPixelsToProcessor(ip, x, cmY, threshold);
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
				DoubleCommon_2D.assignPixelsToProcessor(ip, x_out, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessor(ip, x_out, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", ip);
		}
		DoubleCommon_2D.convertImage(imX, output);
		return imX;

	}

	/**
	 * Returns the tolerance for a preconditioner.
	 * 
	 * @return the tolerance for a preconditioner
	 */
	public double getPreconditionerTolerance() {
		return P.getTolerance();
	}

	private double findOmega(DoubleMatrix1D bhat, double[] s) {
		int m = bhat.size();
		int n = s.length;
		double alpha = s[n - 1];
		double t0 = bhat.viewPart(n, m - n).aggregate(DoubleFunctions.plus, DoubleFunctions.square);
		DoubleMatrix1D s2 = new DenseDoubleMatrix1D(s);
		s2.assign(DoubleFunctions.square);
		double alpha2 = alpha * alpha;
		DoubleMatrix1D tt = s2.copy();
		tt.assign(DoubleFunctions.plus(alpha2));
		tt.assign(DoubleFunctions.inv);
		double t1 = s2.aggregate(tt, DoubleFunctions.plus, DoubleFunctions.mult);
		s2 = new DenseDoubleMatrix1D(s);
		s2.assign(DoubleFunctions.mult(alpha));
		s2.assign(bhat.viewPart(0, n), DoubleFunctions.mult);
		s2.assign(DoubleFunctions.square);
		DoubleMatrix1D work_vec = tt.copy();
		work_vec.assign(DoubleFunctions.pow(3));
		work_vec.assign(DoubleFunctions.abs);
		double t3 = work_vec.aggregate(s2, DoubleFunctions.plus, DoubleFunctions.mult);
		work_vec = new DenseDoubleMatrix1D(s);
		work_vec.assign(tt, DoubleFunctions.mult);
		double t4 = work_vec.aggregate(DoubleFunctions.plus, DoubleFunctions.square);
		work_vec = tt.copy();
		work_vec.assign(bhat.viewPart(0, n), DoubleFunctions.mult);
		work_vec.assign(DoubleFunctions.mult(alpha2));
		double t5 = work_vec.aggregate(DoubleFunctions.plus, DoubleFunctions.square);
		s2 = new DenseDoubleMatrix1D(s);
		s2.assign(bhat.viewPart(0, n), DoubleFunctions.mult);
		s2.assign(DoubleFunctions.square);
		tt.assign(DoubleFunctions.pow(3));
		tt.assign(DoubleFunctions.abs);
		double v2 = tt.aggregate(s2, DoubleFunctions.plus, DoubleFunctions.mult);
		return (m * alpha2 * v2) / (t1 * t3 + t4 * (t5 + t0));
	}

	private DoubleMatrix1D tikhonovSolver(DoubleMatrix2D U, double[] s, DoubleMatrix2D V, DoubleMatrix1D b, DoubleHyBROptions options) {
		TikFmin_2D fmin;
		DoubleMatrix1D bhat = new DenseDoubleMatrix1D(U.rows());
		U.zMult(b, bhat, 1, 0, true);
		double alpha = 0;
		switch (options.regMethod) {
		case GCV:
			fmin = new TikFmin_2D(bhat, s, 1);
			alpha = DoubleFmin.fmin(0, 1, fmin, DoubleCommon_2D.FMIN_TOL);
			break;
		case WGCV:
			fmin = new TikFmin_2D(bhat, s, options.omega);
			alpha = DoubleFmin.fmin(0, 1, fmin, DoubleCommon_2D.FMIN_TOL);
			break;
		case ADAPTWGCV:
			fmin = new TikFmin_2D(bhat, s, options.omega);
			alpha = DoubleFmin.fmin(0, 1, fmin, DoubleCommon_2D.FMIN_TOL);
			break;
		case NONE: // regularization parameter is given
			alpha = options.regPar;
			break;
		}
		DoubleMatrix1D d = new DenseDoubleMatrix1D(s);
		d.assign(DoubleFunctions.square);
		d.assign(DoubleFunctions.plus(alpha * alpha));
		bhat = bhat.viewPart(0, s.length);
		DoubleMatrix1D S = new DenseDoubleMatrix1D(s);
		bhat.assign(S, DoubleFunctions.mult);
		bhat.assign(d, DoubleFunctions.div);
		DoubleMatrix1D x = new DenseDoubleMatrix1D(V.rows());
		V.zMult(bhat, x);
		options.regPar = alpha;
		return x;
	}

	private static class TikFmin_2D implements DoubleFmin_methods {
		DoubleMatrix1D bhat;

		double[] s;

		double omega;

		public TikFmin_2D(DoubleMatrix1D bhat, double[] s, double omega) {
			this.bhat = bhat;
			this.s = s;
			this.omega = omega;
		}

		public double f_to_minimize(double alpha) {
			int m = bhat.size();
			int n = s.length;
			double t0 = bhat.viewPart(n, m - n).aggregate(DoubleFunctions.plus, DoubleFunctions.square);
			DoubleMatrix1D s2 = new DenseDoubleMatrix1D(s);
			s2.assign(DoubleFunctions.square);
			double alpha2 = alpha * alpha;
			DoubleMatrix1D work_vec = s2.copy();
			work_vec.assign(DoubleFunctions.plus(alpha2));
			work_vec.assign(DoubleFunctions.inv);
			DoubleMatrix1D t1 = work_vec.copy();
			t1.assign(DoubleFunctions.mult(alpha2));
			DoubleMatrix1D t2 = t1.copy();
			t2.assign(bhat.viewPart(0, n), DoubleFunctions.mult);
			DoubleMatrix1D t3 = work_vec.copy();
			t3.assign(s2, DoubleFunctions.mult);
			t3.assign(DoubleFunctions.mult(1 - omega));
			double denom = t3.aggregate(t1, DoubleFunctions.plus, DoubleFunctions.plus) + m - n;
			return n * (t2.aggregate(DoubleFunctions.plus, DoubleFunctions.square) + t0) / (denom * denom);
		}

	}

	private double GCVstopfun(double alpha, DoubleMatrix1D u, double[] s, double beta, int n) {
		int k = s.length;
		double beta2 = beta * beta;
		DoubleMatrix1D s2 = new DenseDoubleMatrix1D(s);
		s2.assign(DoubleFunctions.square);
		double alpha2 = alpha * alpha;
		DoubleMatrix1D t1 = s2.copy();
		t1.assign(DoubleFunctions.plus(alpha2));
		t1.assign(DoubleFunctions.inv);
		DoubleMatrix1D t2 = t1.copy();
		t2.assign(u.viewPart(0, k), DoubleFunctions.mult);
		t2.assign(DoubleFunctions.mult(alpha2));
		double num = beta2 * (t2.aggregate(DoubleFunctions.plus, DoubleFunctions.square) + Math.pow(Math.abs(u.getQuick(k)), 2)) / (double) n;
		double den = (n - t1.aggregate(s2, DoubleFunctions.plus, DoubleFunctions.mult)) / (double) n;
		den = den * den;
		return num / den;
	}
}
