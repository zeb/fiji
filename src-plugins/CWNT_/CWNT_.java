import fiji.plugin.nucleitracker.NucleiMasker;
import fiji.plugin.nucleitracker.gui.TuneParametersPanel;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.JFrame;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJVirtualStack;
import mpicbg.imglib.type.numeric.IntegerType;
import mpicbg.imglib.type.numeric.real.FloatType;

public class CWNT_ implements PlugIn {

	/*
	 * FIELDS
	 */

	private NucleiMasker<? extends IntegerType<?>> algo;
	private ImagePlus comp2;
	private ImagePlus comp1;
	private TuneParametersPanel panel;
	private final static int WIDTH = 360;
	private final static int HEIGHT = 530;
	private static final double[] DEFAULT_PARAM = NucleiMasker.DEFAULT_MASKING_PARAMETERS;
	private int stepUpdateToPerform = Integer.MAX_VALUE;
	private DisplayUpdater updater = new DisplayUpdater();

	
	
	@Override
	public void run(String arg) {

		// Get current image sample
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (null == imp)
			return;

		// Create Panel silently
		panel = new TuneParametersPanel(DEFAULT_PARAM);

		// Prepare target imps
		recomputeSampleWindows(imp);

		// Create GUI
		JFrame frame = new JFrame("Test parameters for CWNT");
		frame.getContentPane().add(panel);
		frame.setBounds(100, 100, WIDTH, HEIGHT);
		frame.setVisible(true);

		// Add listeners
		imp.getCanvas().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (panel.getSelectedIndex() <= 1)
					recomputeSampleWindows(imp);
			}
		});

		frame.addWindowListener(new WindowListener() {
			public void windowOpened(WindowEvent e) { }
			public void windowIconified(WindowEvent e) { }
			public void windowDeiconified(WindowEvent e) { }
			public void windowDeactivated(WindowEvent e) { }
			public void windowClosing(WindowEvent e) { }
			public void windowClosed(WindowEvent e) { updater.quit(); }
			public void windowActivated(WindowEvent e) {}
		});

		panel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (e == panel.STEP1_PARAMETER_CHANGED) {
					stepUpdateToPerform = Math.min(1, stepUpdateToPerform);
					updater.doUpdate();
				} else if (e == panel.STEP2_PARAMETER_CHANGED) {
					stepUpdateToPerform = Math.min(2, stepUpdateToPerform);
					updater.doUpdate();
				} else if (e == panel.STEP3_PARAMETER_CHANGED) {
					stepUpdateToPerform = Math.min(3, stepUpdateToPerform);
					updater.doUpdate();
				} else if (e == panel.STEP4_PARAMETER_CHANGED) {
					stepUpdateToPerform = Math.min(4, stepUpdateToPerform);
					updater.doUpdate();
				} else {
					System.err.println("Unknwon event caught: "+e);
				}
			}
		});



	}

	
	
	private FloatProcessor toFloatProcessor(@SuppressWarnings("rawtypes") Image img) {
		@SuppressWarnings("unchecked")
		FloatProcessor fip = new FloatProcessor( img.getDimension(0), img.getDimension(1), 
				ImageJVirtualStack.extractSliceFloat( img, img.getDisplay(), 0, 1, new int[3] ), null); 
		fip.setMinAndMax( img.getDisplay().getMin(),  img.getDisplay().getMax() );
		return fip;
	}


	private void paramStep1Changed() {
		// We have to redo all.
		algo.setParameters(panel.getParameters());
		algo.execStep1(); 
		algo.execStep2(); 
		algo.execStep3(); 
		algo.execStep4();

		int slice1 = comp1.getSlice();
		comp1.setSlice(1);
		comp1.setProcessor(toFloatProcessor(algo.getGaussianFilteredImage()));
		comp1.setSlice(2);
		comp1.setProcessor(toFloatProcessor(algo.getAnisotropicDiffusionImage()));
		comp1.setSlice(3);
		comp1.setProcessor(toFloatProcessor(algo.getResult()));
		comp1.setSlice(slice1);

		int slice2 = comp2.getSlice();
		comp2.setSlice(1);
		comp2.setProcessor(toFloatProcessor(algo.getGradientNorm()));
		comp2.setSlice(2);
		comp2.setProcessor(toFloatProcessor(algo.getLaplacianMagnitude()));
		comp2.setSlice(3);
		comp2.setProcessor(toFloatProcessor(algo.getHessianDeterminant()));
		comp2.setSlice(4);
		comp2.setProcessor(toFloatProcessor(algo.getMask()));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}

	private void paramStep2Changed() {
		algo.setParameters(panel.getParameters());
		algo.execStep2(); 
		algo.execStep3(); 
		algo.execStep4(); 

		int slice1 = comp1.getSlice();
		comp1.setSlice(2);
		comp1.setProcessor(toFloatProcessor(algo.getAnisotropicDiffusionImage()));
		comp1.setSlice(3);
		comp1.setProcessor(toFloatProcessor(algo.getResult()));
		comp1.setSlice(slice1);

		int slice2 = comp2.getSlice();
		comp2.setSlice(1);
		comp2.setProcessor(toFloatProcessor(algo.getGradientNorm()));
		comp2.setSlice(2);
		comp2.setProcessor(toFloatProcessor(algo.getLaplacianMagnitude()));
		comp2.setSlice(3);
		comp2.setProcessor(toFloatProcessor(algo.getHessianDeterminant()));
		comp2.setSlice(4);
		comp2.setProcessor(toFloatProcessor(algo.getMask()));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}

	private void paramStep3Changed() {
		algo.setParameters(panel.getParameters());
		algo.execStep3(); 
		algo.execStep4(); 

		int slice1 = comp1.getSlice();
		comp1.setSlice(3);
		comp1.setProcessor(toFloatProcessor(algo.getResult()));
		comp1.setSlice(slice1);

		int slice2 = comp2.getSlice();
		comp2.setSlice(1);
		comp2.setProcessor(toFloatProcessor(algo.getGradientNorm()));
		comp2.setSlice(2);
		comp2.setProcessor(toFloatProcessor(algo.getLaplacianMagnitude()));
		comp2.setSlice(3);
		comp2.setProcessor(toFloatProcessor(algo.getHessianDeterminant()));
		comp2.setSlice(4);
		comp2.setProcessor(toFloatProcessor(algo.getMask()));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}

	private void paramStep4Changed() {
		algo.setParameters(panel.getParameters());
		algo.execStep4(); 

		int slice1 = comp1.getSlice();
		comp1.setSlice(3);
		comp1.setProcessor(toFloatProcessor(algo.getResult()));
		comp1.setSlice(slice1);

		int slice2 = comp2.getSlice();
		comp2.setSlice(4);
		comp2.setProcessor(toFloatProcessor(algo.getMask()));
		comp2.setSlice(slice2);
		comp2.getProcessor().setMinAndMax(0, 2);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void recomputeSampleWindows(ImagePlus imp) {
		
		ImagePlus snip = new Duplicator().run(imp, imp.getSlice(), imp.getSlice());

		// Copy to Imglib
		Image<? extends IntegerType<?>> img = null;
		switch( imp.getType() )	{		
		case ImagePlus.GRAY8 : 
			img =  ImagePlusAdapter.wrapByte( snip );
			break;
		case ImagePlus.GRAY16 : 
			img = ImagePlusAdapter.wrapShort( snip );
			break;
		default:
			System.err.println("Image type not handled: "+imp.getType());
			return;
		}

		// Prepare algo
		algo = new NucleiMasker(img);
		algo.setParameters(panel.getParameters());
		boolean check = algo.checkInput() && algo.process();
		if (!check) {
			System.err.println("Problem with the segmenter: "+algo.getErrorMessage());
			return;
		}

		// Prepare results holder;
		Image 				F 	= algo.getGaussianFilteredImage();
		Image 				AD	= algo.getAnisotropicDiffusionImage();
		Image<FloatType> 	G 	= algo.getGradientNorm();
		Image<FloatType> 	L 	= algo.getLaplacianMagnitude();
		Image<FloatType> 	H 	= algo.getHessianDeterminant();
		Image<FloatType> 	M 	= algo.getMask();
		Image 				R 	= algo.getResult();

		int width = F.getDimension(0);
		int height = F.getDimension(1);

		ImageStack floatStack = new ImageStack(width, height); // The stack of ips that scales roughly from 0 to 1
		floatStack.addSlice("Gradient norm", toFloatProcessor(G));
		floatStack.addSlice("Laplacian mangitude", toFloatProcessor(L));
		floatStack.addSlice("Hessian determintant", toFloatProcessor(H));
		floatStack.addSlice("Mask", toFloatProcessor(M));
		if (comp2 == null) {
			comp2 = new ImagePlus("Scaled derivatives", floatStack);	
		} else {
			comp2.setStack(floatStack); 
		}
		comp2.show();
		comp2.getProcessor().resetMinAndMax();

		ImageStack tStack = new ImageStack(width, height); // The stack of ips that scales roughly like source image
		tStack.addSlice("Gaussian filtered", toFloatProcessor(F));
		tStack.addSlice("Anisotropic diffusion", toFloatProcessor(AD));
		tStack.addSlice("Masked image", toFloatProcessor(R));
		if (comp1 == null) {
			comp1 = new ImagePlus("Components", tStack);	
		} else {
			comp1.setStack(tStack);
		}
		comp1.show();
	}
	
	

	public void refresh() {
		switch (stepUpdateToPerform) {
		case 1: 
			paramStep1Changed();
			break;
		case 2:
			paramStep2Changed();
			break;
		case 3:
			paramStep3Changed();
			break;
		case 4:
			paramStep4Changed();
			break;
		}
		stepUpdateToPerform = Integer.MAX_VALUE;

	}


	@SuppressWarnings("unused")
	private static String echoParameters(final double[] param) {
		String str = "";
		str 	+= "\n σf\t= " + param[0];
		str 	+= "\n Nad\t= " + (int) param[1];
		str 	+= "\n κad\t= " + param[2];
		str 	+= "\n σd\t= " + param[3];
		str 	+= "\n γ\t= " + param[4];
		str 	+= "\n α\t= " + param[5];
		str 	+= "\n β\t= " + param[6];
		str 	+= "\n ε\t= " + param[7];
		str 	+= "\n δ\t= " + param[8];
		return str;
	}


	/*
	 * MAIN METHODS
	 */

	public static void main(String[] args) {

		File testImage = new File("E:/Users/JeanYves/Documents/Projects/BRajaseka/Data/Meta-nov7mdb18ssplus-embryo2-1.tif");
//		File testImage = new File("/Users/tinevez/Projects/BRajaseka/Data/Meta-nov7mdb18ssplus-embryo2-1.tif");

		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(testImage.getAbsolutePath());
		imp.show();

		CWNT_ plugin = new CWNT_();
		plugin.run("");
	}


	/*
	 * PRIVATE CLASS
	 */

	/**
	 * This is a helper class modified after a class by Albert Cardona
	 */
	private class DisplayUpdater extends Thread {
		long request = 0;

		// Constructor autostarts thread
		DisplayUpdater() {
			super("NucleiTracker updater thread");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call displayer update from this thread
					if (r > 0)
						refresh();
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
