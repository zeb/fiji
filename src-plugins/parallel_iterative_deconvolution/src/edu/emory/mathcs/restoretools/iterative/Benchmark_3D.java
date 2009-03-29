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
import ij.io.Opener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import edu.emory.mathcs.restoretools.iterative.method.cgls.DoubleCGLS_3D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.DoublePCGLS_3D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.FloatCGLS_3D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.FloatPCGLS_3D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.DoubleHyBROptions;
import edu.emory.mathcs.restoretools.iterative.method.hybr.DoubleHyBR_3D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.FloatHyBROptions;
import edu.emory.mathcs.restoretools.iterative.method.hybr.FloatHyBR_3D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.InSolvType;
import edu.emory.mathcs.restoretools.iterative.method.hybr.RegMethodType;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.DoubleMRNSD_3D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.DoublePMRNSD_3D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.FloatMRNSD_3D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.FloatPMRNSD_3D;
import edu.emory.mathcs.restoretools.iterative.method.wpl.DoubleWPLOptions;
import edu.emory.mathcs.restoretools.iterative.method.wpl.DoubleWPL_3D;
import edu.emory.mathcs.restoretools.iterative.method.wpl.FloatWPLOptions;
import edu.emory.mathcs.restoretools.iterative.method.wpl.FloatWPL_3D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.PreconditionerType;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Benchmark for Parallel Iterative Deconvolution 3D
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class Benchmark_3D {

	private static final String path = "/home/pwendyk/images/iterative/";

	private static final String blur_image = "head-blur.tif";

	private static final String psf_image = "head-psf.tif";

	private static final BoundaryType boundary = BoundaryType.REFLEXIVE;

	private static final ResizingType resizing = ResizingType.AUTO;

	private static final OutputType output = OutputType.FLOAT;

	private static final int NITER = 10;

	private static final int MAXITER = 5;

	private static final double prec_tol = 0.01;

	private static final String format = "%.2f";

	public static void benchmarkDoubleCGLS_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking DoubleCGLS_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoubleCGLS_3D cgls = new DoubleCGLS_3D(blurImage, psfImage, boundary, resizing, output, MAXITER, 0, false, false);
			ImagePlus imX = cgls.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			cgls = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleCGLS_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoublePCGLS_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_auto_tol = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_auto_tol = 0;
		System.out.println("Benchmarking DoublePCGLS_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoublePCGLS_3D pcgls = new DoublePCGLS_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, prec_tol, MAXITER, 0, false, false);
			ImagePlus imX = pcgls.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_auto_tol = System.nanoTime();
			pcgls = new DoublePCGLS_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, -1, MAXITER, 0, false, false);
			imX = pcgls.deblur(-1);
			elapsedTime_deblur_auto_tol = System.nanoTime() - elapsedTime_deblur_auto_tol;
			av_time_deblur_auto_tol = av_time_deblur_auto_tol + elapsedTime_deblur_auto_tol;

			pcgls = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (tol =  " + prec_tol + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (tol = auto): " + String.format(format, av_time_deblur_auto_tol / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoublePCGLS_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_auto_tol / 1000000000.0 / (double) NITER, prec_tol);
	}

	public static void benchmarkDoubleMRNSD_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking DoubleMRNSD_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoubleMRNSD_3D mrnsd = new DoubleMRNSD_3D(blurImage, psfImage, boundary, resizing, output, MAXITER, 0, false, false);
			ImagePlus imX = mrnsd.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			mrnsd = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleMRNSD_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoublePMRNSD_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_auto_tol = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_auto_tol = 0;
		System.out.println("Benchmarking DoublePMRNSD_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			DoublePMRNSD_3D pmrnsd = new DoublePMRNSD_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, prec_tol, MAXITER, 0, false, false);
			ImagePlus imX = pmrnsd.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_auto_tol = System.nanoTime();
			pmrnsd = new DoublePMRNSD_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, -1, MAXITER, 0, false, false);
			imX = pmrnsd.deblur(-1);
			elapsedTime_deblur_auto_tol = System.nanoTime() - elapsedTime_deblur_auto_tol;
			av_time_deblur_auto_tol = av_time_deblur_auto_tol + elapsedTime_deblur_auto_tol;

			pmrnsd = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (tol =  " + prec_tol + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (tol = auto): " + String.format(format, av_time_deblur_auto_tol / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoublePMRNSD_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_auto_tol / 1000000000.0 / (double) NITER, prec_tol);
	}

	public static void benchmarkDoubleHyBR_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking DoubleHyBR_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			DoubleHyBROptions options = new DoubleHyBROptions(InSolvType.TIKHONOV, RegMethodType.ADAPTWGCV, -1, -1, false, 2, 0);
			elapsedTime_deblur = System.nanoTime();
			DoubleHyBR_3D hybr = new DoubleHyBR_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.NONE, -1, MAXITER, options, false);
			ImagePlus imX = hybr.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			hybr = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleHyBR_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoublePHyBR_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_auto_tol = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_auto_tol = 0;
		System.out.println("Benchmarking preconditioned DoubleHyBR_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			DoubleHyBROptions options = new DoubleHyBROptions(InSolvType.TIKHONOV, RegMethodType.ADAPTWGCV, -1, -1, false, 2, 0);
			elapsedTime_deblur = System.nanoTime();
			DoubleHyBR_3D hybr = new DoubleHyBR_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, prec_tol, MAXITER, options, false);
			ImagePlus imX = hybr.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_auto_tol = System.nanoTime();
			hybr = new DoubleHyBR_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, -1, MAXITER, options, false);
			imX = hybr.deblur(-1);
			elapsedTime_deblur_auto_tol = System.nanoTime() - elapsedTime_deblur_auto_tol;
			av_time_deblur_auto_tol = av_time_deblur_auto_tol + elapsedTime_deblur_auto_tol;

			hybr = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (tol =  " + prec_tol + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (tol = auto): " + String.format(format, av_time_deblur_auto_tol / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoublePHyBR_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_auto_tol / 1000000000.0 / (double) NITER, prec_tol);
	}

	public static void benchmarkDoubleDAMAS_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking DoubleDAMAS_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			DoubleWPLOptions options = new DoubleWPLOptions(0, 1.0, 1.0, 1.0, true, false, false, 0, false, false);
			elapsedTime_deblur = System.nanoTime();
			DoubleWPL_3D damas = new DoubleWPL_3D(blurImage, psfImage[0][0][0], boundary, resizing, output, MAXITER, options, false);
			ImagePlus imX = damas.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			damas = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoubleDAMAS_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkDoublePDAMAS_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking DoublePDAMAS_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			DoubleWPLOptions options = new DoubleWPLOptions(0.001, 1.0, 1.0, 1.0, true, false, false, 0, false, false);
			elapsedTime_deblur = System.nanoTime();
			DoubleWPL_3D damas = new DoubleWPL_3D(blurImage, psfImage[0][0][0], boundary, resizing, output, MAXITER, options, false);
			ImagePlus imX = damas.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			damas = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("DoublePDAMAS_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatCGLS_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking FloatCGLS_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatCGLS_3D cgls = new FloatCGLS_3D(blurImage, psfImage, boundary, resizing, output, MAXITER, 0, false, false);
			ImagePlus imX = cgls.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			cgls = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatCGLS_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatPCGLS_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_auto_tol = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_auto_tol = 0;
		System.out.println("Benchmarking FloatPCGLS_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatPCGLS_3D pcgls = new FloatPCGLS_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, (float) prec_tol, MAXITER, 0, false, false);
			ImagePlus imX = pcgls.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_auto_tol = System.nanoTime();
			pcgls = new FloatPCGLS_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, -1, MAXITER, 0, false, false);
			imX = pcgls.deblur(-1);
			elapsedTime_deblur_auto_tol = System.nanoTime() - elapsedTime_deblur_auto_tol;
			av_time_deblur_auto_tol = av_time_deblur_auto_tol + elapsedTime_deblur_auto_tol;

			pcgls = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (tol =  " + prec_tol + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (tol = auto): " + String.format(format, av_time_deblur_auto_tol / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatPCGLS_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_auto_tol / 1000000000.0 / (double) NITER, prec_tol);
	}

	public static void benchmarkFloatMRNSD_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking FloatMRNSD_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatMRNSD_3D mrnsd = new FloatMRNSD_3D(blurImage, psfImage, boundary, resizing, output, MAXITER, 0, false, false);
			ImagePlus imX = mrnsd.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			mrnsd = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatMRNSD_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatPMRNSD_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_auto_tol = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_auto_tol = 0;
		System.out.println("Benchmarking FloatPMRNSD_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			elapsedTime_deblur = System.nanoTime();
			FloatPMRNSD_3D pmrnsd = new FloatPMRNSD_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, (float) prec_tol, MAXITER, 0, false, false);
			ImagePlus imX = pmrnsd.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_auto_tol = System.nanoTime();
			pmrnsd = new FloatPMRNSD_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, -1, MAXITER, 0, false, false);
			imX = pmrnsd.deblur(-1);
			elapsedTime_deblur_auto_tol = System.nanoTime() - elapsedTime_deblur_auto_tol;
			av_time_deblur_auto_tol = av_time_deblur_auto_tol + elapsedTime_deblur_auto_tol;

			pmrnsd = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (tol =  " + prec_tol + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (tol = auto): " + String.format(format, av_time_deblur_auto_tol / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatPMRNSD_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_auto_tol / 1000000000.0 / (double) NITER, prec_tol);
	}

	public static void benchmarkFloatHyBR_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking FloatHyBR_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			FloatHyBROptions options = new FloatHyBROptions(InSolvType.TIKHONOV, RegMethodType.ADAPTWGCV, -1, -1, false, 2, 0);
			elapsedTime_deblur = System.nanoTime();
			FloatHyBR_3D hybr = new FloatHyBR_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.NONE, -1, MAXITER, options, false);
			ImagePlus imX = hybr.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			hybr = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatHyBR_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatPHyBR_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		double av_time_deblur_auto_tol = 0;
		long elapsedTime_deblur = 0;
		long elapsedTime_deblur_auto_tol = 0;
		System.out.println("Benchmarking preconditioned FloatHyBR_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			FloatHyBROptions options = new FloatHyBROptions(InSolvType.TIKHONOV, RegMethodType.ADAPTWGCV, -1, -1, false, 2, 0);
			elapsedTime_deblur = System.nanoTime();
			FloatHyBR_3D hybr = new FloatHyBR_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, (float) prec_tol, MAXITER, options, false);
			ImagePlus imX = hybr.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;

			elapsedTime_deblur_auto_tol = System.nanoTime();
			hybr = new FloatHyBR_3D(blurImage, psfImage, boundary, resizing, output, PreconditionerType.FFT, -1, MAXITER, options, false);
			imX = hybr.deblur(-1);
			elapsedTime_deblur_auto_tol = System.nanoTime() - elapsedTime_deblur_auto_tol;
			av_time_deblur_auto_tol = av_time_deblur_auto_tol + elapsedTime_deblur_auto_tol;

			hybr = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time (tol =  " + prec_tol + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		System.out.println("Average execution time (tol = auto): " + String.format(format, av_time_deblur_auto_tol / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatPHyBR_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_auto_tol / 1000000000.0 / (double) NITER, prec_tol);
	}

	public static void benchmarkFloatDAMAS_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking FloatDAMAS_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			FloatWPLOptions options = new FloatWPLOptions(0, 1.0f, 1.0f, 1.0f, true, false, false, 0, false, false);
			elapsedTime_deblur = System.nanoTime();
			FloatWPL_3D damas = new FloatWPL_3D(blurImage, psfImage[0][0][0], boundary, resizing, output, MAXITER, options, false);
			ImagePlus imX = damas.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			damas = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatDAMAS_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void benchmarkFloatPDAMAS_3D(int threads) {
		ConcurrencyUtils.setNumberOfProcessors(threads);
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + blur_image);
		ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
		psfImage[0][0][0] = o.openImage(path + psf_image);
		double av_time_deblur = 0;
		long elapsedTime_deblur = 0;
		System.out.println("Benchmarking FloatPDAMAS_3D using " + threads + " threads");
		for (int i = 0; i < NITER; i++) {
			FloatWPLOptions options = new FloatWPLOptions(0.001f, 1.0f, 1.0f, 1.0f, true, false, false, 0, false, false);
			elapsedTime_deblur = System.nanoTime();
			FloatWPL_3D damas = new FloatWPL_3D(blurImage, psfImage[0][0][0], boundary, resizing, output, MAXITER, options, false);
			ImagePlus imX = damas.deblur(-1);
			elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
			av_time_deblur = av_time_deblur + elapsedTime_deblur;
			damas = null;
			imX = null;
			System.gc();
		}
		blurImage = null;
		psfImage = null;
		System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
		writeResultsToFile("FloatPDAMAS_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
	}

	public static void writeResultsToFile(String filename, double time_deblur) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			out.write(new Date().toString());
			out.newLine();
			out.write("Number of processors: " + ConcurrencyUtils.getNumberOfProcessors());
			out.newLine();
			out.write("deblur time: ");
			out.write(String.format(format, time_deblur));
			out.write(" seconds");
			out.newLine();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeResultsToFile(String filename, double time_deblur, double time_deblur_auto_tol, double prec_tol) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			out.write(new Date().toString());
			out.newLine();
			out.write("Number of processors: " + ConcurrencyUtils.getNumberOfProcessors());
			out.newLine();
			out.write("deblur time (tol = " + prec_tol + "): ");
			out.write(String.format(format, time_deblur));
			out.write(" seconds");
			out.newLine();
			out.write("deblur time (tol = auto): ");
			out.write(String.format(format, time_deblur_auto_tol));
			out.write(" seconds");
			out.newLine();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		// benchmarkDoubleCGLS_3D(1);
		// System.gc();
		// benchmarkDoubleCGLS_3D(2);
		// System.gc();
		// benchmarkDoubleCGLS_3D(4);
		// System.gc();
		// benchmarkDoubleCGLS_3D(8);
		// System.gc();
		// benchmarkDoublePCGLS_3D(1);
		// System.gc();
		// benchmarkDoublePCGLS_3D(2);
		// System.gc();
		// benchmarkDoublePCGLS_3D(4);
		// System.gc();
		// benchmarkDoublePCGLS_3D(8);
		// System.gc();
		// benchmarkDoubleMRNSD_3D(1);
		// System.gc();
		// benchmarkDoubleMRNSD_3D(2);
		// System.gc();
		// benchmarkDoubleMRNSD_3D(4);
		// System.gc();
		// benchmarkDoubleMRNSD_3D(8);
		// System.gc();
		// benchmarkDoublePMRNSD_3D(1);
		// System.gc();
		// benchmarkDoublePMRNSD_3D(2);
		// System.gc();
		// benchmarkDoublePMRNSD_3D(4);
		// System.gc();
		// benchmarkDoublePMRNSD_3D(8);
		// System.gc();
		// benchmarkDoubleHyBR_3D(1);
		// System.gc();
		// benchmarkDoubleHyBR_3D(2);
		// System.gc();
		// benchmarkDoubleHyBR_3D(4);
		// System.gc();
		// benchmarkDoubleHyBR_3D(8);
		// System.gc();
		// benchmarkDoublePHyBR_3D(1);
		// System.gc();
		// benchmarkDoublePHyBR_3D(2);
		// System.gc();
		// benchmarkDoublePHyBR_3D(4);
		// System.gc();
		// benchmarkDoublePHyBR_3D(8);
		// System.gc();

		// benchmarkFloatCGLS_3D(1);
		// System.gc();
		// benchmarkFloatCGLS_3D(2);
		// System.gc();
		// benchmarkFloatCGLS_3D(4);
		// System.gc();
		// benchmarkFloatCGLS_3D(8);
		// System.gc();
