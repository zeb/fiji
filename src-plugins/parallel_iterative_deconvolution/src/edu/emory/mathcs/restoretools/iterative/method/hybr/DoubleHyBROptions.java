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

/**
 * Options for HyBR.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleHyBROptions {

	public InSolvType inSolv;

	public RegMethodType regMethod;

	public double regPar;

	public double omega;

	public boolean reorth;

	public int begReg;

	public double flatTol;

	/**
	 * Creates new instance of DoubleHyBROptions.
	 * 
	 * @param inSolv
	 *            type of solver for the inner problem
	 * @param regMethod
	 *            type of method for finding a regularization parameter
	 * @param regPar
	 *            regularization parameter
	 * @param omega
	 *            omega parameter
	 * @param reorth
	 *            reorthogonalize Lanczos subspaces
	 * @param begReg
	 *            begin regularization after this iteration
	 * @param flatTol
	 *            tolerance for detecting flatness in the GCV curve as a
	 *            stopping criteria
	 */
	public DoubleHyBROptions(InSolvType inSolv, RegMethodType regMethod, double regPar, double omega, boolean reorth, int begReg, double flatTol) {
		this.inSolv = inSolv;
		this.regMethod = regMethod;
		this.regPar = regPar;
		this.omega = omega;
		this.reorth = reorth;
		this.begReg = begReg;
		this.flatTol = flatTol;
	}
}
