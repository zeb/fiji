package edu.emory.mathcs.restoretools.iterative;

import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;
import cern.colt.Timer;
import edu.emory.mathcs.restoretools.iterative.method.cgls.DoubleCGLS_2D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.DoubleCGLS_3D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.DoublePCGLS_2D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.DoublePCGLS_3D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.FloatCGLS_2D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.FloatCGLS_3D;
import edu.emory.mathcs.restoretools.iterative.method.cgls.FloatPCGLS_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.DoubleHyBR_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.DoubleHyBR_3D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.FloatHyBR_2D;
import edu.emory.mathcs.restoretools.iterative.method.hybr.FloatHyBR_3D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.DoubleMRNSD_2D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.DoublePMRNSD_2D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.DoublePMRNSD_3D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.FloatMRNSD_2D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.FloatMRNSD_3D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.FloatPMRNSD_2D;
import edu.emory.mathcs.restoretools.iterative.method.mrnsd.FloatPMRNSD_3D;
import edu.emory.mathcs.restoretools.iterative.preconditioner.PreconditionerType;

public class TestsIterative {
	private static final String path = "d:\\Research\\Images\\";

	public static void testCGLS_2D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "grain-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "grain-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		DoubleCGLS_2D cgls = new DoubleCGLS_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 4, -1, false, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = cgls.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatCGLS_2D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "satellite-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "satellite-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		FloatCGLS_2D cgls = new FloatCGLS_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 30, -1, false, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = cgls.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testCGLS_3D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "micro\\micro_blur.tif");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "micro\\micro_psf.tif");
		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][1];
		PSF[0][0][0] = psfImage;
		DoubleCGLS_3D cgls = new DoubleCGLS_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 2, -1, false, false);
		t.stop();
		double ctime = t.seconds();
		System.out.println("Constructor: " + ctime);
		t.reset().start();
		ImagePlus imX = cgls.deblur(0);
		t.stop();
		double dtime = t.seconds();
		System.out.println("Deblur: " + dtime);
		System.out.println("Total: " + (ctime + dtime));
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatCGLS_3D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head\\head_blur.tif");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "head\\head_psf.tif");
		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][1];
		PSF[0][0][0] = psfImage;
		FloatCGLS_3D cgls = new FloatCGLS_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 2, -1, false, false);
		t.stop();
		double ctime = t.seconds();
		System.out.println("Constructor: " + ctime);
		t.reset().start();
		ImagePlus imX = cgls.deblur(0);
		t.stop();
		double dtime = t.seconds();
		System.out.println("Deblur: " + dtime);
		System.out.println("Total: " + (ctime + dtime));
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testPCGLS_2D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "astronaut-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "astronaut-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		DoublePCGLS_2D pcgls = new DoublePCGLS_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 4, -1, false, false);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = pcgls.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPCGLS_2D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "io1024-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "io1024-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		FloatPCGLS_2D pcgls = new FloatPCGLS_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 4, 1e-7f, false, false);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = pcgls.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testPMRNSD_2D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "grain-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "grain-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		DoublePMRNSD_2D pmrnsd = new DoublePMRNSD_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 5, -1, false, true);
		t.stop();
		double ctime = t.seconds();
		System.out.println("Constructor: " + ctime);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		double dtime = t.seconds();
		System.out.println("Deblur: " + dtime);
		System.out.println("Total: " + (ctime + dtime));
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPMRNSD_2D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "grain-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "grain-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		FloatPMRNSD_2D pmrnsd = new FloatPMRNSD_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 5, -1, false, true);
		t.stop();
		double ctime = t.seconds();
		System.out.println("Constructor: " + ctime);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		double dtime = t.seconds();
		System.out.println("Deblur: " + dtime);
		System.out.println("Total: " + (ctime + dtime));
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testPMRNSD_3D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head_blur.tif");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "head_psf.tif");
		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][1];
		PSF[0][0][0] = psfImage;
		DoublePMRNSD_3D pmrnsd = new DoublePMRNSD_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, -1, false, true);
		t.stop();
		double ctime = t.seconds();
		System.out.println("Constructor: " + ctime);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		double dtime = t.seconds();
		System.out.println("Deblur: " + dtime);
		System.out.println("Total: " + (ctime + dtime));
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testPCGLS_3D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "micro\\micro_blur.tif");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "micro\\micro_psf.tif");
		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][1];
		PSF[0][0][0] = psfImage;
		DoublePCGLS_3D pcgls = new DoublePCGLS_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, -1, false, false);
		t.stop();
		double ctime = t.seconds();
		System.out.println("Constructor: " + ctime);
		t.reset().start();
		ImagePlus imX = pcgls.deblur(0);
		t.stop();
		double dtime = t.seconds();
		System.out.println("Deblur: " + dtime);
		System.out.println("Total: " + (ctime + dtime));
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPMRNSD_3D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head\\head_blur.tif");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "head\\head_psf.tif");
		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][1];
		PSF[0][0][0] = psfImage;
		FloatPMRNSD_3D pmrnsd = new FloatPMRNSD_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, -1, false, true);
		t.stop();
		double ctime = t.seconds();
		System.out.println("Constructor: " + ctime);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		double dtime = t.seconds();
		System.out.println("Deblur: " + dtime);
		System.out.println("Total: " + (ctime + dtime));
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatMRNSD_3D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "\\micro\\micro_blur.tif");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "\\micro\\micro_psf.tif");
		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][1];
		PSF[0][0][0] = psfImage;
		FloatMRNSD_3D mrnsd = new FloatMRNSD_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 4, -1, false, true);
		t.stop();
		double ctime = t.seconds();
		System.out.println("Constructor: " + ctime);
		t.reset().start();
		ImagePlus imX = mrnsd.deblur(0);
		t.stop();
		double dtime = t.seconds();
		System.out.println("Deblur: " + dtime);
		System.out.println("Total: " + (ctime + dtime));
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testPMRNSD_2D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "star_cluster\\" + "star-cluster-blur.fit");
		blurImage.show();
		ImagePlus psfImage00 = o.openImage(path + "star_cluster\\" + "mpsf00.fit");
		ImagePlus psfImage01 = o.openImage(path + "star_cluster\\" + "mpsf01.fit");
		ImagePlus psfImage02 = o.openImage(path + "star_cluster\\" + "mpsf02.fit");
		ImagePlus psfImage03 = o.openImage(path + "star_cluster\\" + "mpsf03.fit");
		ImagePlus psfImage04 = o.openImage(path + "star_cluster\\" + "mpsf04.fit");
		ImagePlus psfImage05 = o.openImage(path + "star_cluster\\" + "mpsf05.fit");
		ImagePlus psfImage06 = o.openImage(path + "star_cluster\\" + "mpsf06.fit");
		ImagePlus psfImage07 = o.openImage(path + "star_cluster\\" + "mpsf07.fit");
		ImagePlus psfImage08 = o.openImage(path + "star_cluster\\" + "mpsf08.fit");
		ImagePlus psfImage09 = o.openImage(path + "star_cluster\\" + "mpsf09.fit");
		ImagePlus psfImage10 = o.openImage(path + "star_cluster\\" + "mpsf10.fit");
		ImagePlus psfImage11 = o.openImage(path + "star_cluster\\" + "mpsf11.fit");
		ImagePlus psfImage12 = o.openImage(path + "star_cluster\\" + "mpsf12.fit");
		ImagePlus psfImage13 = o.openImage(path + "star_cluster\\" + "mpsf13.fit");
		ImagePlus psfImage14 = o.openImage(path + "star_cluster\\" + "mpsf14.fit");
		ImagePlus psfImage15 = o.openImage(path + "star_cluster\\" + "mpsf15.fit");
		ImagePlus psfImage16 = o.openImage(path + "star_cluster\\" + "mpsf16.fit");
		ImagePlus psfImage17 = o.openImage(path + "star_cluster\\" + "mpsf17.fit");
		ImagePlus psfImage18 = o.openImage(path + "star_cluster\\" + "mpsf18.fit");
		ImagePlus psfImage19 = o.openImage(path + "star_cluster\\" + "mpsf19.fit");
		ImagePlus psfImage20 = o.openImage(path + "star_cluster\\" + "mpsf20.fit");
		ImagePlus psfImage21 = o.openImage(path + "star_cluster\\" + "mpsf21.fit");
		ImagePlus psfImage22 = o.openImage(path + "star_cluster\\" + "mpsf22.fit");
		ImagePlus psfImage23 = o.openImage(path + "star_cluster\\" + "mpsf23.fit");
		ImagePlus psfImage24 = o.openImage(path + "star_cluster\\" + "mpsf24.fit");

		ImagePlus[][] PSF = new ImagePlus[5][5];
		PSF[0][0] = psfImage04;
		PSF[0][1] = psfImage09;
		PSF[0][2] = psfImage14;
		PSF[0][3] = psfImage19;
		PSF[0][4] = psfImage24;

		PSF[1][0] = psfImage03;
		PSF[1][1] = psfImage08;
		PSF[1][2] = psfImage13;
		PSF[1][3] = psfImage18;
		PSF[1][4] = psfImage23;

		PSF[2][0] = psfImage02;
		PSF[2][1] = psfImage07;
		PSF[2][2] = psfImage12;
		PSF[2][3] = psfImage17;
		PSF[2][4] = psfImage22;

		PSF[3][0] = psfImage01;
		PSF[3][1] = psfImage06;
		PSF[3][2] = psfImage11;
		PSF[3][3] = psfImage16;
		PSF[3][4] = psfImage21;

		PSF[4][0] = psfImage00;
		PSF[4][1] = psfImage05;
		PSF[4][2] = psfImage10;
		PSF[4][3] = psfImage15;
		PSF[4][4] = psfImage20;

		Timer t = new Timer().start();
		DoublePMRNSD_2D pmrnsd = new DoublePMRNSD_2D(blurImage, PSF, BoundaryType.REFLEXIVE,  ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 39, -1, false, false);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPMRNSD_2D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "star_cluster\\" + "star-cluster-blur.fit");
		blurImage.show();
		ImagePlus psfImage00 = o.openImage(path + "star_cluster\\" + "mpsf00.fit");
		ImagePlus psfImage01 = o.openImage(path + "star_cluster\\" + "mpsf01.fit");
		ImagePlus psfImage02 = o.openImage(path + "star_cluster\\" + "mpsf02.fit");
		ImagePlus psfImage03 = o.openImage(path + "star_cluster\\" + "mpsf03.fit");
		ImagePlus psfImage04 = o.openImage(path + "star_cluster\\" + "mpsf04.fit");
		ImagePlus psfImage05 = o.openImage(path + "star_cluster\\" + "mpsf05.fit");
		ImagePlus psfImage06 = o.openImage(path + "star_cluster\\" + "mpsf06.fit");
		ImagePlus psfImage07 = o.openImage(path + "star_cluster\\" + "mpsf07.fit");
		ImagePlus psfImage08 = o.openImage(path + "star_cluster\\" + "mpsf08.fit");
		ImagePlus psfImage09 = o.openImage(path + "star_cluster\\" + "mpsf09.fit");
		ImagePlus psfImage10 = o.openImage(path + "star_cluster\\" + "mpsf10.fit");
		ImagePlus psfImage11 = o.openImage(path + "star_cluster\\" + "mpsf11.fit");
		ImagePlus psfImage12 = o.openImage(path + "star_cluster\\" + "mpsf12.fit");
		ImagePlus psfImage13 = o.openImage(path + "star_cluster\\" + "mpsf13.fit");
		ImagePlus psfImage14 = o.openImage(path + "star_cluster\\" + "mpsf14.fit");
		ImagePlus psfImage15 = o.openImage(path + "star_cluster\\" + "mpsf15.fit");
		ImagePlus psfImage16 = o.openImage(path + "star_cluster\\" + "mpsf16.fit");
		ImagePlus psfImage17 = o.openImage(path + "star_cluster\\" + "mpsf17.fit");
		ImagePlus psfImage18 = o.openImage(path + "star_cluster\\" + "mpsf18.fit");
		ImagePlus psfImage19 = o.openImage(path + "star_cluster\\" + "mpsf19.fit");
		ImagePlus psfImage20 = o.openImage(path + "star_cluster\\" + "mpsf20.fit");
		ImagePlus psfImage21 = o.openImage(path + "star_cluster\\" + "mpsf21.fit");
		ImagePlus psfImage22 = o.openImage(path + "star_cluster\\" + "mpsf22.fit");
		ImagePlus psfImage23 = o.openImage(path + "star_cluster\\" + "mpsf23.fit");
		ImagePlus psfImage24 = o.openImage(path + "star_cluster\\" + "mpsf24.fit");

		ImagePlus[][] PSF = new ImagePlus[5][5];
		PSF[0][0] = psfImage04;
		PSF[0][1] = psfImage09;
		PSF[0][2] = psfImage14;
		PSF[0][3] = psfImage19;
		PSF[0][4] = psfImage24;

		PSF[1][0] = psfImage03;
		PSF[1][1] = psfImage08;
		PSF[1][2] = psfImage13;
		PSF[1][3] = psfImage18;
		PSF[1][4] = psfImage23;

		PSF[2][0] = psfImage02;
		PSF[2][1] = psfImage07;
		PSF[2][2] = psfImage12;
		PSF[2][3] = psfImage17;
		PSF[2][4] = psfImage22;

		PSF[3][0] = psfImage01;
		PSF[3][1] = psfImage06;
		PSF[3][2] = psfImage11;
		PSF[3][3] = psfImage16;
		PSF[3][4] = psfImage21;

		PSF[4][0] = psfImage00;
		PSF[4][1] = psfImage05;
		PSF[4][2] = psfImage10;
		PSF[4][3] = psfImage15;
		PSF[4][4] = psfImage20;

		Timer t = new Timer().start();
		FloatPMRNSD_2D pmrnsd = new FloatPMRNSD_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 39, -1, false, false);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPCGLS_2D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "star_cluster\\" + "star-cluster-blur.fit");
		blurImage.show();
		ImagePlus psfImage00 = o.openImage(path + "star_cluster\\" + "mpsf00.fit");
		ImagePlus psfImage01 = o.openImage(path + "star_cluster\\" + "mpsf01.fit");
		ImagePlus psfImage02 = o.openImage(path + "star_cluster\\" + "mpsf02.fit");
		ImagePlus psfImage03 = o.openImage(path + "star_cluster\\" + "mpsf03.fit");
		ImagePlus psfImage04 = o.openImage(path + "star_cluster\\" + "mpsf04.fit");
		ImagePlus psfImage05 = o.openImage(path + "star_cluster\\" + "mpsf05.fit");
		ImagePlus psfImage06 = o.openImage(path + "star_cluster\\" + "mpsf06.fit");
		ImagePlus psfImage07 = o.openImage(path + "star_cluster\\" + "mpsf07.fit");
		ImagePlus psfImage08 = o.openImage(path + "star_cluster\\" + "mpsf08.fit");
		ImagePlus psfImage09 = o.openImage(path + "star_cluster\\" + "mpsf09.fit");
		ImagePlus psfImage10 = o.openImage(path + "star_cluster\\" + "mpsf10.fit");
		ImagePlus psfImage11 = o.openImage(path + "star_cluster\\" + "mpsf11.fit");
		ImagePlus psfImage12 = o.openImage(path + "star_cluster\\" + "mpsf12.fit");
		ImagePlus psfImage13 = o.openImage(path + "star_cluster\\" + "mpsf13.fit");
		ImagePlus psfImage14 = o.openImage(path + "star_cluster\\" + "mpsf14.fit");
		ImagePlus psfImage15 = o.openImage(path + "star_cluster\\" + "mpsf15.fit");
		ImagePlus psfImage16 = o.openImage(path + "star_cluster\\" + "mpsf16.fit");
		ImagePlus psfImage17 = o.openImage(path + "star_cluster\\" + "mpsf17.fit");
		ImagePlus psfImage18 = o.openImage(path + "star_cluster\\" + "mpsf18.fit");
		ImagePlus psfImage19 = o.openImage(path + "star_cluster\\" + "mpsf19.fit");
		ImagePlus psfImage20 = o.openImage(path + "star_cluster\\" + "mpsf20.fit");
		ImagePlus psfImage21 = o.openImage(path + "star_cluster\\" + "mpsf21.fit");
		ImagePlus psfImage22 = o.openImage(path + "star_cluster\\" + "mpsf22.fit");
		ImagePlus psfImage23 = o.openImage(path + "star_cluster\\" + "mpsf23.fit");
		ImagePlus psfImage24 = o.openImage(path + "star_cluster\\" + "mpsf24.fit");

		ImagePlus[][] PSF = new ImagePlus[5][5];
		PSF[0][0] = psfImage04;
		PSF[0][1] = psfImage09;
		PSF[0][2] = psfImage14;
		PSF[0][3] = psfImage19;
		PSF[0][4] = psfImage24;

		PSF[1][0] = psfImage03;
		PSF[1][1] = psfImage08;
		PSF[1][2] = psfImage13;
		PSF[1][3] = psfImage18;
		PSF[1][4] = psfImage23;

		PSF[2][0] = psfImage02;
		PSF[2][1] = psfImage07;
		PSF[2][2] = psfImage12;
		PSF[2][3] = psfImage17;
		PSF[2][4] = psfImage22;

		PSF[3][0] = psfImage01;
		PSF[3][1] = psfImage06;
		PSF[3][2] = psfImage11;
		PSF[3][3] = psfImage16;
		PSF[3][4] = psfImage21;

		PSF[4][0] = psfImage00;
		PSF[4][1] = psfImage05;
		PSF[4][2] = psfImage10;
		PSF[4][3] = psfImage15;
		PSF[4][4] = psfImage20;

		Timer t = new Timer().start();
		FloatPCGLS_2D pmrnsd = new FloatPCGLS_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 39, -1, false, false);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPHyBR_2D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "star_cluster\\" + "star-cluster-blur.fit");
		blurImage.show();
		ImagePlus psfImage00 = o.openImage(path + "star_cluster\\" + "mpsf00.fit");
		ImagePlus psfImage01 = o.openImage(path + "star_cluster\\" + "mpsf01.fit");
		ImagePlus psfImage02 = o.openImage(path + "star_cluster\\" + "mpsf02.fit");
		ImagePlus psfImage03 = o.openImage(path + "star_cluster\\" + "mpsf03.fit");
		ImagePlus psfImage04 = o.openImage(path + "star_cluster\\" + "mpsf04.fit");
		ImagePlus psfImage05 = o.openImage(path + "star_cluster\\" + "mpsf05.fit");
		ImagePlus psfImage06 = o.openImage(path + "star_cluster\\" + "mpsf06.fit");
		ImagePlus psfImage07 = o.openImage(path + "star_cluster\\" + "mpsf07.fit");
		ImagePlus psfImage08 = o.openImage(path + "star_cluster\\" + "mpsf08.fit");
		ImagePlus psfImage09 = o.openImage(path + "star_cluster\\" + "mpsf09.fit");
		ImagePlus psfImage10 = o.openImage(path + "star_cluster\\" + "mpsf10.fit");
		ImagePlus psfImage11 = o.openImage(path + "star_cluster\\" + "mpsf11.fit");
		ImagePlus psfImage12 = o.openImage(path + "star_cluster\\" + "mpsf12.fit");
		ImagePlus psfImage13 = o.openImage(path + "star_cluster\\" + "mpsf13.fit");
		ImagePlus psfImage14 = o.openImage(path + "star_cluster\\" + "mpsf14.fit");
		ImagePlus psfImage15 = o.openImage(path + "star_cluster\\" + "mpsf15.fit");
		ImagePlus psfImage16 = o.openImage(path + "star_cluster\\" + "mpsf16.fit");
		ImagePlus psfImage17 = o.openImage(path + "star_cluster\\" + "mpsf17.fit");
		ImagePlus psfImage18 = o.openImage(path + "star_cluster\\" + "mpsf18.fit");
		ImagePlus psfImage19 = o.openImage(path + "star_cluster\\" + "mpsf19.fit");
		ImagePlus psfImage20 = o.openImage(path + "star_cluster\\" + "mpsf20.fit");
		ImagePlus psfImage21 = o.openImage(path + "star_cluster\\" + "mpsf21.fit");
		ImagePlus psfImage22 = o.openImage(path + "star_cluster\\" + "mpsf22.fit");
		ImagePlus psfImage23 = o.openImage(path + "star_cluster\\" + "mpsf23.fit");
		ImagePlus psfImage24 = o.openImage(path + "star_cluster\\" + "mpsf24.fit");

		ImagePlus[][] PSF = new ImagePlus[5][5];
		PSF[0][0] = psfImage04;
		PSF[0][1] = psfImage09;
		PSF[0][2] = psfImage14;
		PSF[0][3] = psfImage19;
		PSF[0][4] = psfImage24;

		PSF[1][0] = psfImage03;
		PSF[1][1] = psfImage08;
		PSF[1][2] = psfImage13;
		PSF[1][3] = psfImage18;
		PSF[1][4] = psfImage23;

		PSF[2][0] = psfImage02;
		PSF[2][1] = psfImage07;
		PSF[2][2] = psfImage12;
		PSF[2][3] = psfImage17;
		PSF[2][4] = psfImage22;

		PSF[3][0] = psfImage01;
		PSF[3][1] = psfImage06;
		PSF[3][2] = psfImage11;
		PSF[3][3] = psfImage16;
		PSF[3][4] = psfImage21;

		PSF[4][0] = psfImage00;
		PSF[4][1] = psfImage05;
		PSF[4][2] = psfImage10;
		PSF[4][3] = psfImage15;
		PSF[4][4] = psfImage20;

		Timer t = new Timer().start();
		FloatHyBR_2D hybr = new FloatHyBR_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 39, null, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = hybr.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testMRNSD_2D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "grain-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "grain-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		DoubleMRNSD_2D mrnsd = new DoubleMRNSD_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 5, -1, false, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = mrnsd.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatMRNSD_2D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "grain-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "grain-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		FloatMRNSD_2D mrnsd = new FloatMRNSD_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 20, -1, false, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = mrnsd.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testHyBR() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "grain-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "grain-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		DoubleHyBR_2D hybr = new DoubleHyBR_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.NONE, -1, 10, null, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = hybr.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testPHyBR() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "grain-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "grain-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		DoubleHyBR_2D hybr = new DoubleHyBR_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 4, null, false);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = hybr.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatHyBR() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "grain-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "grain-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		FloatHyBR_2D hybr = new FloatHyBR_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.NONE, -1, 10, null, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = hybr.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPHyBR() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "io1024-blur.png");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "io1024-psf.png");
		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[1][1];
		PSF[0][0] = psfImage;
		FloatHyBR_2D hybr = new FloatHyBR_2D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 4, null, false);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = hybr.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPHyBR_3D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head\\head_blur.tif");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "head\\head_psf.tif");
		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][1];
		PSF[0][0][0] = psfImage;
		FloatHyBR_3D hybr = new FloatHyBR_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, null, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = hybr.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testPHyBR_3D() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head\\head_blur.tif");
		blurImage.show();
		ImagePlus psfImage = o.openImage(path + "head\\head_psf.tif");
		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][1];
		PSF[0][0][0] = psfImage;
		DoubleHyBR_3D hybr = new DoubleHyBR_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, null, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = hybr.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testCGLS_2D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "star_cluster_blur.jpg");
		blurImage.show();
		ImagePlus psfImage00 = o.openImage(path + "star_cluster_psf00.jpg");
		ImagePlus psfImage01 = o.openImage(path + "star_cluster_psf01.jpg");
		ImagePlus psfImage10 = o.openImage(path + "star_cluster_psf10.jpg");
		ImagePlus psfImage11 = o.openImage(path + "star_cluster_psf11.jpg");

		Timer t = new Timer().start();
		ImagePlus[][] PSF = new ImagePlus[2][2];
		PSF[0][0] = psfImage00;
		PSF[0][1] = psfImage01;
		PSF[1][0] = psfImage10;
		PSF[1][1] = psfImage11;

		DoubleCGLS_2D cgls = new DoubleCGLS_2D(blurImage, PSF, BoundaryType.ZERO, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, 30, -1, false, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = cgls.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatPMRNSD_3D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head\\head_blur.tif");
		blurImage.show();
		ImagePlus psfImage000 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage001 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage010 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage011 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage100 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage101 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage110 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage111 = o.openImage(path + "head\\head_psf.tif");

		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[2][1][1];
		PSF[0][0][0] = psfImage000;
		// PSF[0][0][1] = psfImage001;
		// PSF[0][1][0] = psfImage010;
		// PSF[0][1][1] = psfImage011;
		// PSF[1][0][0] = psfImage100;
		// PSF[1][0][1] = psfImage101;
		// PSF[1][1][0] = psfImage110;
		PSF[1][0][0] = psfImage111;

		FloatPMRNSD_3D pmrnsd = new FloatPMRNSD_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, -1, false, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testDoublePMRNSD_3D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head\\head_blur.tif");
		blurImage.show();
		ImagePlus psfImage000 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage001 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage010 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage011 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage100 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage101 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage110 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage111 = o.openImage(path + "head\\head_psf.tif");

		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[2][1][1];
		PSF[0][0][0] = psfImage000;
		// PSF[0][0][1] = psfImage001;
		// PSF[0][1][0] = psfImage010;
		// PSF[0][1][1] = psfImage011;
		// PSF[1][0][0] = psfImage100;
		// PSF[1][0][1] = psfImage101;
		// PSF[1][1][0] = psfImage110;
		PSF[1][0][0] = psfImage111;

		DoublePMRNSD_3D pmrnsd = new DoublePMRNSD_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, -1, false, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testPMRNSD_3D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head\\head_blur.tif");
		blurImage.show();
		ImagePlus psfImage000 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage001 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage010 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage011 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage100 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage101 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage110 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage111 = o.openImage(path + "head\\head_psf.tif");

		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[2][1][1];
		PSF[0][0][0] = psfImage000;
		// PSF[0][0][1] = psfImage001;
		// PSF[0][1][0] = psfImage010;
		// PSF[0][1][1] = psfImage011;
		// PSF[1][0][0] = psfImage100;
		// PSF[1][0][1] = psfImage101;
		// PSF[1][1][0] = psfImage110;
		PSF[1][0][0] = psfImage111;

		DoublePMRNSD_3D pmrnsd = new DoublePMRNSD_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, -1, false, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = pmrnsd.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void testFloatHyBR_3D_variant() {
		Opener o = new Opener();
		ImagePlus blurImage = o.openImage(path + "head\\head_blur.tif");
		blurImage.show();
		ImagePlus psfImage000 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage001 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage010 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage011 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage100 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage101 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage110 = o.openImage(path + "head\\head_psf.tif");
		ImagePlus psfImage111 = o.openImage(path + "head\\head_psf.tif");

		Timer t = new Timer().start();
		ImagePlus[][][] PSF = new ImagePlus[1][1][2];
		PSF[0][0][0] = psfImage000;
		// PSF[0][0][1] = psfImage001;
		// PSF[0][1][0] = psfImage010;
		// PSF[0][1][1] = psfImage011;
		// PSF[1][0][0] = psfImage100;
		// PSF[1][0][1] = psfImage101;
		// PSF[1][1][0] = psfImage110;
		PSF[0][0][1] = psfImage111;

		FloatHyBR_3D hybr = new FloatHyBR_3D(blurImage, PSF, BoundaryType.REFLEXIVE, ResizingType.AUTO, OutputType.SAME_AS_SOURCE, PreconditionerType.FFT, -1, 2, null, true);
		t.stop();
		System.out.println("Constructor: " + t);
		t.reset().start();
		ImagePlus imX = hybr.deblur(0);
		t.stop();
		System.out.println("Deblur: " + t);
		System.out.println("niter=" + imX.getProperty("niter"));
		imX.show();
	}

	public static void main(String[] args) {
		new ImageJ();
		// cern.colt.ConcurrencyUtils.setNumberOfProcessors(1);
//		testFloatPHyBR_2D_variant();
		testFloatPMRNSD_3D_variant();
	}

}
