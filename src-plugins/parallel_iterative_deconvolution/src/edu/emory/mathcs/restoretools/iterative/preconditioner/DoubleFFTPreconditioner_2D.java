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

package edu.emory.mathcs.restoretools.iterative.preconditioner;

import ij.IJ;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.function.tint.IntComparator;
import cern.colt.matrix.tdcomplex.DComplexMatrix2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DoubleSorting;
import cern.colt.matrix.AbstractMatrix2D;
import cern.colt.matrix.tdcomplex.impl.DenseDComplexMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.jet.math.tdcomplex.DComplex;
import cern.jet.math.tdcomplex.DComplexFunctions;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.DoubleCommon_2D;
import edu.emory.mathcs.restoretools.iterative.PaddingType;
import edu.emory.mathcs.restoretools.iterative.psf.DoublePSFMatrix_2D;
import edu.emory.mathcs.restoretools.iterative.psf.PSFType;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * 2D preconditioner based on the Fast Fourier Transform.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleFFTPreconditioner_2D implements DoublePreconditioner_2D {
	private AbstractMatrix2D matdata;

	private double tol;

	private BoundaryType boundary;

	private int[] imSize;

	private int[] psfSize;

	private int[] padSize;

	/**
	 * Creates new instance of DoubleFFTPreconditioner_2D.
	 * 
	 * @param PSFMatrix
	 *            PSF matrix
	 * @param B
	 *            blurred image
	 * @param tol
	 *            tolerance
	 */
	public DoubleFFTPreconditioner_2D(DoublePSFMatrix_2D PSFMatrix, DoubleMatrix2D B, double tol) {
		this.tol = tol;
		this.boundary = PSFMatrix.getBoundary();
		this.imSize = new int[2];
		imSize[0] = B.rows();
		imSize[1] = B.columns();
		if (PSFMatrix.getType() == PSFType.INVARIANT) {
			this.psfSize = PSFMatrix.getInvPsfSize();
			this.padSize = PSFMatrix.getInvPadSize();
		} else {
			this.psfSize = PSFMatrix.getPSF().getSize();
			int[] minimal = new int[2];
			minimal[0] = psfSize[0] + imSize[0];
			minimal[1] = psfSize[1] + imSize[1];
			switch (PSFMatrix.getResizing()) {
			case AUTO:
				int[] nextPowTwo = new int[2];
				if (!ConcurrencyUtils.isPowerOf2(minimal[0])) {
					nextPowTwo[0] = ConcurrencyUtils.nextPow2(minimal[0]);
				}
				else {
					nextPowTwo[0] = minimal[0];
				}
				if (!ConcurrencyUtils.isPowerOf2(minimal[1])) {
					nextPowTwo[1] = ConcurrencyUtils.nextPow2(minimal[1]);
				}
				else {
					nextPowTwo[1] = minimal[1];
				}
				if ((nextPowTwo[0] >= 1.5 * minimal[0]) || (nextPowTwo[1] >= 1.5 * minimal[1])) {
					//use minimal padding
					psfSize[0] = minimal[0];
					psfSize[1] = minimal[1];
				} else {
					psfSize[0] = nextPowTwo[0];
					psfSize[1] = nextPowTwo[1];
				}
				break;
			case MINIMAL:
				psfSize[0] = minimal[0];
				psfSize[1] = minimal[1];
				break;
			case NEXT_POWER_OF_TWO:
				psfSize[0] = minimal[0];
				psfSize[1] = minimal[1];
				if (!ConcurrencyUtils.isPowerOf2(psfSize[0])) {
					psfSize[0] = ConcurrencyUtils.nextPow2(psfSize[0]);
				}
				if (!ConcurrencyUtils.isPowerOf2(psfSize[1])) {
					psfSize[1] = ConcurrencyUtils.nextPow2(psfSize[1]);
				}
				break;
			}
			padSize = new int[2];
			if (imSize[0] < psfSize[0]) {
				padSize[0] = (psfSize[0] - imSize[0] + 1) / 2;
			}
			if (imSize[1] < psfSize[1]) {
				padSize[1] = (psfSize[1] - imSize[1] + 1) / 2;
			}

		}
		constructMatrix(PSFMatrix.getPSF().getImage(), B, PSFMatrix.getPSF().getCenter());
	}

	public double getTolerance() {
		return tol;
	}

	public DoubleMatrix1D solve(DoubleMatrix1D b, boolean transpose) {
		DoubleMatrix2D B = b.reshape(imSize[0], imSize[1]);
		B = solve(B, transpose);
		return B.vectorize();
	}

	public DoubleMatrix2D solve(AbstractMatrix2D B, boolean transpose) {
		switch (boundary) {
		case ZERO:
			B = DoubleCommon_2D.padZero_2D((DoubleMatrix2D) B, psfSize[0], psfSize[1]);
			break;
		case PERIODIC:
			B = DoubleCommon_2D.padPeriodic_2D((DoubleMatrix2D) B, psfSize[0], psfSize[1]);
			break;
		case REFLEXIVE:
			B = DoubleCommon_2D.padReflexive_2D((DoubleMatrix2D) B, psfSize[0], psfSize[1]);
			break;
		}
		B = ((DoubleMatrix2D) B).getFft2();
		if (transpose) {
			((DComplexMatrix2D) B).assign((DComplexMatrix2D) matdata, DComplexFunctions.multConjSecond);
		} else {
			((DComplexMatrix2D) B).assign((DComplexMatrix2D) matdata, DComplexFunctions.mult);
		}
		((DComplexMatrix2D) B).ifft2(true);
		B = ((DComplexMatrix2D) B).getRealPart();
		B = ((DoubleMatrix2D) B).viewPart(padSize[0], padSize[1], imSize[0], imSize[1]);
		return ((DoubleMatrix2D) B);
	}

	private void constructMatrix(DoubleMatrix2D[][] PSFs, DoubleMatrix2D B, int[][][] center) {
		matdata = PSFs[0][0].like();
		int[] center1 = center[0][0];
		int rows = PSFs.length;
		int columns = PSFs[0].length;
		int size = rows * columns;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				((DoubleMatrix2D) matdata).assign(PSFs[r][c], DoubleFunctions.plus);
			}
		}
		if (size != 1) {
			((DoubleMatrix2D) matdata).assign(DoubleFunctions.div(size));
		}
		switch (boundary) {
		case ZERO:
			B = DoubleCommon_2D.padZero_2D(B, psfSize[0], psfSize[1]);
			break;
		case PERIODIC:
			B = DoubleCommon_2D.padPeriodic_2D(B, psfSize[0], psfSize[1]);
			break;
		case REFLEXIVE:
			B = DoubleCommon_2D.padReflexive_2D(B, psfSize[0], psfSize[1]);
			break;
		}
		precMatrixOnePsf(center1, B);
	}

	private void precMatrixOnePsf(int[] center, DoubleMatrix2D Bpad) {
		int[] padSize = new int[2];
		padSize[0] = Bpad.rows() - matdata.rows();
		padSize[1] = Bpad.columns() - matdata.columns();
		if ((padSize[0] > 0) || (padSize[1] > 0)) {
			matdata = DoubleCommon_2D.padZero_2D((DoubleMatrix2D) matdata, padSize, PaddingType.POST);
		}
		matdata = DoubleCommon_2D.circShift_2D((DoubleMatrix2D) matdata, center);
		matdata = ((DoubleMatrix2D) matdata).getFft2();
		AbstractMatrix2D E = ((DComplexMatrix2D) matdata).copy();
		((DComplexMatrix2D) E).assign(DComplexFunctions.abs);
		E = ((DComplexMatrix2D) E).getRealPart();
		double[] maxAndLoc = ((DoubleMatrix2D) E).getMaxLocation();
		final double maxE = maxAndLoc[0];

		if (tol == -1) {
			IJ.showStatus("Computing tolerance for preconditioner...");
			double[] minAndLoc = ((DoubleMatrix2D) E).getMinLocation();
			double minE = minAndLoc[0];
			if (maxE / minE < 100) {
				tol = 0;
			} else {
				tol = defaultTol2(((DoubleMatrix2D) E), Bpad);
			}
			IJ.showStatus("Computing tolerance for preconditioner...done.");
		}

		final double[] one = new double[] { 1, 0 };
		if (maxE != 1.0) {
			((DComplexMatrix2D) matdata).assign(DComplexFunctions.div(new double[] { maxE, 0 }));
		}
		final int rows = E.rows();
		final int cols = E.columns();
		final DoubleMatrix2D Ef = (DoubleMatrix2D) E;
		double[] elem;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
					public void run() {
						double[] elem;
						if (maxE != 1.0) {
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									elem = ((DComplexMatrix2D) matdata).getQuick(r, c);
									if (Ef.getQuick(r, c) >= tol) {
										((DComplexMatrix2D) matdata).setQuick(r, c, DComplex.mult(DComplex.inv(elem), maxE));
									} else {
										((DComplexMatrix2D) matdata).setQuick(r, c, one);
									}
								}
							}
						} else {
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									elem = ((DComplexMatrix2D) matdata).getQuick(r, c);
									if (Ef.getQuick(r, c) >= tol) {
										((DComplexMatrix2D) matdata).setQuick(r, c, DComplex.inv(elem));
									} else {
										((DComplexMatrix2D) matdata).setQuick(r, c, one);
									}
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			if (maxE != 1.0) {
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						elem = ((DComplexMatrix2D) matdata).getQuick(i, j);
						if (Ef.getQuick(i, j) >= tol) {
							((DComplexMatrix2D) matdata).setQuick(i, j, DComplex.mult(DComplex.inv(elem), maxE));
						} else {
							((DComplexMatrix2D) matdata).setQuick(i, j, one);
						}
					}
				}
			} else {
				for (int i = 0; i < rows; i++) {
					for (int j = 0; j < cols; j++) {
						elem = ((DComplexMatrix2D) matdata).getQuick(i, j);
						if (Ef.getQuick(i, j) >= tol) {
							((DComplexMatrix2D) matdata).setQuick(i, j, DComplex.inv(elem));
						} else {
							((DComplexMatrix2D) matdata).setQuick(i, j, one);
						}
					}
				}
			}
		}
	}

	private double defaultTol2(DoubleMatrix2D E, DoubleMatrix2D B) {
		DoubleMatrix1D s = new DenseDoubleMatrix1D(E.size());
		System.arraycopy((double[]) E.elements(), 0, (double[]) s.elements(), 0, s.size());
		final double[] evalues = (double[]) s.elements();
		IntComparator compDec = new IntComparator() {
			public int compare(int a, int b) {
				if (evalues[a] != evalues[a] || evalues[b] != evalues[b])
					return compareNaN(evalues[a], evalues[b]); // swap NaNs to
				// the end
				return evalues[a] < evalues[b] ? 1 : (evalues[a] == evalues[b] ? 0 : -1);
			}
		};
		int[] indices = DoubleSorting.quickSort.sortIndex(s, compDec);
		s = s.viewSelection(indices);
		AbstractMatrix2D Bhat = B.getFft2();
		((DComplexMatrix2D) Bhat).assign(DComplexFunctions.abs);
		Bhat = ((DComplexMatrix2D) Bhat).getRealPart();
		DoubleMatrix1D bhat = new DenseDoubleMatrix1D(Bhat.size(), (double[]) ((DoubleMatrix2D) Bhat).elements(), 0, 1);
		bhat = bhat.viewSelection(indices);
		bhat.assign(DoubleFunctions.div((double) Math.sqrt(B.size())));
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

	private final int compareNaN(double a, double b) {
		if (a != a) {
			if (b != b)
				return 0; // NaN equals NaN
			else
				return 1; // e.g. NaN > 5
		}
		return -1; // e.g. 5 < NaN
	}
}
