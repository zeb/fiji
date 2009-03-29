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

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.DoubleHyBROptions;
import edu.emory.mathcs.restoretools.iterative.psf.DoublePSFMatrix_2D;

/**
 * Interface for Lanczos bidiagonalization.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public interface DoubleLBD_2D {
	/**
	 * Perform one step of Lanczos bidiagonalization
	 * 
	 * @param A
	 *            matrix
	 * @param U
	 *            accumulation of vectors
	 * @param B
	 *            bidiagonal matrix
	 * @param V
	 *            accumulation of vectors
	 * @param options
	 *            HyBR options
	 * @return updated U, B and V (in this order)
	 */
	public DoubleMatrix2D[] perform(DoublePSFMatrix_2D A, DoubleMatrix2D U, DoubleMatrix2D B, DoubleMatrix2D V, DoubleHyBROptions options);
}
