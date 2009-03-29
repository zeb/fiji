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
package edu.emory.mathcs.restoretools.iterative;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.matrix.tfloat.FloatMatrix1D;
import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Common methods for 2D deblurring. Some code is from Bob Dougherty's Iterative
 * Deconvolve 3D.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 * 
 */
public class FloatCommon_2D {
	
	private FloatCommon_2D() {}

	/**
	 * Tolerance for optimization.Fmin.fmin.
	 */
	public static final float FMIN_TOL = 1.0e-4f;

	/**
	 * Relative accuracy for float.
	 */
	public static final float eps = (float) Math.pow(2, -23);

	/**
	 * Square root of the relative accuracy for float.
	 */
	public static final float sqrteps = (float) Math.sqrt(eps);

	/**
	 * Copies pixel values from image processor <code>ip</code> to matrix
	 * <code>X</code>.
	 * 
	 * @param ip
	 *            image processor
	 * @return matrix
	 * 
	 */
	public static FloatMatrix2D assignPixelsToMatrix_2D(final ImageProcessor ip) {
		FloatMatrix2D X;
		if (ip instanceof FloatProcessor) {
			X = new DenseFloatMatrix2D(ip.getHeight(), ip.getWidth(), (float[]) ip.getPixels(), 0, 0, ip.getWidth(), 1);
		} else {
			X = new DenseFloatMatrix2D(ip.getHeight(), ip.getWidth(), (float[]) ip.convertToFloat().getPixels(), 0, 0, ip.getWidth(), 1);
		}
		return X;
	}

	/**
	 * Copies pixel values from image processor <code>ip</code> to matrix
	 * <code>X</code>.
	 * 
	 * @param X
	 *            matrix
	 * @param ip
	 *            image processor
	 */
	public static void assignPixelsToMatrix_2D(final FloatMatrix2D X, final ImageProcessor ip) {
		if (ip instanceof FloatProcessor) {
			X.assign((float[]) ip.getPixels());
		} else {
			X.assign((float[]) ip.convertToFloat().getPixels());
		}
	}

