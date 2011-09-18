package fiji.plugin.nucleitracker.test;

import fiji.plugin.nucleitracker.NucleiMasker;
import fiji.plugin.nucleitracker.gui.TuneParametersPanel;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFrame;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.image.display.imagej.ImageJVirtualStack;
import mpicbg.imglib.type.numeric.IntegerType;
import mpicbg.imglib.type.numeric.real.FloatType;

public class TestGUI implements PlugIn {

	private NucleiMasker<? extends IntegerType<?>> algo;
	private ImagePlus comp2;
	private ImagePlus comp1;
	private TuneParametersPanel panel;
	private final static int WIDTH = 360;
	private final static int HEIGHT = 530;
	private static final double[] DEFAULT_PARAM = new double[] {
		0.5,
		5,
		50,
		1,
		1,
		2.7,
		14.9,
		16.9,
		0.5
	};

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void run(String arg) {

		// Get current image sample
		ImagePlus imp = WindowManager.getCurrentImage();
		if (null == imp)
			return;
		
		// Duplicate
		imp.setRoi(new Roi(100, 300, 100, 100));
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
		algo.setParameters(DEFAULT_PARAM);
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
		comp2 = new ImagePlus("Scaled derivatives", floatStack);
		comp2.show();
		
		ImageStack tStack = new ImageStack(width, height); // The stack of ips that scales roughly like source image
		tStack.addSlice("Gaussian filtered", toFloatProcessor(F));
		tStack.addSlice("Anisotropic diffusion", toFloatProcessor(AD));
		tStack.addSlice("Masked image", toFloatProcessor(R));
		comp1 = new ImagePlus("Components", tStack);
		comp1.show();
		
		
		// Create GUI
		panel = new TuneParametersPanel(DEFAULT_PARAM);
		JFrame frame = new JFrame();
		frame.getContentPane().add(panel);
		frame.setBounds(100, 100, WIDTH, HEIGHT);
		frame.setVisible(true);
		
		// Add listener
		panel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (e == panel.STEP1_PARAMETER_CHANGED) {
					 paramStep1Changed();
				} else if (e == panel.STEP2_PARAMETER_CHANGED) {
					 paramStep2Changed();
				} else if (e == panel.STEP3_PARAMETER_CHANGED) {
					 paramStep3Changed();
				} else if (e == panel.STEP4_PARAMETER_CHANGED) {
					 paramStep4Changed();
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
		comp2.resetDisplayRange();
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
		comp2.resetDisplayRange();

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
		comp2.resetDisplayRange();

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
		comp2.resetDisplayRange();

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



	
	

	public static void main(String[] args) {
		
		File testImage = new File("E:/Users/JeanYves/Documents/Projects/BRajaseka/Data/Meta-nov7mdb18ssplus-embryo2-1.tif");
//		File testImage = new File("/Users/tinevez/Projects/BRajaseka/Data/Meta-nov7mdb18ssplus-embryo2-1.tif");
		
		ImageJ.main(args);
		ImagePlus imp = IJ.openImage(testImage.getAbsolutePath());
		imp.show();
		
		TestGUI plugin = new TestGUI();
		plugin.run("");
		
		
	}
	
}
