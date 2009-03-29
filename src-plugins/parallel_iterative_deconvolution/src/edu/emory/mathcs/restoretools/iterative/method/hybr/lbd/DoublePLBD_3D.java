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

package edu.emory.mathcs.restoretools.iterative.method.hybr.lbd;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DoubleAlgebra;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.restoretools.iterative.method.hybr.DoubleHyBROptions;
import edu.emory.mathcs.restoretools.iterative.preconditioner.DoublePreconditioner_3D;
import edu.emory.mathcs.restoretools.iterative.psf.DoublePSFMatrix_3D;

/**
 * Lanczos bidiagonalization with preconditioner.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoublePLBD_3D implements DoubleLBD_3D {

	private final DoubleAlgebra alg = DoubleAlgebra.DEFAULT;

	private final DoubleFactory2D factory = DoubleFactory2D.dense;

	private final DoubleMatrix2D alphaBeta = new DenseDoubleMatrix2D(2, 1);

	private final DoublePreconditioner_3D P;

	/**
	 * Creates new instance of DoublePLBD_3D
	 * 
	 * @param P
	 *            preconditioner
	 */
	public DoublePLBD_3D(DoublePreconditioner_3D P) {
		this.P = P;
	}

	public DoubleMatrix2D[] perform(DoublePSFMatrix_3D A, DoubleMatrix2D U, DoubleMatrix2D B, DoubleMatrix2D V, DoubleHyBROptions options) {
		DoubleMatrix2D[] result = new DoubleMatrix2D[3];
		int k = U.columns();
		DoubleMatrix1D u, v, column;
		if (k == 1) {
			column = U.viewColumn(k - 1).copy();
			column = P.solve(column, true);
			v = A.times(column, true);
		} else {
			column = U.viewColumn(k - 1).copy();
			column = P.solve(column, true);
			v = A.times(column, true);
			column = V.viewColumn(k - 2).copy();
			v.assign(column.assign(DoubleFunctions.mult(B.getQuick(k - 1, k - 2))), DoubleFunctions.minus);
			if (options.reorth) {
				for (int j = 0; j < k - 1; j++) {
					column = V.viewColumn(j).copy();
					v.assign(column.assign(DoubleFunctions.mult(column.zDotProduct(v))), DoubleFunctions.minus);
				}
			}
		}
		double alpha = alg.norm2(v);
		v.assign(DoubleFunctions.div(alpha));
		column = A.times(v, false);
		u = P.solve(column, false);
		column = U.viewColumn(k - 1).copy();
		u.assign(column.assign(DoubleFunctions.mult(alpha)), DoubleFunctions.minus);
		if (options.reorth) {
			for (int j = 0; j < k; j++) {
				column = U.viewColumn(j).copy();
				u.assign(column.assign(DoubleFunctions.mult(column.zDotProduct(u))), DoubleFunctions.minus);
			}
		}
		double beta = alg.norm2(u);
		alphaBeta.setQuick(0, 0, alpha);
		alphaBeta.setQuick(1, 0, beta);
		u.assign(DoubleFunctions.div(beta));
		result[0] = factory.appendColumn(U, u);
		if (V == null) {
			result[2] = new DenseDoubleMatrix2D(v.size(), 1);
			result[2].assign((double[]) v.elements());
		} else {
			result[2] = factory.appendColumn(V, v);
		}
		if (B == null) {
			result[1] = new DenseDoubleMatrix2D(2, 1);
			result[1].assign(alphaBeta);
		} else {
			result[1] = factory.composeBidiagonal(B, alphaBeta);
		}
		return result;
	}
}
