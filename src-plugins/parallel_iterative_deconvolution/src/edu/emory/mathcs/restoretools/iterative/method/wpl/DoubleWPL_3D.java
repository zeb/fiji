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
 A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
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
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.Timer;
import cern.colt.matrix.tdouble.DoubleMatrix3D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix3D;
import cern.jet.math.tdouble.DoubleFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.DoubleCommon_2D;
import edu.emory.mathcs.restoretools.iterative.DoubleCommon_3D;
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
public class DoubleWPL_3D {
	private DoubleWPLOptions options;

	private DoubleMatrix3D PSF;

	private DoubleMatrix3D B;

	private java.awt.image.ColorModel cmY;

	private int slices;

	private int columns;

	private int rows;

	private int bColumns;

	private int bRows;

	private int bSlices;

	private int maxIts;

	private double minB = 0;

	private double minPSF = 0;

	private double sum;

	private double scalePSF = 1;

	private double[][] g;

	private boolean showIteration;
	
	private OutputType output;

	/**
	 * Creates new instance of DoubleWPL_3D
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
	public DoubleWPL_3D(ImagePlus imB, ImagePlus imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, int maxIts, DoubleWPLOptions options, boolean showIteration) {
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
		ImageStack isB = imB.getStack();
		cmY = ipB.getColorModel();
		bSlices = isB.getSize();
		bColumns = ipB.getWidth();
		bRows = ipB.getHeight();
		B = new DenseDoubleMatrix3D(bSlices, bRows, bColumns);
		DoubleCommon_3D.assignPixelsToMatrix_3D(isB, B);
		ImageProcessor ipPSF = imPSF.getProcessor();
		ImageStack isPSF = imPSF.getStack();
		int psfSlices = isPSF.getSize();
		int psfColumns = ipPSF.getWidth();
		int psfRows = ipPSF.getHeight();
		PSF = new DenseDoubleMatrix3D(psfSlices, psfRows, psfColumns);
		DoubleCommon_3D.assignPixelsToMatrix_3D(isPSF, PSF);
		this.maxIts = maxIts;
		if (options == null) {
			this.options = new DoubleWPLOptions(0, 1.0, 1.0, 1.0, false, false, true, 0.01, false, true);
		} else {
			this.options = options;
		}
		if (this.options.dB) {
			minB = unDB(B);
			minPSF = unDB(PSF);
		}
		this.showIteration = showIteration;
		sum = PSF.zSum();
		if ((sum != 0) && this.options.normalize)
			scalePSF /= sum;
		slices = expandedSize(psfSlices, bSlices, resizing);
		columns = expandedSize(psfColumns, bColumns, resizing);
		rows = expandedSize(psfRows, bRows, resizing);
		if ((psfSlices > slices) || (psfColumns > columns) || (psfRows > rows)) {
			throw new IllegalArgumentException("PSF cannot be largest that the image.");
		}
		g = gaussianWeights(slices, rows, columns, this.options.filterX, this.options.filterY, this.options.filterZ);
		switch (boundary) {
		case PERIODIC:
			B = DoubleCommon_3D.padPeriodic_3D(B, slices, rows, columns);
			break;
		case REFLEXIVE:
			B = DoubleCommon_3D.padReflexive_3D(B, slices, rows, columns);
			break;
		case ZERO:
			B = DoubleCommon_3D.padZero_3D(B, slices, rows, columns);
			break;
		}
		double[] maxLoc = PSF.getMaxLocation();
		int[] padSize = new int[3];
		padSize[0] = slices - psfSlices;
		padSize[1] = rows - psfRows;
		padSize[2] = columns - psfColumns;
		PSF = DoubleCommon_3D.padZero_3D(PSF, padSize, PaddingType.POST);
		PSF = DoubleCommon_3D.circShift_3D(PSF, new int[] {(int)maxLoc[1], (int)maxLoc[2], (int)maxLoc[3]});
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
		PSF.dht3();
		DoubleMatrix3D X;
		DoubleMatrix3D AX = B.like();
		if (options.antiRing) {
			IJ.showStatus("WPL: performing anti-ringing step.");
			X = B.copy();
			X.dht3();
			convolveFD(slices, rows, columns, PSF, X, AX);
			AX.idht3(true);
			copyDataAverage(bSlices, bRows, bColumns, slices, rows, columns, sum, B, AX, B);
		}
		if (options.gamma > 0.0001) {
			IJ.showStatus("WPL: Winner filter");
			double magMax = findMagMax(PSF);
			B.dht3();
			X = PSF.copy();
			deconvolveFD(options.gamma, magMax, slices, rows, columns, X, X, PSF);
			AX = B.copy();
			deconvolveFD(options.gamma, magMax, slices, rows, columns, AX, X, B);
			B.idht3(true);
		}

		int sOff = (slices - bSlices + 1) / 2;
		int rOff = (rows - bRows + 1) / 2;
		int cOff = (columns - bColumns + 1) / 2;

		PSF.idht3(true);
		double aSum = PSF.aggregate(DoubleFunctions.plus, DoubleFunctions.abs);
		if (scalePSF != 1) {
			B.assign(DoubleFunctions.div(scalePSF));
		}
		PSF.dht3();
		X = B.copy();
		ImagePlus imX = null;
		ImageStack is = new ImageStack(bColumns, bRows);
		if (showIteration) {
			DoubleCommon_3D.assignPixelsToStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY);
			imX = new ImagePlus("(deblurred)", is);
			imX.show();
		}
		double oldPercentChange = Double.MAX_VALUE;
		for (int iter = 0; iter < maxIts; iter++) {
			IJ.showStatus("WPL iteration: " + (iter + 1) + "/" + maxIts);
			X.dht3();
			gaussianFilter(X, g);
			convolveFD(slices, rows, columns, PSF, X, AX);
			AX.idht3(true);
			X.idht3(true);
			double meanDelta = meanDelta(B, AX, X, aSum);
			if (showIteration) {
				if (threshold == -1.0) {
					DoubleCommon_3D.updatePixelsInStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY);
				} else {
					DoubleCommon_3D.updatePixelsInStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY, threshold);
				}
				ImageProcessor ip1 = imX.getProcessor();
				ip1.setMinAndMax(0, 0);
				ip1.setColorModel(cmY);
				imX.updateAndDraw();
			}
			double sumPixels = energySum(X, bSlices, bRows, bColumns, sOff, rOff, cOff);
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
		X.dht3();
		gaussianFilterWithScaling(X, g, aSum);
		X.idht3(true);
		if (options.dB) {
			toDB(PSF, minPSF);
			toDB(B, minB);
			toDB(X, -90);
		}
		if (!showIteration) {
			if (threshold == -1.0) {
				DoubleCommon_3D.assignPixelsToStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY);
			} else {
				DoubleCommon_3D.assignPixelsToStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", is);
		}
		DoubleCommon_3D.convertImage(imX, output);
		return imX;
	}

	private static void convolveFD(final int slices, final int rows, final int columns, DoubleMatrix3D H1, DoubleMatrix3D H2, DoubleMatrix3D Result) {
		final double[] h1 = (double[]) H1.elements();
		final double[] h2 = (double[]) H2.elements();
		final double[] result = (double[]) Result.elements();
		final int sliceStride = columns * rows;
		final int rowStride = columns;

		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * columns * rows >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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
						int sC, cC, rC, idx1, idx2;
						double h2e, h2o;
						for (int s = startslice; s < stopslice; s++) {
							sC = (slices - s) % slices;
							for (int r = 0; r < rows; r++) {
								rC = (rows - r) % rows;
								for (int c = 0; c < columns; c += 2) {
									cC = (columns - c) % columns;
									idx1 = c + rowStride * r + sliceStride * s;
									idx2 = cC + rowStride * rC + sliceStride * sC;
									h2e = (h2[idx1] + h2[idx2]) / 2;
									h2o = (h2[idx1] - h2[idx2]) / 2;
									result[idx1] = (double) (h1[idx1] * h2e + h1[idx2] * h2o);
									cC = (columns - c - 1) % columns;
									idx1 = c + 1 + rowStride * r + sliceStride * s;
									idx2 = cC + rowStride * rC + sliceStride * sC;
									h2e = (h2[idx1] + h2[idx2]) / 2;
									h2o = (h2[idx1] - h2[idx2]) / 2;
									result[idx1] = (double) (h1[idx1] * h2e + h1[idx2] * h2o);
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
			int sC, cC, rC, idx1, idx2;
			double h2e, h2o;
			for (int s = 0; s < slices; s++) {
				sC = (slices - s) % slices;
				for (int r = 0; r < rows; r++) {
					rC = (rows - r) % rows;
					for (int c = 0; c < columns; c += 2) {
						cC = (columns - c) % columns;
						idx1 = c + rowStride * r + sliceStride * s;
						idx2 = cC + rowStride * rC + sliceStride * sC;
						h2e = (h2[idx1] + h2[idx2]) / 2;
						h2o = (h2[idx1] - h2[idx2]) / 2;
						result[idx1] = (double) (h1[idx1] * h2e + h1[idx2] * h2o);
						cC = (columns - c - 1) % columns;
						idx1 = c + 1 + rowStride * r + sliceStride * s;
						idx2 = cC + rowStride * rC + sliceStride * sC;
						h2e = (h2[idx1] + h2[idx2]) / 2;
						h2o = (h2[idx1] - h2[idx2]) / 2;
						result[idx1] = (double) (h1[idx1] * h2e + h1[idx2] * h2o);
					}
				}
			}
		}
	}

	private static void copyDataAverage(final int slices, final int rows, final int columns, final int slicesE, final int rowsE, final int columnsE, final double sum, DoubleMatrix3D DataIn, DoubleMatrix3D DataOut, DoubleMatrix3D Result) {
		final double[] dataIn = (double[]) DataIn.elements();
		final double[] dataOut = (double[]) DataOut.elements();
		final double[] result = (double[]) Result.elements();
		final int sOff = (slicesE - slices + 1) / 2;
		final int rOff = (rowsE - rows + 1) / 2;
		final int cOff = (columnsE - columns + 1) / 2;
		final int sliceStride = rowsE * columnsE;
		final int rowStride = columnsE;

		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slicesE * columnsE * rowsE >= ConcurrencyUtils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			int k = slicesE / np;
			for (int j = 0; j < np; j++) {
				final int startslice = -sOff + j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slicesE - sOff;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {

					public void run() {
						int sOut, cOut, rOut, idx;
						double alphaS, alphaC, alphaR;
						double a;
						for (int s = startslice; s < stopslice; s++) {
							sOut = s + sOff;
							if (s < 0) {
								alphaS = -s / ((double) sOff);
							} else if (s > (slices - 1)) {
								alphaS = (s - slices) / ((float) sOff);
							} else {
								alphaS = 0;
							}
							for (int r = -rOff; r < rowsE - rOff; r++) {
								rOut = r + rOff;
								if (r < 0) {
									alphaR = -r / ((double) rOff);
								} else if (r > (rows - 1)) {
									alphaR = (r - rows) / ((double) rOff);
								} else {
									alphaR = 0;
								}
								for (int c = -cOff; c < columnsE - cOff; c++) {
									cOut = c + cOff;
									if (c < 0) {
										alphaC = -c / ((double) cOff);
									} else if (c > (columns - 1)) {
										alphaC = (c - columns) / ((double) cOff);
									} else {
										alphaC = 0;
									}
									a = alphaS;
									if (alphaR > a)
										a = alphaR;
									if (alphaC > a)
										a = alphaC;
									idx = cOut + rowStride * rOut + sliceStride * sOut;
									result[idx] = (1 - a) * dataIn[idx] + a * dataOut[idx] / sum;
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
			int sOut, cOut, rOut, idx;
			double alphaS, alphaC, alphaR;
			double a;
			for (int s = -sOff; s < slicesE - sOff; s++) {
				sOut = s + sOff;
				if (s < 0) {
					alphaS = -s / ((double) sOff);
				} else if (s > (slices - 1)) {
					alphaS = (s - slices) / ((float) sOff);
				} else {
					alphaS = 0;
				}
				for (int r = -rOff; r < rowsE - rOff; r++) {
					rOut = r + rOff;
					if (r < 0) {
						alphaR = -r / ((double) rOff);
					} else if (r > (rows - 1)) {
						alphaR = (r - rows) / ((double) rOff);
					} else {
						alphaR = 0;
					}
					for (int c = -cOff; c < columnsE - cOff; c++) {
						cOut = c + cOff;
						if (c < 0) {
							alphaC = -c / ((double) cOff);
						} else if (c > (columns - 1)) {
							alphaC = (c - columns) / ((double) cOff);
						} else {
							alphaC = 0;
						}
						a = alphaS;
						if (alphaR > a)
							a = alphaR;
						if (alphaC > a)
							a = alphaC;
						idx = cOut + rowStride * rOut + sliceStride * sOut;
						result[idx] = (1 - a) * dataIn[idx] + a * dataOut[idx] / sum;
					}
				}
			}
		}
	}

	private static void deconvolveFD(final double gamma, final double magMax, final int slices, final int rows, final int columns, DoubleMatrix3D H1, DoubleMatrix3D H2, DoubleMatrix3D Result) {
		final double gammaScaled = gamma * magMax;
		final double[] h1 = (double[]) H1.elements();
		final double[] h2 = (double[]) H2.elements();
		final double[] result = (double[]) Result.elements();
		final int sliceStride = columns * rows;
		final int rowStride = columns;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * columns * rows >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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
						int sC, cC, rC, idx1, idx2;
						double mag, h2e, h2o;
						for (int s = startslice; s < stopslice; s++) {
							sC = (slices - s) % slices;
							for (int r = 0; r < rows; r++) {
								rC = (rows - r) % rows;
								for (int c = 0; c < columns; c++) {
									cC = (columns - c) % columns;
									idx1 = c + rowStride * r + sliceStride * s;
									idx2 = cC + rowStride * rC + sliceStride * sC;
									h2e = (h2[idx1] + h2[idx2]) / 2;
									h2o = (h2[idx1] - h2[idx2]) / 2;
									mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
									double tmp = h1[idx1] * h2e - h1[idx2] * h2o;
									result[idx1] = (tmp / (mag + gammaScaled));
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
			int sC, cC, rC, idx1, idx2;
			double mag, h2e, h2o;
			for (int s = 0; s < slices; s++) {
				sC = (slices - s) % slices;
				for (int r = 0; r < rows; r++) {
					rC = (rows - r) % rows;
					for (int c = 0; c < columns; c++) {
						cC = (columns - c) % columns;
						idx1 = c + rowStride * r + sliceStride * s;
						idx2 = cC + rowStride * rC + sliceStride * sC;
						h2e = (h2[idx1] + h2[idx2]) / 2;
						h2o = (h2[idx1] - h2[idx2]) / 2;
						mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
						double tmp = h1[idx1] * h2e - h1[idx2] * h2o;
						result[idx1] = (tmp / (mag + gammaScaled));
					}
				}
			}
		}
	}

	private static double energySum(DoubleMatrix3D X, final int slices, final int rows, final int columns, final int sOff, final int rOff, final int cOff) {
		double sumPixels = 0;
		final int rowStride = X.rowStride();
		final int sliceStride = X.sliceStride();
		final double[] elemsX = (double[]) X.elements();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * rows * columns >= ConcurrencyUtils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Double>() {
					public Double call() throws Exception {
						double sumPixels = 0;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								for (int c = 0; c < columns; c++) {
									sumPixels += elemsX[c + cOff + rowStride * (r + rOff) + sliceStride * (s + sOff)];
								}
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
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < columns; c++) {
						sumPixels += elemsX[c + cOff + rowStride * (r + rOff) + sliceStride * (s + sOff)];
					}
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

	private static double findMagMax(DoubleMatrix3D H2) {
		final double[] h2 = (double[]) H2.elements();
		double magMax = 0;
		final int slices = H2.slices();
		final int rows = H2.rows();
		final int columns = H2.columns();
		final int sliceStride = rows * columns;
		final int rowStride = columns;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * rows * columns >= ConcurrencyUtils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Double>() {
					public Double call() throws Exception {
						int sC, cC, rC, idx1, idx2;
						double magMax = 0;
						double mag;
						for (int s = startslice; s < stopslice; s++) {
							sC = (slices - s) % slices;
							for (int r = 0; r < rows; r++) {
								rC = (rows - r) % rows;
								for (int c = 0; c < columns; c++) {
									cC = (columns - c) % columns;
									idx1 = c + rowStride * r + sliceStride * s;
									idx2 = cC + rowStride * rC + sliceStride * sC;
									mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
									if (mag > magMax)
										magMax = mag;
								}
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
			int sC, cC, rC, idx1, idx2;
			double mag;
			for (int s = 0; s < slices; s++) {
				sC = (slices - s) % slices;
				for (int r = 0; r < rows; r++) {
					rC = (rows - r) % rows;
					for (int c = 0; c < columns; c++) {
						cC = (columns - c) % columns;
						idx1 = c + rowStride * r + sliceStride * s;
						idx2 = cC + rowStride * rC + sliceStride * sC;
						mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
						if (mag > magMax)
							magMax = mag;
					}
				}
			}
		}
		return magMax;
	}

	private static void gaussianFilter(DoubleMatrix3D X, final double[][] weights) {
		final double[] elems = (double[]) X.elements();
		final int sliceStride = X.sliceStride();
		final int rowStride = X.rowStride();
		final int columnStride = X.columnStride();
		final int slices = X.slices();
		final int rows = X.rows();
		final int columns = X.columns();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * columns * rows >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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
						int idx;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									elems[idx] *= weights[2][s] * weights[1][r] * weights[0][c];
									idx += columnStride;
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
			int idx;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						elems[idx] *= weights[2][s] * weights[1][r] * weights[0][c];
						idx += columnStride;
					}
				}
			}
		}
	}

	private static void gaussianFilterWithScaling(DoubleMatrix3D X, final double[][] weights, final double scale) {
		final double[] elems = (double[]) X.elements();
		final int sliceStride = X.sliceStride();
		final int rowStride = X.rowStride();
		final int columnStride = X.columnStride();
		final int slices = X.slices();
		final int rows = X.rows();
		final int columns = X.columns();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * columns * rows >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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
						int idx;
						for (int s = startslice; s < stopslice; s++) {
							for (int r = 0; r < rows; r++) {
								idx = s * sliceStride + r * rowStride;
								for (int c = 0; c < columns; c++) {
									elems[idx] *= weights[2][s] * weights[1][r] * weights[0][c] / scale;
									idx += columnStride;
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
			int idx;
			for (int s = 0; s < slices; s++) {
				for (int r = 0; r < rows; r++) {
					idx = s * sliceStride + r * rowStride;
					for (int c = 0; c < columns; c++) {
						elems[idx] *= weights[2][s] * weights[1][r] * weights[0][c] / scale;
						idx += columnStride;
					}
				}
			}
		}
	}

	private static double[][] gaussianWeights(final int slices, final int rows, final int columns, final double filterX, final double filterY, final double filterZ) {
		final double[][] weights = new double[3][];
		weights[0] = new double[columns];
		weights[1] = new double[rows];
		weights[2] = new double[slices];
		final double cc = columns / (filterX + 0.000001);
		final double rc = rows / (filterY + 0.000001);
		final double sc = slices / (filterZ + 0.000001);
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (Math.max(slices, Math.max(columns, rows)) >= ConcurrencyUtils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int kcol = columns / np;
			int krow = rows / np;
			int ksls = slices / np;
			for (int j = 0; j < np; j++) {
				final int startcol = j * kcol;
				final int stopcol;
				final int startrow = j * krow;
				final int stoprow;
				final int startslice = j * ksls;
				final int stopslice;
				if (j == np - 1) {
					stopcol = columns;
					stoprow = rows;
					stopslice = slices;
				} else {
					stopcol = startcol + kcol;
					stoprow = startrow + krow;
					stopslice = startslice + ksls;
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
						for (int s = startslice; s < stopslice; s++) {
							int sShifted = s;
							if (sShifted > slices / 2)
								sShifted = slices - sShifted;
							double tmp = (sShifted / sc);
							weights[2][s] = (double) Math.exp(-tmp * tmp);
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
			for (int s = 0; s < slices; s++) {
				int sShifted = s;
				if (sShifted > slices / 2)
					sShifted = slices - sShifted;
				double tmp = (sShifted / sc);
				weights[2][s] = (double) Math.exp(-tmp * tmp);
			}
		}
		return weights;
	}

	public static void main(String[] args) {
		ConcurrencyUtils.setNumberOfProcessors(2);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage("D:\\Research\\Images\\head_mri\\iterative\\head-blur.tif");
		ImagePlus psfImage = o.openImage("D:\\Research\\Images\\head_mri\\iterative\\head-psf.tif");
		Timer t = new Timer();
		t.reset().start();
		DoubleWPL_3D damas = new DoubleWPL_3D(blurImage, psfImage, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 15, null, true);
		ImagePlus X = damas.deblur(-1);
		t.stop();
		t.display();
		X.show();
	}

	private static double meanDelta(DoubleMatrix3D B, DoubleMatrix3D AX, DoubleMatrix3D X, final double aSum) {
		double meanDelta = 0;
		final double[] elemsB = (double[]) B.elements();
		final double[] elemsAX = (double[]) AX.elements();
		final double[] elemsX = (double[]) X.elements();
		final int size = B.size();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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

	private static void toDB(DoubleMatrix3D X, final double minDB) {
		final double[] x = (double[]) X.elements();
		final double SCALE = 10 / Math.log(10);
		final double minVal = Math.exp(minDB / SCALE);
		int size = X.size();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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

	private static double unDB(DoubleMatrix3D X) {
		final double[] x = (double[]) X.elements();
		final double SCALE = 10 / Math.log(10);
		final int size = X.size();
		double min = Double.MAX_VALUE;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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