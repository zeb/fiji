package fiji.plugin.constrainedshapes;

import fiji.plugin.constrainedshapes.GeomShape.EvalFunction;
import fiji.plugin.constrainedshapes.GeomShapeFitter.Method;
import fiji.util.optimization.MinimiserMonitor;
import fiji.util.optimization.MultivariateFunction;

import static fiji.plugin.constrainedshapes.GeomShapeFitter.Method;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class Two_Circle_Fitter implements PlugIn, ActionListener, MinimiserMonitor {

	/*
	 * FIELDS
	 */
	
	private static final Method DEFAULT_METHOD = GeomShapeFitter.Method.CONJUGATE_DIRECTION_SEARCH;
	private TCSDialog dialog;
	private GeomShape.EvalFunction target_function = EvalFunction.MEAN;
	private GeomShapeFitter.Method method = DEFAULT_METHOD;
	private int[] slice_parameters = new int[] {1, 1, 1};
	private double[] upper_bounds = new double[6];
	private double[] lower_bounds = new double[6];
	private ImagePlus imp;
	private ImageCanvas canvas;
	private RoiListStackWindow stack_window;
	
	/*
	 * PUBLIC METHODS
	 */

	


	public synchronized void run(String arg) {

		ImagePlus current = WindowManager.getCurrentImage();
		if (current == null) { return; }

		setImagePlus(current);
		if (current.getStack().getSize() > 1) {
			stack_window = new RoiListStackWindow(imp, canvas);
			stack_window.show();
		}
		
		TwoCircleShape tcs;
		Roi roi = imp.getRoi();
		if ( !(roi instanceof TwoCircleRoi) ) {
			new TwoCircleRoi().run("");
		}

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
		method = DEFAULT_METHOD;
		target_function = dialog.getSelectedTargetFunction();
		slice_parameters = dialog.getSliceParameters();
		boolean do_monitor = dialog.doMonitor();
		
		// Close dialog
		dialog.dispose();
		
		// Infer bounds for minimization
		final int width = imp.getWidth();
		final int height = imp.getHeight();
		lower_bounds[0] = 0;
		lower_bounds[1] = 0;
		lower_bounds[2] = 0;
		lower_bounds[3] = 0;
		lower_bounds[4] = 0;
		lower_bounds[5] = 0;
		upper_bounds[0] = width;
		upper_bounds[1] = height;
		upper_bounds[2] = Math.min(width, height);
		upper_bounds[3] = width;
		upper_bounds[4] = height;
		upper_bounds[5] = Math.min(width, height);		
		
		// Start calculation
		IJ.showStatus("Executing fit...");
		TwoCircleShape[] results = exec(tcs, do_monitor);
		IJ.showStatus("Fitting done.");
		
		//Display result table
		displayResults(results);
	}

	
	public TwoCircleShape[] exec(TwoCircleShape tcs, boolean do_monitor) {
		final int start = slice_parameters[0];
		final int stop  = slice_parameters[1];
		final int step  = slice_parameters[2];
		final Color orig_color = Roi.getColor();
		
		ImageProcessor ip = null;
		GeomShapeFitter optimizer = new GeomShapeFitter(new TwoCircleShape()); // Just to initialize
		optimizer.setFunction(target_function);
		optimizer.setMethod(method);
		optimizer.setNPoints((int) tcs.getPerimeter());
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		TwoCircleRoi roi_to_store;
		imp.setRoi(roi);
		if (do_monitor) {	
			optimizer.setMonitor(this);
			Roi.setColor(Color.BLUE);
		}
		for (int i = 0; i < lower_bounds.length; i++) {
			optimizer.setLowerBound(i, lower_bounds[i]);
			optimizer.setUpperBound(i, upper_bounds[i]);			
		}
		
		TwoCircleShape[] results = new TwoCircleShape[ 1 + (int) Math.floor( (stop-start)/step) ];
		int index = 0;
		for (int i = start; i <= stop; i += step) {
			if (IJ.escapePressed()) {
				IJ.resetEscape();
				break;
			}
			imp.setSlice(i);
			ip = imp.getImageStack().getProcessor(i);
			optimizer.setImageProcessor(ip);
			optimizer.setShape(tcs);
			optimizer.setNPoints((int) tcs.getPerimeter());
			optimizer.optimize();
			results[index] = tcs.clone();
			if (imp.getStack().getSize() > 1) {
				roi_to_store = new TwoCircleRoi(results[index]);
				stack_window.setRoi(roi_to_store, i);
			}
			imp.draw();
			index++;
		}
		Roi.setColor(orig_color);
		imp.draw();
		return results;
	}

	
	public void displayResults(TwoCircleShape[] results) {	
		
		String[] tcs_params = TwoCircleShape.getParameterNames(); 
		String[] column_names = new String[tcs_params.length + 1];
		column_names[0] = "Frame";
		for (int i = 1; i < column_names.length; i++) {
			column_names[i] = tcs_params[i-1];
		}
		final int start = slice_parameters[0];
		final int step  = slice_parameters[2];
			
		Object[][]table_data = new Object[results.length][column_names.length];
		TwoCircleShape tcs;
		double[] params;
		int index = start;
		for (int i = 0; i < table_data.length; i++) {
			tcs = results[i];
			params = tcs.getParameters();
			table_data[i][0]	= index;
			for (int j = 0; j < params.length; j++) {
				table_data[i][j+1] 	= params[j];
			}
			index += step;
		}

		JTable table = new JTable(table_data, column_names);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));

		JScrollPane scrollPane = new JScrollPane(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		JPanel		table_panel = new JPanel(new GridLayout());
		table_panel.add(scrollPane);	
	    JFrame frame = new JFrame("Two-circle fit for "+imp.getShortTitle());

	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    frame.setContentPane(table_panel);
	    frame.pack();
	    frame.setVisible(true);
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
		instance.setTargetFunction(GeomShape.EvalFunction.MEAN);
				
		TwoCircleShape start_point = new TwoCircleShape(207.6, 210.0, 90.0, 328.4, 320.0, 60.0);
		System.out.println("Fitting from "+start_point);
		TwoCircleShape[] results = instance.exec(start_point, true);
		System.out.println("Fitting done:");
		for (int i = 0; i < results.length; i++) {
			System.out.println(results[i]);
		}
		
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
		Roi roi = imp.getRoi();
		if (roi instanceof TwoCircleRoi) {
			this.notifyAll(); // We simply wake the thread so that this plugin execution resumes.
		} else {
			IJ.error("Please specify a Two-circle Roi.");
		}
	}
	


	/*
	 * SETTERS AND GETTERS
	 */

	public void setTargetFunction(GeomShape.EvalFunction target_function) {	this.target_function = target_function; }
	public GeomShape.EvalFunction getTargetFunction() { return target_function;	}
	public void setSliceParameters(int[] slice_parameters) { this.slice_parameters = slice_parameters; }
	public int[] getSliceParameters() { return slice_parameters; }
	public ImagePlus getImagePlus() { return imp; }
	
	public void setImagePlus(ImagePlus imp) {		
		this.imp = imp;	
		this.canvas = imp.getCanvas();
	}


	public void newMinimum(double value, double[] parameterValues,
			MultivariateFunction beingOptimized) {
		imp.draw(); // This is enough to refresh the shape display as it is optimized.
	}

	public void updateProgress(double progress) {	}


	
}
