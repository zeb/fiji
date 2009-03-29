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

import cern.colt.matrix.tfloat.FloatFactory2D;
import cern.colt.matrix.tfloat.FloatMatrix1D;
import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import cern.colt.matrix.tfloat.algo.FloatAlgebra;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.restoretools.iterative.method.hybr.FloatHyBROptions;
import edu.emory.mathcs.restoretools.iterative.psf.FloatPSFMatrix_2D;

/**
 * Lanczos bidiagonalization without preconditioner.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */

public class FloatSimpleLBD_2D implements FloatLBD_2D {
	private final FloatAlgebra alg = FloatAlgebra.DEFAULT;

	private final FloatFactory2D factory = FloatFactory2D.dense;

	private final FloatMatrix2D alphaBeta = new DenseFloatMatrix2D(2, 1);

	/**
	 * Creates new instance of FloatSimpleLBD_2D
	 * 
	 */
	public FloatSimpleLBD_2D() {
	}

	public FloatMatrix2D[] perform(FloatPSFMatrix_2D A, FloatMatrix2D U, FloatMatrix2D B, FloatMatrix2D V, FloatHyBROptions options) {
		FloatMatrix2D[] result = new FloatMatrix2D[3];
		int k = U.columns();
		FloatMatrix1D u, v, column;
		if (k == 1) {
			v = A.times(U.viewColumn(k - 1), true);
		} else {
			v = A.times(U.viewColumn(k - 1), true);
			column = V.viewColumn(k - 2).copy();
			v.assign(column.assign(FloatFunctions.mult(B.getQuick(k - 1, k - 2))), FloatFunctions.minus);
			if (options.reorth) {
				for (int j = 0; j < k - 1; j++) {
					column = V.viewColumn(j).copy();
					v.assign(column.assign(FloatFunctions.mult(column.zDotProduct(v))), FloatFunctions.minus);
				}
			}
		}
		float alpha = alg.norm2(v);
		v.assign(FloatFunctions.div(alpha));
		u = A.times(v, false);
		column = U.viewColumn(k - 1).copy();
		u.assign(column.assign(FloatFunctions.mult(alpha)), FloatFunctions.minus);
		if (options.reorth) {
			for (int j = 0; j < k; j++) {
				column = U.viewColumn(j).copy();
				u.assign(column.assign(FloatFunctions.mult(column.zDotProduct(u))), FloatFunctions.minus);
			}
		}
		float beta = alg.norm2(u);
		alphaBeta.setQuick(0, 0, alpha);
		alphaBeta.setQuick(1, 0, beta);
		u.assign(FloatFunctions.div(beta));
		result[0] = factory.appendColumn(U, u);
		if (V == null) {
			result[2] = new DenseFloatMatrix2D(v.size(), 1);
			result[2].assign((float[]) v.elements());
		} else {
			result[2] = factory.appendColumn(V, v);
		}
		if (B == null) {
			result[1] = new DenseFloatMatrix2D(2, 1);
			result[1].assign(alphaBeta);
		} else {
			result[1] = factory.composeBidiagonal(B, alphaBeta);
		}
		return result;
	}
}