//		benchmarkFloatPCGLS_3D(1);
//		System.gc();
//		benchmarkFloatPCGLS_3D(2);
//		System.gc();
//		benchmarkFloatPCGLS_3D(4);
//		System.gc();
//		benchmarkFloatPCGLS_3D(8);
//		System.gc();
		// benchmarkFloatMRNSD_3D(1);
		// System.gc();
		// benchmarkFloatMRNSD_3D(2);
		// System.gc();
		// benchmarkFloatMRNSD_3D(4);
		// System.gc();
		// benchmarkFloatMRNSD_3D(8);
		// System.gc();
		benchmarkFloatPMRNSD_3D(1);
		System.gc();
		benchmarkFloatPMRNSD_3D(2);
		System.gc();
		benchmarkFloatPMRNSD_3D(4);
		System.gc();
		benchmarkFloatPMRNSD_3D(8);
		// System.gc();
		// benchmarkFloatHyBR_3D(1);
		// System.gc();
		// benchmarkFloatHyBR_3D(2);
		// System.gc();
		// benchmarkFloatHyBR_3D(4);
		// System.gc();
		// benchmarkFloatHyBR_3D(8);
		// System.gc();
//		benchmarkFloatPHyBR_3D(1);
//		System.gc();
//		benchmarkFloatPHyBR_3D(2);
//		System.gc();
//		benchmarkFloatPHyBR_3D(4);
//		System.gc();
//		benchmarkFloatPHyBR_3D(8);
//		System.gc();
//		benchmarkFloatPDAMAS_3D(1);
//		System.gc();
//		benchmarkFloatPDAMAS_3D(2);
//		System.gc();
//		benchmarkFloatPDAMAS_3D(4);
//		System.gc();
//		benchmarkFloatPDAMAS_3D(8);
		System.exit(0);

	}
}