	/**
	 * Copies pixel values from matrix <code>x</code> to image processor
	 * <code>ip</code>.
	 * 
	 * @param ip
	 *            image processor
	 * @param x
	 *            matrix
	 * @param cmY
	 *            color model
	 */
	public static void assignPixelsToProcessor(final FloatProcessor ip, final FloatMatrix1D x, final java.awt.image.ColorModel cmY) {
		final int rows = ip.getHeight();
		final int cols = ip.getWidth();
		final float[] px = (float[]) ip.getPixels();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if (x.isView()) {
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = cols / np;
				for (int j = 0; j < np; j++) {
					final int startcol = j * k;
					final int stopcol;
					if (j == np - 1) {
						stopcol = cols;
					} else {
						stopcol = startcol + k;
					}
					futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
						public void run() {
							int idx = startcol * rows;
							for (int c = startcol; c < stopcol; c++) {
								for (int r = 0; r < rows; r++) {
									px[c + cols * r] = (float) x.getQuick(idx++);
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
				int idx = 0;
				for (int c = 0; c < cols; c++) {
					for (int r = 0; r < rows; r++) {
						px[c + cols * r] = (float) x.getQuick(idx++);
					}
				}
			}
		} else {
			final float[] elems = (float[]) x.elements();
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = cols / np;
				for (int j = 0; j < np; j++) {
					final int startcol = j * k;
					final int stopcol;
					if (j == np - 1) {
						stopcol = cols;
					} else {
						stopcol = startcol + k;
					}
					futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
						public void run() {
							int idx = startcol * rows;
							for (int c = startcol; c < stopcol; c++) {
								for (int r = 0; r < rows; r++) {
									px[c + cols * r] = (float) elems[idx++];
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
				int idx = 0;
				for (int c = 0; c < cols; c++) {
					for (int r = 0; r < rows; r++) {
						px[c + cols * r] = (float) elems[idx++];
					}
				}
			}
		}
		ip.setMinAndMax(0, 0);
		ip.setColorModel(cmY);
	}

	/**
	 * Copies pixel values from matrix <code>x</code> to image processor
	 * <code>ip</code>.
	 * 
	 * @param ip
	 *            image processor
	 * @param x
	 *            matrix
	 * @param cmY
	 *            color model
	 * @param threshold
	 *            the smallest positive value assigned to the image processor,
	 *            all the values less than the threshold are set to zero
	 * 
	 */
	public static void assignPixelsToProcessor(final FloatProcessor ip, final FloatMatrix1D x, final java.awt.image.ColorModel cmY, final float threshold) {
		final int rows = ip.getHeight();
		final int cols = ip.getWidth();
		final float[] px = (float[]) ip.getPixels();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if (x.isView()) {
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = cols / np;
				for (int j = 0; j < np; j++) {
					final int startcol = j * k;
					final int stopcol;
					if (j == np - 1) {
						stopcol = cols;
					} else {
						stopcol = startcol + k;
					}
					futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
						public void run() {
							int idx = startcol * rows;
							float elem;
							for (int c = startcol; c < stopcol; c++) {
								for (int r = 0; r < rows; r++) {
									elem = (float) x.getQuick(idx++);
									if (elem >= threshold) {
										px[c + cols * r] = elem;
									} else {
										px[c + cols * r] = 0;
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
				int idx = 0;
				float elem;
				for (int c = 0; c < cols; c++) {
					for (int r = 0; r < rows; r++) {
						elem = (float) x.getQuick(idx++);
						if (elem >= threshold) {
							px[c + cols * r] = elem;
						} else {
							px[c + cols * r] = 0;
						}
					}
				}
			}
		} else {
			final float[] elems = (float[]) x.elements();
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = cols / np;
				for (int j = 0; j < np; j++) {
					final int startcol = j * k;
					final int stopcol;
					if (j == np - 1) {
						stopcol = cols;
					} else {
						stopcol = startcol + k;
					}
					futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
						public void run() {
							int idx = startcol * rows;
							for (int c = startcol; c < stopcol; c++) {
								for (int r = 0; r < rows; r++) {
									if ((float) elems[idx] >= threshold) {
										px[c + cols * r] = (float) elems[idx];
									} else {
										px[c + cols * r] = 0;
									}
									idx++;
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
				int idx = 0;
				for (int c = 0; c < cols; c++) {
					for (int r = 0; r < rows; r++) {
						if ((float) elems[idx] >= threshold) {
							px[c + cols * r] = (float) elems[idx];
						} else {
							px[c + cols * r] = 0;
						}
						idx++;
					}
				}
			}
		}
		ip.setMinAndMax(0, 0);
		ip.setColorModel(cmY);
	}

	/**
	 * Copies pixel values from matrix <code>X</code> to image processor
	 * <code>ip</code>.
	 * 
	 * @param ip
	 *            image processor
	 * @param X
	 *            matrix
	 * @param cmY
	 *            color model
	 * 
	 */
	public static void assignPixelsToProcessor(final FloatProcessor ip, final FloatMatrix2D X, final java.awt.image.ColorModel cmY) {
		final int rows = X.rows();
		final int cols = X.columns();
		final float[] px = (float[]) ip.getPixels();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if (X.isView()) {
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									px[c + cols * r] = (float) X.getQuick(r, c);
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
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						px[c + cols * r] = (float) X.getQuick(r, c);
					}
				}
			}
		} else {
			final float[] elems = (float[]) X.elements();
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
							int idx = startrow * cols;
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									px[idx] = (float) elems[idx];
									idx++;
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
				int idx = 0;
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						px[idx] = (float) elems[idx];
						idx++;
					}
				}
			}
		}
		ip.setMinAndMax(0, 0);
		ip.setColorModel(cmY);
	}

	/**
	 * Copies pixel values from complex matrix <code>X</code> to image processor
	 * <code>ip</code>
	 * 
	 * @param ip
	 *            image processor
	 * @param X
	 *            padded matrix
	 * @param rows
	 *            original number of rows
	 * @param cols
	 *            original number of columns
	 * @param rOff
	 *            row offset
	 * @param cOff
	 *            column offset
	 * @param cmY
	 *            color model
	 * 
	 */
	public static void assignPixelsToProcessorPadded(final FloatProcessor ip, final FloatMatrix2D X, final int rows, final int cols, final int rOff, final int cOff, final java.awt.image.ColorModel cmY) {
		final float[] px = (float[]) ip.getPixels();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if (X.isView()) {
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									px[c + cols * r] = (float) X.getQuick(r + rOff, c + cOff);
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
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						px[c + cols * r] = (float) X.getQuick(r + rOff, c + cOff);
					}
				}
			}
		} else {
			final float[] elems = (float[]) X.elements();
			final int rowStride = X.columns();
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
							int idx;
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									idx = (r + rOff) * rowStride + (c + cOff);
									px[r * cols + c] = (float) elems[idx];
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
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						idx = (r + rOff) * rowStride + (c + cOff);
						px[r * cols + c] = (float) elems[idx];
						idx++;
					}
				}
			}
		}
		ip.setMinAndMax(0, 0);
		ip.setColorModel(cmY);
	}

	/**
	 * Copies pixel values from complex matrix <code>X</code> to image processor
	 * <code>ip</code>
	 * 
	 * @param ip
	 *            image processor
	 * @param X
	 *            padded matrix
	 * @param rows
	 *            original number of rows
	 * @param cols
	 *            original number of columns
	 * @param rOff
	 *            row offset
	 * @param cOff
	 *            column offset
	 * @param cmY
	 *            color model
	 * @param threshold
	 *            the smallest positive value assigned to the image processor,
	 *            all the values less than the threshold are set to zero
	 */
	public static void assignPixelsToProcessorPadded(final FloatProcessor ip, final FloatMatrix2D X, final int rows, final int cols, final int rOff, final int cOff, final java.awt.image.ColorModel cmY, final float threshold) {
		final float[] px = (float[]) ip.getPixels();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if (X.isView()) {
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
							float elem;
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									elem = (float) X.getQuick(r + rOff, c + cOff);
									if (elem >= threshold) {
										px[c + cols * r] = elem;
									} else {
										px[c + cols * r] = 0;
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
				float elem;
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						elem = (float) X.getQuick(r + rOff, c + cOff);
						if (elem >= threshold) {
							px[c + cols * r] = elem;
						} else {
							px[c + cols * r] = 0;
						}
					}
				}
			}
		} else {
			final float[] elems = (float[]) X.elements();
			final int rowStride = X.columns();
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
							int idx;
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									idx = (r + rOff) * rowStride + (c + cOff);
									if ((float) elems[idx] >= threshold) {
										px[r * cols + c] = (float) elems[idx];
									} else {
										px[r * cols + c] = 0;
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
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						idx = (r + rOff) * rowStride + (c + cOff);
						if ((float) elems[idx] >= threshold) {
							px[r * cols + c] = (float) elems[idx];
						} else {
							px[r * cols + c] = 0;
						}
					}
				}
			}
		}
		ip.setMinAndMax(0, 0);
		ip.setColorModel(cmY);
	}

	/**
	 * Copies pixel values from matrix <code>X</code> to image processor
	 * <code>ip</code>.
	 * 
	 * @param ip
	 *            image processor
	 * @param X
	 *            matrix
	 * @param cmY
	 *            color model
	 * @param threshold
	 *            the smallest positive value assigned to the image processor,
	 *            all the values less than the threshold are set to zero
	 * 
	 */
	public static void assignPixelsToProcessor(final FloatProcessor ip, final FloatMatrix2D X, final java.awt.image.ColorModel cmY, final float threshold) {
		final int rows = X.rows();
		final int cols = X.columns();
		final float[] px = (float[]) ip.getPixels();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if (X.isView()) {
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
							float elem;
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									elem = (float) X.getQuick(r, c);
									if (elem >= threshold) {
										px[c + cols * r] = elem;
									} else {
										px[c + cols * r] = 0;
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
				float elem;
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						elem = (float) X.getQuick(r, c);
						if (elem >= threshold) {
							px[c + cols * r] = elem;
						} else {
							px[c + cols * r] = 0;
						}
					}
				}
			}
		} else {
			final float[] elems = (float[]) X.elements();
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
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
							int idx = startrow * cols;
							for (int r = startrow; r < stoprow; r++) {
								for (int c = 0; c < cols; c++) {
									if ((float) elems[idx] >= threshold) {
										px[idx] = (float) elems[idx];
									} else {
										px[idx] = 0;
									}
									idx++;
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
				int idx = 0;
				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < cols; c++) {
						if ((float) elems[idx] >= threshold) {
							px[idx] = (float) elems[idx];
						} else {
							px[idx] = 0;
						}
						idx++;
					}
				}
			}
		}
		ip.setMinAndMax(0, 0);
		ip.setColorModel(cmY);
	}

	/**
	 * Computes the circular shift of <code>PSF</code> matrix. This method
	 * computes a matrix containing first column of a blurring matrix when
	 * implementing periodic boundary conditions.
	 * 
	 * @param PSF
	 *            real matrix containing the point spread function.
	 * @param center
	 *            indices of center of <code>PSF</code>.
	 * @return real matrix containing first column of a blurring matrix
	 */
	public static FloatMatrix2D circShift_2D(FloatMatrix2D PSF, int[] center) {

		int rows = PSF.rows();
		int cols = PSF.columns();
		int cr = center[0];
		int cc = center[1];
		FloatMatrix2D P1 = new DenseFloatMatrix2D(rows, cols);
		P1.viewPart(0, 0, rows - cr, cols - cc).assign(PSF.viewPart(cr, cc, rows - cr, cols - cc));
		P1.viewPart(0, cols - cc, rows - cr, cc).assign(PSF.viewPart(cr, 0, rows - cr, cc));
		P1.viewPart(rows - cr, 0, cr, cols - cc).assign(PSF.viewPart(0, cc, cr, cols - cc));
		P1.viewPart(rows - cr, cols - cc, cr, cc).assign(PSF.viewPart(0, 0, cr, cc));
		return P1;
	}
	
	/**
	 * Converts an image into a given output type.
	 * 
	 * @param image
	 *            image
	 * @param output
	 *            output type
	 */
	public static void convertImage(ImagePlus image, OutputType output) {
		switch (output) {
		case BYTE:
			new ImageConverter(image).convertToGray8();
			break;
		case SHORT:
			new ImageConverter(image).convertToGray16();
			break;
		case FLOAT:
			//image is always in 32-bit precision
			break;
		}
	}

	/**
	 * Pads matrix <code>X</code> with periodic boundary conditions.
	 * 
	 * @param X
	 *            matrix to be padded
	 * @param rowsPad
	 *            number of rows in padded matrix
	 * @param colsPad
	 *            number of columns in padded matrix
	 * @return padded matrix
	 */
	public static FloatMatrix2D padPeriodic_2D(final FloatMatrix2D X, final int rowsPad, final int colsPad) {
		final int rows = X.rows();
		final int cols = X.columns();
		if ((rows == rowsPad) && (cols == colsPad)) {
			return X;
		}
		final FloatMatrix2D Xpad = new DenseFloatMatrix2D(rowsPad, colsPad);
		final int rOff = (rowsPad - rows + 1) / 2;
		final int cOff = (colsPad - cols + 1) / 2;
		final float[] elemsXpad = (float[]) Xpad.elements();
		final int rowStrideXpad = Xpad.columns();
		int np = ConcurrencyUtils.getNumberOfProcessors();

		if (X.isView()) {
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = rowsPad / np;
				for (int j = 0; j < np; j++) {
					final int startidx = -rOff + j * k;
					final int stopidx;
					if (j == np - 1) {
						stopidx = rowsPad - rOff;
					} else {
						stopidx = startidx + k;
					}
					futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
						public void run() {
							int cIn, rIn, cOut, rOut;
							int idxXpad;
							for (int r = startidx; r < stopidx; r++) {
								rOut = r + rOff;
								rIn = periodic(r, rows);
								for (int c = -cOff; c < colsPad - cOff; c++) {
									cOut = c + cOff;
									cIn = periodic(c, cols);
									idxXpad = rOut * rowStrideXpad + cOut;
									elemsXpad[idxXpad] = X.getQuick(rIn, cIn);
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
				int cIn, rIn, cOut, rOut;
				int idxXpad;
				for (int r = -rOff; r < rowsPad - rOff; r++) {
					rOut = r + rOff;
					rIn = periodic(r, rows);
					for (int c = -cOff; c < colsPad - cOff; c++) {
						cOut = c + cOff;
						cIn = periodic(c, cols);
						idxXpad = rOut * rowStrideXpad + cOut;
						elemsXpad[idxXpad] = X.getQuick(rIn, cIn);
					}
				}
			}
		} else {
			final float[] elemsX = (float[]) X.elements();
			final int rowStrideX = X.columns();
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = rowsPad / np;
				for (int j = 0; j < np; j++) {
					final int startidx = -rOff + j * k;
					final int stopidx;
					if (j == np - 1) {
						stopidx = rowsPad - rOff;
					} else {
						stopidx = startidx + k;
					}
					futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
						public void run() {
							int cIn, rIn, cOut, rOut;
							int idxX;
							int idxXpad;
							for (int r = startidx; r < stopidx; r++) {
								rOut = r + rOff;
								rIn = periodic(r, rows);
								for (int c = -cOff; c < colsPad - cOff; c++) {
									cOut = c + cOff;
									cIn = periodic(c, cols);
									idxX = rIn * rowStrideX + cIn;
									idxXpad = rOut * rowStrideXpad + cOut;
									elemsXpad[idxXpad] = elemsX[idxX];
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
				int cIn, rIn, cOut, rOut;
				int idxXpad;
				int idxX;
				for (int r = -rOff; r < rowsPad - rOff; r++) {
					rOut = r + rOff;
					rIn = periodic(r, rows);
					for (int c = -cOff; c < colsPad - cOff; c++) {
						cOut = c + cOff;
						cIn = periodic(c, cols);
						idxX = rIn * rowStrideX + cIn;
						idxXpad = rOut * rowStrideXpad + cOut;
						elemsXpad[idxXpad] = elemsX[idxX];
					}
				}
			}
		}
		return Xpad;
	}

	/**
	 * Pads matrix <code>X</code> with reflexive boundary conditions.
	 * 
	 * @param X
	 *            matrix to be padded
	 * @param rowsPad
	 *            number of rows in padded matrix
	 * @param colsPad
	 *            number of columns in padded matrix
	 * @return padded matrix
	 */
	public static FloatMatrix2D padReflexive_2D(final FloatMatrix2D X, final int rowsPad, final int colsPad) {
		final int rows = X.rows();
		final int cols = X.columns();
		if ((rows == rowsPad) && (cols == colsPad)) {
			return X;
		}
		final FloatMatrix2D Xpad = new DenseFloatMatrix2D(rowsPad, colsPad);
		final int rOff = (rowsPad - rows + 1) / 2;
		final int cOff = (colsPad - cols + 1) / 2;
		final float[] elemsXpad = (float[]) Xpad.elements();
		final int rowStrideXpad = Xpad.columns();
		int np = ConcurrencyUtils.getNumberOfProcessors();
		if (X.isView()) {
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = rowsPad / np;
				for (int j = 0; j < np; j++) {
					final int startidx = -rOff + j * k;
					final int stopidx;
					if (j == np - 1) {
						stopidx = rowsPad - rOff;
					} else {
						stopidx = startidx + k;
					}
					futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
						public void run() {
							int cIn, rIn, cOut, rOut;
							int idxXpad;
							for (int r = startidx; r < stopidx; r++) {
								rOut = r + rOff;
								rIn = mirror(r, rows);
								for (int c = -cOff; c < colsPad - cOff; c++) {
									cOut = c + cOff;
									cIn = mirror(c, cols);
									idxXpad = rOut * rowStrideXpad + cOut;
									elemsXpad[idxXpad] = X.getQuick(rIn, cIn);
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
				int cIn, rIn, cOut, rOut;
				int idxXpad;
				for (int r = -rOff; r < rowsPad - rOff; r++) {
					rOut = r + rOff;
					rIn = mirror(r, rows);
					for (int c = -cOff; c < colsPad - cOff; c++) {
						cOut = c + cOff;
						cIn = mirror(c, cols);
						idxXpad = rOut * rowStrideXpad + cOut;
						elemsXpad[idxXpad] = X.getQuick(rIn, cIn);
					}
				}
			}
		} else {
			final float[] elemsX = (float[]) X.elements();
			final int rowStrideX = X.columns();
			if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = rowsPad / np;
				for (int j = 0; j < np; j++) {
					final int startidx = -rOff + j * k;
					final int stopidx;
					if (j == np - 1) {
						stopidx = rowsPad - rOff;
					} else {
						stopidx = startidx + k;
					}
					futures[j] = ConcurrencyUtils.threadPool.submit(new Runnable() {
						public void run() {
							int cIn, rIn, cOut, rOut;
							int idxX;
							int idxXpad;
							for (int r = startidx; r < stopidx; r++) {
								rOut = r + rOff;
								rIn = mirror(r, rows);
								for (int c = -cOff; c < colsPad - cOff; c++) {
									cOut = c + cOff;
									cIn = mirror(c, cols);
									idxX = rIn * rowStrideX + cIn;
									idxXpad = rOut * rowStrideXpad + cOut;
									elemsXpad[idxXpad] = elemsX[idxX];
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
				int cIn, rIn, cOut, rOut;
				int idxX;
				int idxXpad;
				for (int r = -rOff; r < rowsPad - rOff; r++) {
					rOut = r + rOff;
					rIn = mirror(r, rows);
					for (int c = -cOff; c < colsPad - cOff; c++) {
						cOut = c + cOff;
						cIn = mirror(c, cols);
						idxX = rIn * rowStrideX + cIn;
						idxXpad = rOut * rowStrideXpad + cOut;
						elemsXpad[idxXpad] = elemsX[idxX];
					}
				}
			}

		}
		return Xpad;
	}

	/**
	 * Pads matrix <code>X</code> with zero boundary conditions.
	 * 
	 * @param X
	 *            matrix to be padded
	 * @param rowsPad
	 *            number of rows in padded matrix
	 * @param colsPad
	 *            number of columns in padded matrix
	 * @return padded matrix
	 */
	public static FloatMatrix2D padZero_2D(FloatMatrix2D X, int rowsPad, int colsPad) {
		final int rows = X.rows();
		final int cols = X.columns();
		if ((rows == rowsPad) && (cols == colsPad)) {
			return X;
		}
		FloatMatrix2D Xpad = new DenseFloatMatrix2D(rowsPad, colsPad);
		final int rOff = (rowsPad - rows + 1) / 2;
		final int cOff = (colsPad - cols + 1) / 2;
		Xpad.viewPart(rOff, cOff, rows, cols).assign(X);
		return Xpad;
	}

	/**
	 * Pads matrix <code>X</code> with zero boundary conditions.
	 * 
	 * @param X
	 *            matrix to be padded
	 * @param padSize
	 *            padding size
	 * @param padding
	 *            type of padding
	 * @return padded matrix
	 */
	public static FloatMatrix2D padZero_2D(FloatMatrix2D X, int[] padSize, PaddingType padding) {
		if ((padSize[0] == 0) && (padSize[1] == 0)) {
			return X;
		}
		FloatMatrix2D Xpad = null;
		switch (padding) {
		case BOTH:
			Xpad = new DenseFloatMatrix2D(X.rows() + 2 * padSize[0], X.columns() + 2 * padSize[1]);
			Xpad.viewPart(padSize[0], padSize[1], X.rows(), X.columns()).assign(X);
			break;
		case POST:
			Xpad = new DenseFloatMatrix2D(X.rows() + padSize[0], X.columns() + padSize[1]);
			Xpad.viewPart(0, 0, X.rows(), X.columns()).assign(X);
			break;
		case PRE:
			Xpad = new DenseFloatMatrix2D(X.rows() + padSize[0], X.columns() + padSize[1]);
			Xpad.viewPart(padSize[0], padSize[1], X.rows(), X.columns()).assign(X);
			break;
		}
		return Xpad;
	}

	private static int mirror(int i, int n) {
		int ip = mod(i, 2 * n);
		if (ip < n) {
			return ip;
		} else {
			return n - (ip % n) - 1;
		}
	}

	private static int mod(int i, int n) {
		return ((i % n) + n) % n;
	}

	private static int periodic(int i, int n) {
		int ip = mod(i, 2 * n);
		if (ip < n) {
			return ip;
		} else {
			return (ip % n);
		}
	}

}
