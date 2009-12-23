package fiji.plugin.constrainedshapes;

import fiji.plugin.constrainedshapes.GeomShape.EvalFunction;

import static fiji.plugin.constrainedshapes.GeomShapeFitter.Method;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Two_Circle_Fitter implements PlugIn, ActionListener {

	/*
	 * FIELDS
	 */
	
	private TCSDialog dialog;
	private GeomShape.EvalFunction target_function = EvalFunction.MEAN;
	private GeomShapeFitter.Method method = Method.CONJUGATE_DIRECTION_SEARCH;
	private int[] slice_parameters = new int[] {1, 1, 1};
	private ImagePlus imp;
	private ImageCanvas canvas;
	private Graphics2D graphics;
	
	/*
	 * INNER CLASSES
	 */



	public synchronized void run(String arg) {

		ImagePlus current = WindowManager.getCurrentImage();
		if (current == null) { return; }
		setImagePlus(current);
		
		// Prepare a starting two-circle shape
		final int width = getImagePlus().getWidth();
		final int height = getImagePlus().getHeight();
		final double radius = Math.min(width, height)/4;
		TwoCircleShape tcs = new TwoCircleShape(width/2.0-0.8*radius, height/2.0, radius, width/2.0+0.8*radius, height/2.0, radius);
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		
		canvas.removeMouseListener(canvas);
		canvas.removeMouseMotionListener(canvas); // So as to avoid roi clashes
		imp.saveRoi();
		imp.setRoi(roi);

		// Display dialog, and wait for user clicks
		displayICWindow(imp);

		// Put the plugin to halt until the user presses the dialog's button
		try {
			this.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Retrieve the adjusted two-circle shape
		tcs = (TwoCircleShape) ( (TwoCircleRoi)imp.getRoi() ).getShape();
		imp.killRoi();
		canvas.addMouseListener(canvas);
		canvas.addMouseMotionListener(canvas); // Restore listeners
		
		// Retrieve dialog parameters
		method = dialog.getSelectedMethod();
		target_function = dialog.getSelectedTargetFunction();
		slice_parameters = dialog.getSliceParameters();
		boolean do_monitor = dialog.doMonitor();
		
		// Close dialog
		dialog.dispose();
		
		// Start calculation
		IJ.showStatus("Executing fit...");
		exec(tcs, do_monitor);
		IJ.showStatus("Fitting done.");
	}

	
	public TwoCircleShape exec(TwoCircleShape tcs, boolean do_monitor) {
		final int start = getSliceParameters()[0];
		final int stop  = getSliceParameters()[1];
		final int step  = getSliceParameters()[2];
		
		ImageProcessor ip = null;
		GeomShapeFitter optimizer = new GeomShapeFitter();
		optimizer.setFunction(target_function);
		optimizer.setMethod(method);
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		if (do_monitor) {	
			graphics.setColor(Color.BLUE);
			imp.setRoi(roi); // If we set it now, the optimization process will change the ROI during processing
		}

		for (int i = start; i <= stop; i += step) {
			imp.setSlice(i);
			ip = imp.getImageStack().getProcessor(i);
			optimizer.setImageProcessor(ip);
			optimizer.setShape(tcs);
			optimizer.optimize();
			imp.updateAndDraw();
		}
		imp.setRoi(roi);  
		return tcs;
	}

	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		String demo_file = "/Users/tinevez/Development/fiji/testTCS.tif";
		ij.io.Opener o = new ij.io.Opener();
		ImagePlus imp = o.openTiff("", demo_file);
		imp.show();
		
		Two_Circle_Fitter instance = new Two_Circle_Fitter();
		instance.setImagePlus(imp);
		instance.setMethod(GeomShapeFitter.Method.CONJUGATE_DIRECTION_SEARCH);
		instance.setTargetFunction(GeomShape.EvalFunction.MEAN);
				
		TwoCircleShape start_point = new TwoCircleShape(207.6, 210.0, 90.0, 328.4, 320.0, 60.0);
		System.out.println("Fitting from "+start_point);
		TwoCircleShape tcs = instance.exec(start_point, true);
		System.out.println("Fitting done: "+tcs);
		
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Display the user interface dialog.
	 */
	private void displayICWindow(final ImagePlus imp) {
		ImageWindow window = imp.getWindow();
		final Rectangle r = window.getBounds();		 

		dialog = new TCSDialog(imp);
		int x = 0;
		if (r.x > dialog.getWidth()) {
			x = r.x - dialog.getWidth();
		} else if (r.x + r.width + dialog.getWidth() < IJ.getScreenSize().width) {
			x = r.x + r.width;
		} else {
			x = IJ.getScreenSize().width /2;
		}
		dialog.setBounds(x, r.y, dialog.getWidth(), dialog.getHeight());
		dialog.setVisible(true);
		dialog.addActionListener(this);
	}

	/*
	 * ACTIONLISTENER METHOD
	 */

	public synchronized void actionPerformed(ActionEvent e) {
		this.notifyAll(); // We simply wake the thread so that this plugin execution resumes.
	}
	


	/*
	 * SETTERS AND GETTERS
	 */

	public void setMethod(GeomShapeFitter.Method method) { this.method = method; }
	public GeomShapeFitter.Method getMethod() {	return method; }
	public void setTargetFunction(GeomShape.EvalFunction target_function) {	this.target_function = target_function; }
	public GeomShape.EvalFunction getTargetFunction() { return target_function;	}
	public void setSliceParameters(int[] slice_parameters) { this.slice_parameters = slice_parameters; }
	public int[] getSliceParameters() { return slice_parameters; }
	public ImagePlus getImagePlus() { return imp; }
	
	public void setImagePlus(ImagePlus imp) {		
		this.imp = imp;	
		this.canvas = imp.getCanvas();
		this.graphics = (Graphics2D) canvas.getGraphics();
	}


	
}
