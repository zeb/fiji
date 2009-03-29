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
import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.FloatCommon_2D;
import edu.emory.mathcs.restoretools.iterative.OutputType;
import edu.emory.mathcs.restoretools.iterative.PaddingType;
import edu.emory.mathcs.restoretools.iterative.ResizingType;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 *  Wiener Filter Preconditioned Landweber. This is a nonnegatively constrained method.
 * 
 * @author Bob Dougherty
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatWPL_2D {
	private FloatWPLOptions options;

	private FloatMatrix2D PSF;

	private FloatMatrix2D B;

	private java.awt.image.ColorModel cmY;

	private int columns;

	private int rows;

	private int bColumns;

	private int bRows;

	private int maxIts;

	private float minB = 0;

	private float minPSF = 0;

	private float sum;

	private float scalePSF = 1;

	private float[][] g;

	private boolean showIteration;
	
	private OutputType output;
	

	/**
	 * Creates new instance of FloatWPL_2D
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
	public FloatWPL_2D(ImagePlus imB, ImagePlus imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, int maxIts, FloatWPLOptions options, boolean showIteration) {
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
		B = new DenseFloatMatrix2D(bRows, bColumns);
		FloatCommon_2D.assignPixelsToMatrix_2D(B, ipB);

		ImageProcessor ipPSF = imPSF.getProcessor();
		int psfColumns = ipPSF.getWidth();
		int psfRows = ipPSF.getHeight();
		PSF = new DenseFloatMatrix2D(psfRows, psfColumns);
		FloatCommon_2D.assignPixelsToMatrix_2D(PSF, ipPSF);

		this.maxIts = maxIts;
		if (options == null) {
			this.options = new FloatWPLOptions(0, 1.0f, 1.0f, 1.0f, false, false, true, 0.01f, false, true);
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
			B = FloatCommon_2D.padPeriodic_2D(B, rows, columns);
			break;
		case REFLEXIVE:
			B = FloatCommon_2D.padReflexive_2D(B, rows, columns);
			break;
		case ZERO:
			B = FloatCommon_2D.padZero_2D(B, rows, columns);
			break;
		}
		float[] maxLoc = PSF.getMaxLocation();
		int[] padSize = new int[2];
		padSize[0] = rows - psfRows;
		padSize[1] = columns - psfColumns;
		PSF = FloatCommon_2D.padZero_2D(PSF, padSize, PaddingType.POST);
		PSF = FloatCommon_2D.circShift_2D(PSF, new int[] {(int)maxLoc[1], (int)maxLoc[2]});
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
		PSF.dht2();
		FloatMatrix2D X;
		FloatMatrix2D AX = B.like();
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
			float magMax = findMagMax(PSF);
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
		float aSum = PSF.aggregate(FloatFunctions.plus, FloatFunctions.abs);
		if (scalePSF != 1) {
			B.assign(FloatFunctions.div(scalePSF));
		}
		PSF.dht2();
		X = B.copy();
		ImagePlus imX = null;
		FloatProcessor ip = new FloatProcessor(bColumns, bRows);
		if (showIteration) {
			FloatCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY);
			imX = new ImagePlus("(deblurred)", ip);
			imX.show();
		}
		float oldPercentChange = Float.MAX_VALUE;
		for (int iter = 0; iter < maxIts; iter++) {
			IJ.showStatus("WPL iteration: " + (iter + 1) + "/" + maxIts);
			X.dht2();
			gaussianFilter(X, g);
			convolveFD(rows, columns, PSF, X, AX);
			AX.idht2(true);
			X.idht2(true);
			float meanDelta = meanDelta(B, AX, X, aSum);
			if (showIteration) {
				if (threshold == -1.0) {
					FloatCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY);
				} else {
					FloatCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY, threshold);
				}
				imX.updateAndDraw();
			}
			float sumPixels = energySum(X, bRows, bColumns, cOff, rOff);
			float percentChange = 100 * meanDelta / sumPixels;
			if (options.logMean)
				IJ.write(Float.toString(percentChange));
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
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY);
			} else {
				FloatCommon_2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", ip);
		}
		FloatCommon_2D.convertImage(imX, output);
		return imX;
	}

	private static void convolveFD(final int rows, final int columns, FloatMatrix2D H1, FloatMatrix2D H2, FloatMatrix2D Result) {
		final float[] h1 = (float[]) H1.elements();
		final float[] h2 = (float[]) H2.elements();
		final float[] result = (float[]) Result.elements();
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
						float h2e, h2o;
						for (int r = startrow; r < stoprow; r++) {
							rC = (rows - r) % rows;
							for (int c = 0; c < columns; c ++) {
								cC = (columns - c) % columns;
								idx1 = c + columns * r;
								idx2 = cC + columns * rC;
								h2e = (h2[idx1] + h2[idx2]) / 2;
								h2o = (h2[idx1] - h2[idx2]) / 2;
								result[idx1] = (float) (h1[idx1] * h2e + h1[idx2] * h2o);
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
			float h2e, h2o;
			for (int r = 0; r < rows; r++) {
				rC = (rows - r) % rows;
				for (int c = 0; c < columns; c ++) {
					cC = (columns - c) % columns;
					idx1 = c + columns * r;
					idx2 = cC + columns * rC;
					h2e = (h2[idx1] + h2[idx2]) / 2;
					h2o = (h2[idx1] - h2[idx2]) / 2;
					result[idx1] = (float) (h1[idx1] * h2e + h1[idx2] * h2o);
				}
			}
		}
	}

	private static void copyDataAverage(final int rows, final int columns, final int rowsE, final int columnsE, final float sum, FloatMatrix2D DataIn, FloatMatrix2D DataOut, FloatMatrix2D Result) {
		final float[] dataIn = (float[]) DataIn.elements();
		final float[] dataOut = (float[]) DataOut.elements();
		final float[] result = (float[]) Result.elements();

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
						float alphaI, alphaJ;
						float a;
						for (int r = startrow; r < stoprow; r++) {
							rOut = r + rOff;
							if (r < 0) {
								alphaJ = -r / ((float) rOff);
							} else if (r > (rows - 1)) {
								alphaJ = (r - rows) / ((float) rOff);
							} else {
								alphaJ = 0;
							}
							for (int c = -cOff; c < columnsE - cOff; c++) {
								cOut = c + cOff;
								if (c < 0) {
									alphaI = -c / ((float) cOff);
								} else if (c > (columns - 1)) {
									alphaI = (c - columns) / ((float) cOff);
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
			float alphaI, alphaJ;
			float a;
			for (int r = -rOff; r < rowsE - rOff; r++) {
				rOut = r + rOff;
				if (r < 0) {
					alphaJ = -r / ((float) rOff);
				} else if (r > (rows - 1)) {
					alphaJ = (r - rows) / ((float) rOff);
				} else {
					alphaJ = 0;
				}
				for (int c = -cOff; c < columnsE - cOff; c++) {
					cOut = c + cOff;
					if (c < 0) {
						alphaI = -c / ((float) cOff);
					} else if (c > (columns - 1)) {
						alphaI = (c - columns) / ((float) cOff);
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

	private static void deconvolveFD(final float gamma, final float magMax, final int rows, final int columns, FloatMatrix2D H1, FloatMatrix2D H2, FloatMatrix2D Result) {
		final float gammaScaled = gamma * magMax;
		final float[] h1 = (float[]) H1.elements();
		final float[] h2 = (float[]) H2.elements();
		final float[] result = (float[]) Result.elements();
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
						float mag, h2e, h2o;
						for (int r = startrow; r < stoprow; r++) {
							rC = (rows - r) % rows;
							for (int c = 0; c < columns; c++) {
								cC = (columns - c) % columns;
								idx1 = c + columns * r;
								idx2 = cC + columns * rC;
								h2e = (h2[idx1] + h2[idx2]) / 2;
								h2o = (h2[idx1] - h2[idx2]) / 2;
								mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
								float tmp = h1[idx1] * h2e - h1[idx2] * h2o;
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
			float mag, h2e, h2o;
			for (int r = 0; r < rows; r++) {
				rC = (rows - r) % rows;
				for (int c = 0; c < columns; c++) {
					cC = (columns - c) % columns;
					idx1 = c + columns * r;
					idx2 = cC + columns * rC;
					h2e = (h2[idx1] + h2[idx2]) / 2;
					h2o = (h2[idx1] - h2[idx2]) / 2;
					mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
					float tmp = h1[idx1] * h2e - h1[idx2] * h2o;
					result[idx1] = (tmp / (mag + gammaScaled));
				}
			}
		}
	}

	private static float energySum(FloatMatrix2D X, final int rows, final int columns, final int cOff, final int rOff) {
		float sumPixels = 0;
		final int rowStride = X.rowStride();
		final float[] elemsX = (float[]) X.elements();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (rows * columns >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Float>() {
					public Float call() throws Exception {
						float sumPixels = 0;
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
					results[j] = (Float) futures[j].get();
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

	private static float findMagMax(FloatMatrix2D H2) {
		final float[] h2 = (float[]) H2.elements();
		float magMax = 0;
		final int rows = H2.rows();
		final int columns = H2.columns();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (rows * columns >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Float>() {
					public Float call() throws Exception {
						int cC, rC, idx1, idx2;
						float magMax = 0;
						float mag;
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
					results[j] = (Float) futures[j].get();
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
			float mag;
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

	private static void gaussianFilter(FloatMatrix2D X, final float[][] weights) {
		final float[] elems = (float[]) X.elements();
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

	private static void gaussianFilterWithScaling(FloatMatrix2D X, final float[][] weights, final float scale) {
		final float[] elems = (float[]) X.elements();
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

	private static float[][] gaussianWeights(final int rows, final int columns, final float filterX, final float filterY) {
		final float[][] weights = new float[2][];
		weights[0] = new float[columns];
		weights[1] = new float[rows];
		final float cc = (float)(columns / (filterX + 0.000001));
		final float rc = (float)(rows / (filterY + 0.000001));
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
							float tmp = (cShifted / cc);
							weights[0][c] = (float) Math.exp(-tmp * tmp);
						}
						for (int r = startrow; r < stoprow; r++) {
							int rShifted = r;
							if (rShifted > rows / 2)
								rShifted = rows - rShifted;
							float tmp = (rShifted / rc);
							weights[1][r] = (float) Math.exp(-tmp * tmp);
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
				float tmp = (cShifted / cc);
				weights[0][c] = (float) Math.exp(-tmp * tmp);
			}
			for (int r = 0; r < rows; r++) {
				int rShifted = r;
				if (rShifted > rows / 2)
					rShifted = rows - rShifted;
				float tmp = (rShifted / rc);
				weights[1][r] = (float) Math.exp(-tmp * tmp);
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
		FloatWPL_2D damas = new FloatWPL_2D(blurImage, psfImage, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE,15, null, true);
		ImagePlus X = damas.deblur(-1);
		t.stop();
		t.display();
		X.show();
	}

	private static float meanDelta(FloatMatrix2D B, FloatMatrix2D AX, FloatMatrix2D X, final float aSum) {
		float meanDelta = 0;
		final float[] elemsB = (float[]) B.elements();
		final float[] elemsAX = (float[]) AX.elements();
		final float[] elemsX = (float[]) X.elements();
		final int size = B.size();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Float>() {
					public Float call() throws Exception {
						float meanDelta = 0;
						float delta;
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
					results[j] = (Float) futures[j].get();
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
			float delta;
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

	private static void toDB(FloatMatrix2D X, final float minDB) {
		final float[] x = (float[]) X.elements();
		final float SCALE = (float)(10 / Math.log(10));
		final float minVal = (float)Math.exp(minDB / SCALE);
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
								x[i] = (float)(SCALE * Math.log(x[i]));
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
					x[i] = (float)(SCALE * Math.log(x[i]));
				else
					x[i] = minDB;
			}
		}
	}

	private static float unDB(FloatMatrix2D X) {
		final float[] x = (float[]) X.elements();
		final float SCALE = (float)(10 / Math.log(10));
		final int size = X.size();
		float min = Float.MAX_VALUE;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Float>() {
					public Float call() throws Exception {
						float min = Float.MAX_VALUE;
						for (int i = startidx; i < stopidx; i++) {
							if (x[i] < min)
								min = x[i];
							x[i] = (float) Math.exp(x[i] / SCALE);
						}
						return min;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Float) futures[j].get();
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
				x[i] = (float) Math.exp(x[i] / SCALE);
			}
		}
		return min;
	}
}