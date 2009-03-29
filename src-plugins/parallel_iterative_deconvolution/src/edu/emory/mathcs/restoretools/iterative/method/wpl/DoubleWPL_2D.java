/*  License:
 Copyright (c) 2005, OptiNav, Inc.
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 Neither the name of OptiNav, Inc. nor the names of its contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.emory.mathcs.restoretools.iterative.method.wpl;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.Timer;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.DoubleCommon_2D;
import edu.emory.mathcs.restoretools.iterative.OutputType;
import edu.emory.mathcs.restoretools.iterative.PaddingType;
import edu.emory.mathcs.restoretools.iterative.ResizingType;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Wiener Filter Preconditioned Landweber. This is a nonnegatively constrained method.
 * 
 * @author Bob Dougherty
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DoubleWPL_2D {
	private DoubleWPLOptions options;

	private DoubleMatrix2D PSF;

	private DoubleMatrix2D B;

	private java.awt.image.ColorModel cmY;

	private int columns;

	private int rows;

	private int bColumns;

	private int bRows;

	private int maxIts;

	private double minB = 0;

	private double minPSF = 0;

	private double sum;

	private double scalePSF = 1;

	private double[][] g;

	private boolean showIteration;
	
	private OutputType output;
	

	/**
	 * Creates new instance of DoubleWPL_2D
	 * 
	 * @param imB
	 *            blurred image
	 * @param imPSF
	 *            PSF
	 * @param boundary
	 *            boundary conditions
	 * @param resizing
	 *            type of resizing
	 * @param output
	 *            type of an output
	 * @param maxIts
	 *            maximal number of iterations
	 * @param options
	 *            options for WPL
	 * @param showIteration
	 *            if true then the restored image is shown after each iteration
	 */
	public DoubleWPL_2D(ImagePlus imB, ImagePlus imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, int maxIts, DoubleWPLOptions options, boolean showIteration) {
		IJ.showStatus("WPL initialization...");
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
		bColumns = ipB.getWidth();
		bRows = ipB.getHeight();
		B = new DenseDoubleMatrix2D(bRows, bColumns);
		DoubleCommon_2D.assignPixelsToMatrix_2D(B, ipB);

		ImageProcessor ipPSF = imPSF.getProcessor();
		int psfColumns = ipPSF.getWidth();
		int psfRows = ipPSF.getHeight();
		PSF = new DenseDoubleMatrix2D(psfRows, psfColumns);
		DoubleCommon_2D.assignPixelsToMatrix_2D(PSF, ipPSF);

		this.maxIts = maxIts;
		if (options == null) {
			this.options = new DoubleWPLOptions(0, 1.0, 1.0, 1.0, false, false, true, 0.01, false, true);
		} else {
			this.options = options;
		}
		this.showIteration = showIteration;
		if (this.options.dB) {
			minB = unDB(B);
			minPSF = unDB(PSF);
		}
		sum = PSF.zSum();
		if ((sum != 0) && this.options.normalize)
			scalePSF /= sum;

		columns = expandedSize(psfColumns, bColumns, resizing);
		rows = expandedSize(psfRows, bRows, resizing);
		if ((psfColumns > columns) || (psfRows > rows)) {
			throw new IllegalArgumentException("PSF cannot be largest that the image.");
		}
		g = gaussianWeights(rows, columns, this.options.filterX, this.options.filterY);
		switch (boundary) {
		case PERIODIC:
			B = DoubleCommon_2D.padPeriodic_2D(B, rows, columns);
			break;
		case REFLEXIVE:
			B = DoubleCommon_2D.padReflexive_2D(B, rows, columns);
			break;
		case ZERO:
			B = DoubleCommon_2D.padZero_2D(B, rows, columns);
			break;
		}
		double[] maxLoc = PSF.getMaxLocation();
		int[] padSize = new int[2];
		padSize[0] = rows - psfRows;
		padSize[1] = columns - psfColumns;
		PSF = DoubleCommon_2D.padZero_2D(PSF, padSize, PaddingType.POST);
		PSF = DoubleCommon_2D.circShift_2D(PSF, new int[] {(int)maxLoc[1], (int)maxLoc[2]});
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
		PSF.dht2();
		DoubleMatrix2D X;
		DoubleMatrix2D AX = B.like();
		if (options.antiRing) {
			IJ.showStatus("WPL: performing anti-ringing step.");
			X = B.copy();
			X.dht2();
			convolveFD(rows, columns, PSF, X, AX);
			AX.idht2(true);
			copyDataAverage(bRows, bColumns, rows, columns, sum, B, AX, B);
		}
		if (options.gamma > 0.0001) {
			IJ.showStatus("WPL: Winner filter");
			double magMax = findMagMax(PSF);
			B.dht2();
			X = PSF.copy();
			deconvolveFD(options.gamma, magMax, rows, columns, X, X, PSF);
			AX = B.copy();
			deconvolveFD(options.gamma, magMax, rows, columns, AX, X, B);
			B.idht2(true);
		}

		int rOff = (rows - bRows + 1) / 2;
		int cOff = (columns - bColumns + 1) / 2;

		PSF.idht2(true);
		double aSum = PSF.aggregate(DoubleFunctions.plus, DoubleFunctions.abs);
		if (scalePSF != 1) {
			B.assign(DoubleFunctions.div(scalePSF));
		}
		PSF.dht2();
		X = B.copy();
		ImagePlus imX = null;
		FloatProcessor ip = new FloatProcessor(bColumns, bRows);
		if (showIteration) {
			DoubleCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY);
			imX = new ImagePlus("(deblurred)", ip);
			imX.show();
		}
		double oldPercentChange = Double.MAX_VALUE;
		for (int iter = 0; iter < maxIts; iter++) {
			IJ.showStatus("WPL iteration: " + (iter + 1) + "/" + maxIts);
			X.dht2();
			gaussianFilter(X, g);
			convolveFD(rows, columns, PSF, X, AX);
			AX.idht2(true);
			X.idht2(true);
			double meanDelta = meanDelta(B, AX, X, aSum);
			if (showIteration) {
				if (threshold == -1.0) {
					DoubleCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY);
				} else {
					DoubleCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY, threshold);
				}
				imX.updateAndDraw();
			}
			double sumPixels = energySum(X, bRows, bColumns, cOff, rOff);
			double percentChange = 100 * meanDelta / sumPixels;
			if (options.logMean)
				IJ.write(Double.toString(percentChange));
			if ((oldPercentChange - percentChange) < options.changeThreshPercent) {
				if (options.logMean)
					IJ.write("Automatically terminated after " + (iter + 1) + " iterations.");
				break;
			}
			if ((oldPercentChange < percentChange) && options.detectDivergence) {
				if (options.logMean)
					IJ.write("Automatically terminated due to divergence " + (iter + 1) + " iterations.");
				break;
			}
			oldPercentChange = percentChange;
		}
		X.dht2();
		gaussianFilterWithScaling(X, g, aSum);
		X.idht2(true);
		if (options.dB) {
			toDB(PSF, minPSF);
			toDB(B, minB);
			toDB(X, -90);
		}
		if (!showIteration) {
			if (threshold == -1.0) {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY);
			} else {
				DoubleCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", ip);
		}
		DoubleCommon_2D.convertImage(imX, output);
		return imX;
	}

	private static void convolveFD(final int rows, final int columns, DoubleMatrix2D H1, DoubleMatrix2D H2, DoubleMatrix2D Result) {
		final double[] h1 = (double[]) H1.elements();
		final double[] h2 = (double[]) H2.elements();
		final double[] result = (double[]) Result.elements();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (columns * rows >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
						int cC, rC, idx1, idx2;
						double h2e, h2o;
						for (int r = startrow; r < stoprow; r++) {
							rC = (rows - r) % rows;
							for (int c = 0; c < columns; c ++) {
								cC = (columns - c) % columns;
								idx1 = c + columns * r;
								idx2 = cC + columns * rC;
								h2e = (h2[idx1] + h2[idx2]) / 2;
								h2o = (h2[idx1] - h2[idx2]) / 2;
								result[idx1] = (double) (h1[idx1] * h2e + h1[idx2] * h2o);
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
			int cC, rC, idx1, idx2;
			double h2e, h2o;
			for (int r = 0; r < rows; r++) {
				rC = (rows - r) % rows;
				for (int c = 0; c < columns; c ++) {
					cC = (columns - c) % columns;
					idx1 = c + columns * r;
					idx2 = cC + columns * rC;
					h2e = (h2[idx1] + h2[idx2]) / 2;
					h2o = (h2[idx1] - h2[idx2]) / 2;
					result[idx1] = (double) (h1[idx1] * h2e + h1[idx2] * h2o);
				}
			}
		}
	}

	private static void copyDataAverage(final int rows, final int columns, final int rowsE, final int columnsE, final double sum, DoubleMatrix2D DataIn, DoubleMatrix2D DataOut, DoubleMatrix2D Result) {
		final double[] dataIn = (double[]) DataIn.elements();
		final double[] dataOut = (double[]) DataOut.elements();
		final double[] result = (double[]) Result.elements();

		final int rOff = (rowsE - rows + 1) / 2;
		final int cOff = (columnsE - columns + 1) / 2;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (columnsE * rowsE >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rowsE / np;
			for (int j = 0; j < np; j++) {
				final int startrow = -rOff + j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rowsE - rOff;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {

					public void run() {
						int cOut, rOut, idx;
						double alphaI, alphaJ;
						double a;
						for (int r = startrow; r < stoprow; r++) {
							rOut = r + rOff;
							if (r < 0) {
								alphaJ = -r / ((double) rOff);
							} else if (r > (rows - 1)) {
								alphaJ = (r - rows) / ((double) rOff);
							} else {
								alphaJ = 0;
							}
							for (int c = -cOff; c < columnsE - cOff; c++) {
								cOut = c + cOff;
								if (c < 0) {
									alphaI = -c / ((double) cOff);
								} else if (c > (columns - 1)) {
									alphaI = (c - columns) / ((double) cOff);
								} else {
									alphaI = 0;
								}
								a = alphaJ;
								if (alphaI > a)
									a = alphaI;
								idx = cOut + columnsE * rOut;
								result[idx] = (1 - a) * dataIn[idx] + a * dataOut[idx] / sum;
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
			int cOut, rOut, idx;
			double alphaI, alphaJ;
			double a;
			for (int r = -rOff; r < rowsE - rOff; r++) {
				rOut = r + rOff;
				if (r < 0) {
					alphaJ = -r / ((double) rOff);
				} else if (r > (rows - 1)) {
					alphaJ = (r - rows) / ((double) rOff);
				} else {
					alphaJ = 0;
				}
				for (int c = -cOff; c < columnsE - cOff; c++) {
					cOut = c + cOff;
					if (c < 0) {
						alphaI = -c / ((double) cOff);
					} else if (c > (columns - 1)) {
						alphaI = (c - columns) / ((double) cOff);
					} else {
						alphaI = 0;
					}
					a = alphaJ;
					if (alphaI > a)
						a = alphaI;
					idx = cOut + columnsE * rOut;
					result[idx] = (1 - a) * dataIn[idx] + a * dataOut[idx] / sum;
				}
			}
		}
	}

	private static void deconvolveFD(final double gamma, final double magMax, final int rows, final int columns, DoubleMatrix2D H1, DoubleMatrix2D H2, DoubleMatrix2D Result) {
		final double gammaScaled = gamma * magMax;
		final double[] h1 = (double[]) H1.elements();
		final double[] h2 = (double[]) H2.elements();
		final double[] result = (double[]) Result.elements();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (columns * rows >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
						int cC, rC, idx1, idx2;
						double mag, h2e, h2o;
						for (int r = startrow; r < stoprow; r++) {
							rC = (rows - r) % rows;
							for (int c = 0; c < columns; c++) {
								cC = (columns - c) % columns;
								idx1 = c + columns * r;
								idx2 = cC + columns * rC;
								h2e = (h2[idx1] + h2[idx2]) / 2;
								h2o = (h2[idx1] - h2[idx2]) / 2;
								mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
								double tmp = h1[idx1] * h2e - h1[idx2] * h2o;
								result[idx1] = (tmp / (mag + gammaScaled));
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
			int cC, rC, idx1, idx2;
			double mag, h2e, h2o;
			for (int r = 0; r < rows; r++) {
				rC = (rows - r) % rows;
				for (int c = 0; c < columns; c++) {
					cC = (columns - c) % columns;
					idx1 = c + columns * r;
					idx2 = cC + columns * rC;
					h2e = (h2[idx1] + h2[idx2]) / 2;
					h2o = (h2[idx1] - h2[idx2]) / 2;
					mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
					double tmp = h1[idx1] * h2e - h1[idx2] * h2o;
					result[idx1] = (tmp / (mag + gammaScaled));
				}
			}
		}
	}

	private static double energySum(DoubleMatrix2D X, final int rows, final int columns, final int cOff, final int rOff) {
		double sumPixels = 0;
		final int rowStride = X.rowStride();
		final double[] elemsX = (double[]) X.elements();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (rows * columns >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Double>() {
					public Double call() throws Exception {
						double sumPixels = 0;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = 0; c < columns; c++) {
								sumPixels += elemsX[c + cOff + rowStride * (r + rOff)];
							}
						}
						return sumPixels;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Double) futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (int j = 0; j < np; j++) {
				sumPixels += results[j];
			}
		} else {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					sumPixels += elemsX[c + cOff + rowStride * (r + rOff)];
				}
			}
		}
		return sumPixels;
	}

	private static int expandedSize(int psfSize, int bSize, ResizingType resizing) {
		int result = 0;
		int minimal = psfSize + bSize;
		switch (resizing) {
		case AUTO:
			int nextPowTwo;
			if (!ConcurrencyUtils.isPowerOf2(minimal)) {
				nextPowTwo = ConcurrencyUtils.nextPow2(minimal);
			}
			else {
				nextPowTwo = minimal;
			}
			if (nextPowTwo >= 1.5 * minimal) {
				//use minimal padding
				result = minimal;
			} else {
				result = nextPowTwo;
			}
			break;
		case MINIMAL:
			result = minimal;
			break;
		case NEXT_POWER_OF_TWO:
			result = minimal;
			if (!ConcurrencyUtils.isPowerOf2(result)) {
				result = ConcurrencyUtils.nextPow2(result);
			}
			break;
		}
		if (result < 4) {
			result = 4;
		}
		return result;
	}

	private static double findMagMax(DoubleMatrix2D H2) {
		final double[] h2 = (double[]) H2.elements();
		double magMax = 0;
		final int rows = H2.rows();
		final int columns = H2.columns();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (rows * columns >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Double>() {
					public Double call() throws Exception {
						int cC, rC, idx1, idx2;
						double magMax = 0;
						double mag;
						for (int r = startrow; r < stoprow; r++) {
							rC = (rows - r) % rows;
							for (int c = 0; c < columns; c++) {
								cC = (columns - c) % columns;
								idx1 = c + columns * r;
								idx2 = cC + columns * rC;
								mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
								if (mag > magMax)
									magMax = mag;
							}
						}
						return magMax;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Double) futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			magMax = results[0];
			for (int j = 1; j < np; j++) {
				if (results[j] > magMax)
					magMax = results[j];
			}
		} else {
			int cC, rC, idx1, idx2;
			double mag;
			for (int r = 0; r < rows; r++) {
				rC = (rows - r) % rows;
				for (int c = 0; c < columns; c++) {
					cC = (columns - c) % columns;
					idx1 = c + columns * r;
					idx2 = cC + columns * rC;
					mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
					if (mag > magMax)
						magMax = mag;
				}
			}
		}
		return magMax;
	}

	private static void gaussianFilter(DoubleMatrix2D X, final double[][] weights) {
		final double[] elems = (double[]) X.elements();
		final int rows = X.rows();
		final int columns = X.columns();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (columns * rows >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
						int idx = startrow * columns;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								elems[i++] *= weights[1][r] * weights[0][c];
							}
							idx += columns;
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
			int idx = 0;
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					elems[i++] *= weights[1][r] * weights[0][c];
				}
				idx += columns;
			}
		}
	}

	private static void gaussianFilterWithScaling(DoubleMatrix2D X, final double[][] weights, final double scale) {
		final double[] elems = (double[]) X.elements();
		final int rows = X.rows();
		final int columns = X.columns();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (columns * rows >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
						int idx = startrow * columns;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								elems[i++] *= weights[1][r] * weights[0][c] / scale;
							}
							idx += columns;
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
			int idx = 0;
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					elems[i++] *= weights[1][r] * weights[0][c] / scale;
				}
				idx += columns;
			}
		}
	}

	private static double[][] gaussianWeights(final int rows, final int columns, final double filterX, final double filterY) {
		final double[][] weights = new double[2][];
		weights[0] = new double[columns];
		weights[1] = new double[rows];
		final double cc = columns / (filterX + 0.000001);
		final double rc = rows / (filterY + 0.000001);
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (Math.max(columns, rows) >= ConcurrencyUtils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int kcol = columns / np;
			int krow = rows / np;
			for (int j = 0; j < np; j++) {
				final int startcol = j * kcol;
				final int stopcol;
				final int startrow = j * krow;
				final int stoprow;
				if (j == np - 1) {
					stopcol = columns;
					stoprow = rows;
				} else {
					stopcol = startcol + kcol;
					stoprow = startrow + krow;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {

					public void run() {
						for (int c = startcol; c < stopcol; c++) {
							int cShifted = c;
							if (cShifted > columns / 2)
								cShifted = columns - cShifted;
							double tmp = (cShifted / cc);
							weights[0][c] = (double) Math.exp(-tmp * tmp);
						}
						for (int r = startrow; r < stoprow; r++) {
							int rShifted = r;
							if (rShifted > rows / 2)
								rShifted = rows - rShifted;
							double tmp = (rShifted / rc);
							weights[1][r] = (double) Math.exp(-tmp * tmp);
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
			for (int c = 0; c < columns; c++) {
				int cShifted = c;
				if (cShifted > columns / 2)
					cShifted = columns - cShifted;
				double tmp = (cShifted / cc);
				weights[0][c] = (double) Math.exp(-tmp * tmp);
			}
			for (int r = 0; r < rows; r++) {
				int rShifted = r;
				if (rShifted > rows / 2)
					rShifted = rows - rShifted;
				double tmp = (rShifted / rc);
				weights[1][r] = (double) Math.exp(-tmp * tmp);
			}
		}
		return weights;
	}

	public static void main(String[] args) {
		ConcurrencyUtils.setNumberOfProcessors(2);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage("D:\\Research\\Images\\io1024-blur.png");
		ImagePlus psfImage = o.openImage("D:\\Research\\Images\\io1024-psf.png");
		Timer t = new Timer();
		t.reset().start();
		DoubleWPL_2D damas = new DoubleWPL_2D(blurImage, psfImage, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE,15, null, true);
		ImagePlus X = damas.deblur(-1);
		t.stop();
		t.display();
		X.show();
	}

	private static double meanDelta(DoubleMatrix2D B, DoubleMatrix2D AX, DoubleMatrix2D X, final double aSum) {
		double meanDelta = 0;
		final double[] elemsB = (double[]) B.elements();
		final double[] elemsAX = (double[]) AX.elements();
		final double[] elemsX = (double[]) X.elements();
		final int size = B.size();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Double>() {
					public Double call() throws Exception {
						double meanDelta = 0;
						double delta;
						for (int i = startidx; i < stopidx; i++) {
							delta = (elemsB[i] - elemsAX[i] / aSum);
							elemsX[i] += delta;
							if (elemsX[i] < 0) {
								elemsX[i] = 0;
							} else {
								meanDelta += Math.abs(delta);
							}
						}
						return meanDelta;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Double) futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (int j = 0; j < np; j++) {
				meanDelta += results[j];
			}
		} else {
			double delta;
			for (int i = 0; i < size; i++) {
				delta = (elemsB[i] - elemsAX[i] / aSum);
				elemsX[i] += delta;
				if (elemsX[i] < 0) {
					elemsX[i] = 0;
				} else {
					meanDelta += Math.abs(delta);
				}
			}
		}
		return meanDelta;
	}

	private static void toDB(DoubleMatrix2D X, final double minDB) {
		final double[] x = (double[]) X.elements();
		final double SCALE = 10 / Math.log(10);
		final double minVal = Math.exp(minDB / SCALE);
		int size = X.size();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
					public void run() {
						for (int i = startidx; i < stopidx; i++) {
							if (x[i] > minVal)
								x[i] = SCALE * Math.log(x[i]);
							else
								x[i] = minDB;
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
			for (int i = 0; i < size; i++) {
				if (x[i] > minVal)
					x[i] = SCALE * Math.log(x[i]);
				else
					x[i] = minDB;
			}
		}
	}

	private static double unDB(DoubleMatrix2D X) {
		final double[] x = (double[]) X.elements();
		final double SCALE = 10 / Math.log(10);
		final int size = X.size();
		double min = Double.MAX_VALUE;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Double>() {
					public Double call() throws Exception {
						double min = Double.MAX_VALUE;
						for (int i = startidx; i < stopidx; i++) {
							if (x[i] < min)
								min = x[i];
							x[i] = (double) Math.exp(x[i] / SCALE);
						}
						return min;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Double) futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			min = results[0];
			for (int j = 1; j < np; j++) {
				if (results[j] < min)
					min = results[j];
			}
		} else {
			for (int i = 0; i < size; i++) {
				if (x[i] < min)
					min = x[i];
				x[i] = (double) Math.exp(x[i] / SCALE);
			}
		}
		return min;
	}
}