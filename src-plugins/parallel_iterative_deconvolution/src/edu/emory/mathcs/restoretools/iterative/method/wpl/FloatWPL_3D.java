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
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.restoretools.iterative.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.FloatCommon_2D;
import edu.emory.mathcs.restoretools.iterative.FloatCommon_3D;
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
public class FloatWPL_3D {
	private FloatWPLOptions options;

	private FloatMatrix3D PSF;

	private FloatMatrix3D B;

	private java.awt.image.ColorModel cmY;

	private int slices;

	private int columns;

	private int rows;

	private int bColumns;

	private int bRows;

	private int bSlices;

	private int maxIts;

	private float minB = 0;

	private float minPSF = 0;

	private float sum;

	private float scalePSF = 1;

	private float[][] g;

	private boolean showIteration;
	
	private OutputType output;

	/**
	 * Creates new instance of FloatWPL_3D
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
	public FloatWPL_3D(ImagePlus imB, ImagePlus imPSF, BoundaryType boundary, ResizingType resizing, OutputType output, int maxIts, FloatWPLOptions options, boolean showIteration) {
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
		B = new DenseFloatMatrix3D(bSlices, bRows, bColumns);
		FloatCommon_3D.assignPixelsToMatrix_3D(isB, B);
		ImageProcessor ipPSF = imPSF.getProcessor();
		ImageStack isPSF = imPSF.getStack();
		int psfSlices = isPSF.getSize();
		int psfColumns = ipPSF.getWidth();
		int psfRows = ipPSF.getHeight();
		PSF = new DenseFloatMatrix3D(psfSlices, psfRows, psfColumns);
		FloatCommon_3D.assignPixelsToMatrix_3D(isPSF, PSF);
		this.maxIts = maxIts;
		if (options == null) {
			this.options = new FloatWPLOptions(0, 1.0f, 1.0f, 1.0f, false, false, true, 0.01f, false, true);
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
			B = FloatCommon_3D.padPeriodic_3D(B, slices, rows, columns);
			break;
		case REFLEXIVE:
			B = FloatCommon_3D.padReflexive_3D(B, slices, rows, columns);
			break;
		case ZERO:
			B = FloatCommon_3D.padZero_3D(B, slices, rows, columns);
			break;
		}
		float[] maxLoc = PSF.getMaxLocation();
		int[] padSize = new int[3];
		padSize[0] = slices - psfSlices;
		padSize[1] = rows - psfRows;
		padSize[2] = columns - psfColumns;
		PSF = FloatCommon_3D.padZero_3D(PSF, padSize, PaddingType.POST);
		PSF = FloatCommon_3D.circShift_3D(PSF, new int[] {(int)maxLoc[1], (int)maxLoc[2], (int)maxLoc[3]});
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
		PSF.dht3();
		FloatMatrix3D X;
		FloatMatrix3D AX = B.like();
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
			float magMax = findMagMax(PSF);
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
		float aSum = PSF.aggregate(FloatFunctions.plus, FloatFunctions.abs);
		if (scalePSF != 1) {
			B.assign(FloatFunctions.div(scalePSF));
		}
		PSF.dht3();
		X = B.copy();
		ImagePlus imX = null;
		ImageStack is = new ImageStack(bColumns, bRows);
		if (showIteration) {
			FloatCommon_3D.assignPixelsToStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY);
			imX = new ImagePlus("(deblurred)", is);
			imX.show();
		}
		float oldPercentChange = Float.MAX_VALUE;
		for (int iter = 0; iter < maxIts; iter++) {
			IJ.showStatus("WPL iteration: " + (iter + 1) + "/" + maxIts);
			X.dht3();
			gaussianFilter(X, g);
			convolveFD(slices, rows, columns, PSF, X, AX);
			AX.idht3(true);
			X.idht3(true);
			float meanDelta = meanDelta(B, AX, X, aSum);
			if (showIteration) {
				if (threshold == -1.0) {
					FloatCommon_3D.updatePixelsInStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY);
				} else {
					FloatCommon_3D.updatePixelsInStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY, threshold);
				}
				ImageProcessor ip1 = imX.getProcessor();
				ip1.setMinAndMax(0, 0);
				ip1.setColorModel(cmY);
				imX.updateAndDraw();
			}
			float sumPixels = energySum(X, bSlices, bRows, bColumns, sOff, rOff, cOff);
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
				FloatCommon_3D.assignPixelsToStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY);
			} else {
				FloatCommon_3D.assignPixelsToStackPadded(is, X, bSlices, bRows, bColumns, sOff, rOff, cOff, cmY, threshold);
			}
			imX = new ImagePlus("(deblurred)", is);
		}
		FloatCommon_3D.convertImage(imX, output);
		return imX;
	}

	private static void convolveFD(final int slices, final int rows, final int columns, FloatMatrix3D H1, FloatMatrix3D H2, FloatMatrix3D Result) {
		final float[] h1 = (float[]) H1.elements();
		final float[] h2 = (float[]) H2.elements();
		final float[] result = (float[]) Result.elements();
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
						float h2e, h2o;
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
									result[idx1] = (float) (h1[idx1] * h2e + h1[idx2] * h2o);
									cC = (columns - c - 1) % columns;
									idx1 = c + 1 + rowStride * r + sliceStride * s;
									idx2 = cC + rowStride * rC + sliceStride * sC;
									h2e = (h2[idx1] + h2[idx2]) / 2;
									h2o = (h2[idx1] - h2[idx2]) / 2;
									result[idx1] = (float) (h1[idx1] * h2e + h1[idx2] * h2o);
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
			float h2e, h2o;
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
						result[idx1] = (float) (h1[idx1] * h2e + h1[idx2] * h2o);
						cC = (columns - c - 1) % columns;
						idx1 = c + 1 + rowStride * r + sliceStride * s;
						idx2 = cC + rowStride * rC + sliceStride * sC;
						h2e = (h2[idx1] + h2[idx2]) / 2;
						h2o = (h2[idx1] - h2[idx2]) / 2;
						result[idx1] = (float) (h1[idx1] * h2e + h1[idx2] * h2o);
					}
				}
			}
		}
	}

	private static void copyDataAverage(final int slices, final int rows, final int columns, final int slicesE, final int rowsE, final int columnsE, final float sum, FloatMatrix3D DataIn, FloatMatrix3D DataOut, FloatMatrix3D Result) {
		final float[] dataIn = (float[]) DataIn.elements();
		final float[] dataOut = (float[]) DataOut.elements();
		final float[] result = (float[]) Result.elements();
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
						float alphaS, alphaC, alphaR;
						float a;
						for (int s = startslice; s < stopslice; s++) {
							sOut = s + sOff;
							if (s < 0) {
								alphaS = -s / ((float) sOff);
							} else if (s > (slices - 1)) {
								alphaS = (s - slices) / ((float) sOff);
							} else {
								alphaS = 0;
							}
							for (int r = -rOff; r < rowsE - rOff; r++) {
								rOut = r + rOff;
								if (r < 0) {
									alphaR = -r / ((float) rOff);
								} else if (r > (rows - 1)) {
									alphaR = (r - rows) / ((float) rOff);
								} else {
									alphaR = 0;
								}
								for (int c = -cOff; c < columnsE - cOff; c++) {
									cOut = c + cOff;
									if (c < 0) {
										alphaC = -c / ((float) cOff);
									} else if (c > (columns - 1)) {
										alphaC = (c - columns) / ((float) cOff);
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
			float alphaS, alphaC, alphaR;
			float a;
			for (int s = -sOff; s < slicesE - sOff; s++) {
				sOut = s + sOff;
				if (s < 0) {
					alphaS = -s / ((float) sOff);
				} else if (s > (slices - 1)) {
					alphaS = (s - slices) / ((float) sOff);
				} else {
					alphaS = 0;
				}
				for (int r = -rOff; r < rowsE - rOff; r++) {
					rOut = r + rOff;
					if (r < 0) {
						alphaR = -r / ((float) rOff);
					} else if (r > (rows - 1)) {
						alphaR = (r - rows) / ((float) rOff);
					} else {
						alphaR = 0;
					}
					for (int c = -cOff; c < columnsE - cOff; c++) {
						cOut = c + cOff;
						if (c < 0) {
							alphaC = -c / ((float) cOff);
						} else if (c > (columns - 1)) {
							alphaC = (c - columns) / ((float) cOff);
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

	private static void deconvolveFD(final float gamma, final float magMax, final int slices, final int rows, final int columns, FloatMatrix3D H1, FloatMatrix3D H2, FloatMatrix3D Result) {
		final float gammaScaled = gamma * magMax;
		final float[] h1 = (float[]) H1.elements();
		final float[] h2 = (float[]) H2.elements();
		final float[] result = (float[]) Result.elements();
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
						float mag, h2e, h2o;
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
									float tmp = h1[idx1] * h2e - h1[idx2] * h2o;
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
			float mag, h2e, h2o;
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
						float tmp = h1[idx1] * h2e - h1[idx2] * h2o;
						result[idx1] = (tmp / (mag + gammaScaled));
					}
				}
			}
		}
	}

	private static float energySum(FloatMatrix3D X, final int slices, final int rows, final int columns, final int sOff, final int rOff, final int cOff) {
		float sumPixels = 0;
		final int rowStride = X.rowStride();
		final int sliceStride = X.sliceStride();
		final float[] elemsX = (float[]) X.elements();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * rows * columns >= ConcurrencyUtils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Float>() {
					public Float call() throws Exception {
						float sumPixels = 0;
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

	private static float findMagMax(FloatMatrix3D H2) {
		final float[] h2 = (float[]) H2.elements();
		float magMax = 0;
		final int slices = H2.slices();
		final int rows = H2.rows();
		final int columns = H2.columns();
		final int sliceStride = rows * columns;
		final int rowStride = columns;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (slices * rows * columns >= ConcurrencyUtils.getThreadsBeginN_3D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = ConcurrencyUtils.threadPool.submit(new Callable<Float>() {
					public Float call() throws Exception {
						int sC, cC, rC, idx1, idx2;
						float magMax = 0;
						float mag;
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
			int sC, cC, rC, idx1, idx2;
			float mag;
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

	private static void gaussianFilter(FloatMatrix3D X, final float[][] weights) {
		final float[] elems = (float[]) X.elements();
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

	private static void gaussianFilterWithScaling(FloatMatrix3D X, final float[][] weights, final float scale) {
		final float[] elems = (float[]) X.elements();
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

	private static float[][] gaussianWeights(final int slices, final int rows, final int columns, final float filterX, final float filterY, final float filterZ) {
		final float[][] weights = new float[3][];
		weights[0] = new float[columns];
		weights[1] = new float[rows];
		weights[2] = new float[slices];
		final float cc = (float)(columns / (filterX + 0.000001));
		final float rc = (float)(rows / (filterY + 0.000001));
		final float sc = (float)(slices / (filterZ + 0.000001));
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
						for (int s = startslice; s < stopslice; s++) {
							int sShifted = s;
							if (sShifted > slices / 2)
								sShifted = slices - sShifted;
							float tmp = (sShifted / sc);
							weights[2][s] = (float) Math.exp(-tmp * tmp);
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
			for (int s = 0; s < slices; s++) {
				int sShifted = s;
				if (sShifted > slices / 2)
					sShifted = slices - sShifted;
				float tmp = (sShifted / sc);
				weights[2][s] = (float) Math.exp(-tmp * tmp);
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
		FloatWPL_3D damas = new FloatWPL_3D(blurImage, psfImage, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 15, null, true);
		ImagePlus X = damas.deblur(-1);
		t.stop();
		t.display();
		X.show();
	}

	private static float meanDelta(FloatMatrix3D B, FloatMatrix3D AX, FloatMatrix3D X, final float aSum) {
		float meanDelta = 0;
		final float[] elemsB = (float[]) B.elements();
		final float[] elemsAX = (float[]) AX.elements();
		final float[] elemsX = (float[]) X.elements();
		final int size = B.size();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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

	private static void toDB(FloatMatrix3D X, final float minDB) {
		final float[] x = (float[]) X.elements();
		final float SCALE = (float)(10 / Math.log(10));
		final float minVal = (float)Math.exp(minDB / SCALE);
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

	private static float unDB(FloatMatrix3D X) {
		final float[] x = (float[]) X.elements();
		final float SCALE = (float)(10 / Math.log(10));
		final int size = X.size();
		float min = Float.MAX_VALUE;
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_3D())) {
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