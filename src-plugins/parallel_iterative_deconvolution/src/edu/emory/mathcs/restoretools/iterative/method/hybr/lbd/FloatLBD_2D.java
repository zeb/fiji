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

import cern.colt.matrix.tfloat.FloatMatrix2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.FloatHyBROptions;
import edu.emory.mathcs.restoretools.iterative.psf.FloatPSFMatrix_2D;

/**
 * Interface for Lanczos bidiagonalization.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public interface FloatLBD_2D {
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
	public FloatMatrix2D[] perform(FloatPSFMatrix_2D A, FloatMatrix2D U, FloatMatrix2D B, FloatMatrix2D V, FloatHyBROptions options);
}
