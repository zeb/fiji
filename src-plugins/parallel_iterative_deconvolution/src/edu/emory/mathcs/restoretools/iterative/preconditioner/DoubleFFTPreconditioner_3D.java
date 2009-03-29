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
import cern.colt.matrix.tdcomplex.DComplexMatrix3D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix3D;
import cern.colt.matrix.tdouble.algo.DoubleSorting;
import cern.colt.matrix.AbstractMatrix3D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.jet.math.tdcomplex.DComplex;
import cern.jet.math.tdcomplex.DComplexFunctions;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.DoubleCommon_3D;
import edu.emory.mathcs.restoretools.iterative.PaddingType;
import edu.emory.mathcs.restoretools.iterative.psf.DoublePSFMatrix_3D;
import edu.emory.mathcs.restoretools.iterative.psf.PSFType;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * 3D preconditioner based on the Fast Fourier Transform.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleFFTPreconditioner_3D implements DoublePreconditioner_3D {
	private AbstractMatrix3D matdata;

	private double tol;

	private BoundaryType boundary;

	private int[] imSize;

	private int[] psfSize;

	private int[] padSize;

	/**
	 * Creates new instance of DoubleFFTPreconditioner_3D.
	 * 
	 * @param PSFMatrix
	 *            PSF matrix
	 * @param B
	 *            blurred image
	 * @param tol
	 *            tolerance
	 */
	public DoubleFFTPreconditioner_3D(DoublePSFMatrix_3D PSFMatrix, DoubleMatrix3D B, double tol) {
		this.tol = tol;
		this.boundary = PSFMatrix.getBoundary();
		this.imSize = new int[3];
		imSize[0] = B.slices();
		imSize[1] = B.rows();
		imSize[2] = B.columns();	
		if (PSFMatrix.getType() == PSFType.INVARIANT) {
			this.psfSize = PSFMatrix.getInvPsfSize();
			this.padSize = PSFMatrix.getInvPadSize();
		} else {
			this.psfSize = PSFMatrix.getPSF().getSize();
			int[] minimal = new int[3];
			minimal[0] = psfSize[0] + imSize[0];
			minimal[1] = psfSize[1] + imSize[1];
			minimal[2] = psfSize[2] + imSize[2];			
			switch (PSFMatrix.getResizing()) {
			case AUTO:
				int[] nextPowTwo = new int[3];
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
				if (!ConcurrencyUtils.isPowerOf2(minimal[2])) {
					nextPowTwo[2] = ConcurrencyUtils.nextPow2(minimal[2]);
				}
				else {
					nextPowTwo[2] = minimal[2];
				}
				if ((nextPowTwo[0] >= 1.5 * minimal[0]) || (nextPowTwo[1] >= 1.5 * minimal[1]) || (nextPowTwo[2] >= 1.5 * minimal[2])) {
					//use minimal padding
					psfSize[0] = minimal[0];
					psfSize[1] = minimal[1];
					psfSize[2] = minimal[2];
				} else {
					psfSize[0] = nextPowTwo[0];
					psfSize[1] = nextPowTwo[1];
					psfSize[2] = nextPowTwo[2];
				}
				break;
			case MINIMAL:
				psfSize[0] = minimal[0];
				psfSize[1] = minimal[1];
				psfSize[2] = minimal[2];
				break;
			case NEXT_POWER_OF_TWO:
				psfSize[0] = minimal[0];
				psfSize[1] = minimal[1];
				psfSize[2] = minimal[2];
				if (!ConcurrencyUtils.isPowerOf2(psfSize[0])) {
					psfSize[0] = ConcurrencyUtils.nextPow2(psfSize[0]);
				}
				if (!ConcurrencyUtils.isPowerOf2(psfSize[1])) {
					psfSize[1] = ConcurrencyUtils.nextPow2(psfSize[1]);
				}
				if (!ConcurrencyUtils.isPowerOf2(psfSize[2])) {
					psfSize[2] = ConcurrencyUtils.nextPow2(psfSize[2]);
				}
				break;
			}
			padSize = new int[3];
			if (imSize[0] < psfSize[0]) {
				padSize[0] = (psfSize[0] - imSize[0] + 1) / 2;
			}
			if (imSize[1] < psfSize[1]) {
				padSize[1] = (psfSize[1] - imSize[1] + 1) / 2;
			}
			if (imSize[2] < psfSize[2]) {
				padSize[2] = (psfSize[2] - imSize[2] + 1) / 2;
			}
		}
		constructMatrix(PSFMatrix.getPSF().getImage(), B, PSFMatrix.getPSF().getCenter());
	}

	public double getTolerance() {
		return tol;
	}

	public DoubleMatrix1D solve(DoubleMatrix1D b, boolean transpose) {
		DoubleMatrix3D B = b.reshape(imSize[0], imSize[1], imSize[2]);
		B = solve(B, transpose);
		return B.vectorize();
	}

	public DoubleMatrix3D solve(AbstractMatrix3D B, boolean transpose) {		
		switch (boundary) {
		case ZERO:
			B = DoubleCommon_3D.padZero_3D((DoubleMatrix3D) B, psfSize[0], psfSize[1], psfSize[2]);
			break;
		case PERIODIC:
			B = DoubleCommon_3D.padPeriodic_3D((DoubleMatrix3D) B, psfSize[0], psfSize[1], psfSize[2]);
			break;
		case REFLEXIVE:
			B = DoubleCommon_3D.padReflexive_3D((DoubleMatrix3D) B, psfSize[0], psfSize[1], psfSize[2]);
			break;
		}
		B = ((DoubleMatrix3D) B).getFft3();
		if (transpose) {
			((DComplexMatrix3D) B).assign((DComplexMatrix3D) matdata, DComplexFunctions.multConjSecond);
		} else {
			((DComplexMatrix3D) B).assign((DComplexMatrix3D) matdata, DComplexFunctions.mult);
		}
		((DComplexMatrix3D) B).ifft3(true);
		B = ((DComplexMatrix3D) B).getRealPart();
		B = ((DoubleMatrix3D) B).viewPart(padSize[0], padSize[1], padSize[2], imSize[0], imSize[1], imSize[2]);
		return ((DoubleMatrix3D) B);
	}

	private void constructMatrix(DoubleMatrix3D[][][] PSFs, DoubleMatrix3D B, int[][][][] center) {
		matdata = PSFs[0][0][0].like();
		int[] center1 = center[0][0][0];
		int slices = PSFs.length;
		int rows = PSFs[0].length;
		int columns = PSFs[0][0].length;
		int size = slices * rows * columns;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					((DoubleMatrix3D) matdata).assign(PSFs[s][r][c], DoubleFunctions.plus);
				}
			}
		}
		if (size != 1) {
			((DoubleMatrix3D) matdata).assign(DoubleFunctions.div(size));
		}
		switch (boundary) {
		case ZERO:
			B = DoubleCommon_3D.padZero_3D(B, psfSize[0], psfSize[1], psfSize[2]);
			break;
		case PERIODIC:
			B = DoubleCommon_3D.padPeriodic_3D(B, psfSize[0], psfSize[1], psfSize[2]);
			break;
		case REFLEXIVE:
			B = DoubleCommon_3D.padReflexive_3D(B, psfSize[0], psfSize[1], psfSize[2]);
			break;
		}
		precMatrixOnePsf(center1, B);
	}

	private void precMatrixOnePsf(int[] center, DoubleMatrix3D Bpad) {
		int[] padSize = new int[3];
		padSize[0] = Bpad.slices() - matdata.slices();
		padSize[1] = Bpad.rows() - matdata.rows();
		padSize[2] = Bpad.columns() - matdata.columns();
		if ((padSize[0] > 0) || (padSize[1] > 0) || (padSize[2] > 0)) {
			matdata = DoubleCommon_3D.padZero_3D((DoubleMatrix3D) matdata, padSize, PaddingType.POST);
		}
		matdata = DoubleCommon_3D.circShift_3D((DoubleMatrix3D) matdata, center);
		matdata = ((DoubleMatrix3D) matdata).getFft3();
		AbstractMatrix3D E = ((DComplexMatrix3D) matdata).copy();
		((DComplexMatrix3D) E).assign(DComplexFunctions.abs);
		E = ((DComplexMatrix3D) E).getRealPart();
		double[] maxAndLoc = ((DoubleMatrix3D) E).getMaxLocation();
		final double maxE = maxAndLoc[0];

		if (tol == -1) {
			IJ.showStatus("Computing tolerance for preconditioner...");
			double[] minAndLoc = ((DoubleMatrix3D) E).getMinLocation();
			double minE = minAndLoc[0];
			if (maxE / minE < 100) {
				tol = 0;
			} else {
				tol = defaultTol2(((DoubleMatrix3D) E), Bpad);
			}
			IJ.showStatus("Computing tolerance for preconditioner...done.");
		}

		final double[] one = new double[] { 1, 0 };
		if (maxE != 1.0) {
			((DComplexMatrix3D) matdata).assign(DComplexFunctions.div(new double[] { maxE, 0 }));
		}
		final int slices = E.slices();
		final int rows = E.rows();
		final int cols = E.columns();
		final DoubleMatrix3D Ef = (DoubleMatrix3D) E;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * rows * cols >= ConcurrencyUtils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
					public void run() {
						double[] elem;
						if (maxE != 1.0) {
							for (int s = startslice; s < stopslice; s++) {
								for (int r = 0; r < rows; r++) {
									for (int c = 0; c < cols; c++) {
										elem = ((DComplexMatrix3D) matdata).getQuick(s, r, c);
										if (Ef.getQuick(s, r, c) >= tol) {
											((DComplexMatrix3D) matdata).setQuick(s, r, c, DComplex.mult(DComplex.inv(elem), maxE));
										} else {
											((DComplexMatrix3D) matdata).setQuick(s, r, c, one);
										}
									}
								}
							}
						} else {
							for (int s = startslice; s < stopslice; s++) {
								for (int r = 0; r < rows; r++) {
									for (int c = 0; c < cols; c++) {
										elem = ((DComplexMatrix3D) matdata).getQuick(s, r, c);
										if (Ef.getQuick(s, r, c) >= tol) {
											((DComplexMatrix3D) matdata).setQuick(s, r, c, DComplex.inv(elem));
										} else {
											((DComplexMatrix3D) matdata).setQuick(s, r, c, one);
										}
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
			double[] elem;
			if (maxE != 1.0) {
				for (int s = 0; s < slices; s++) {
					for (int r = 0; r < rows; r++) {
						for (int c = 0; c < cols; c++) {
							elem = ((DComplexMatrix3D) matdata).getQuick(s, r, c);
							if (Ef.getQuick(s, r, c) >= tol) {
								((DComplexMatrix3D) matdata).setQuick(s, r, c, DComplex.mult(DComplex.inv(elem), maxE));
							} else {
								((DComplexMatrix3D) matdata).setQuick(s, r, c, one);
							}
						}
					}
				}
			} else {
				for (int s = 0; s < slices; s++) {
					for (int r = 0; r < rows; r++) {
						for (int c = 0; c < cols; c++) {
							elem = ((DComplexMatrix3D) matdata).getQuick(s, r, c);
							if (Ef.getQuick(s, r, c) >= tol) {
								((DComplexMatrix3D) matdata).setQuick(s, r, c, DComplex.inv(elem));
							} else {
								((DComplexMatrix3D) matdata).setQuick(s, r, c, one);
							}
						}
					}
				}
			}
		}
	}

	private double defaultTol2(DoubleMatrix3D E, DoubleMatrix3D B) {
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
		AbstractMatrix3D Bhat = B.getFft3();
		((DComplexMatrix3D) Bhat).assign(DComplexFunctions.abs);
		Bhat = ((DComplexMatrix3D) Bhat).getRealPart();
		DoubleMatrix1D bhat = new DenseDoubleMatrix1D(Bhat.size(), (double[]) ((DoubleMatrix3D) Bhat).elements(), 0, 1);
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
