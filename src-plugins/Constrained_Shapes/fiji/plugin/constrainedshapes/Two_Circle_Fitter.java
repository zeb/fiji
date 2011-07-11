package fiji.plugin.constrainedshapes;

import fiji.plugin.constrainedshapes.ParameterizedShape.EvalFunction;
import fiji.plugin.constrainedshapes.ShapeFitter.Method;

import static fiji.plugin.constrainedshapes.ShapeFitter.Method;
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

import pal.math.MinimiserMonitor;
import pal.math.MultivariateFunction;

public class Two_Circle_Fitter implements PlugIn, ActionListener, MinimiserMonitor {
	protected static final Method DEFAULT_METHOD = ShapeFitter.Method.CONJUGATE_DIRECTION_SEARCH;
	protected TCSDialog dialog;
	protected ParameterizedShape.EvalFunction targetFunction = EvalFunction.MEAN;
	protected ShapeFitter.Method method = DEFAULT_METHOD;
	protected int[] sliceParameters = new int[] {1, 1, 1};
	protected ImagePlus imp;
	protected ImageCanvas canvas;
	protected RoiListStackWindow stackWindow;
	boolean userHasCanceled = false;
	boolean launchedFromRunMethod = false;

	/*
	 * PUBLIC METHODS
	 */

	public synchronized void run(String arg) {
		launchedFromRunMethod = true;
		ImagePlus current = WindowManager.getCurrentImage();
		if (current == null) { return; }

		setImagePlus(current);
		if (current.getStack().getSize() > 1) {
			stackWindow = new RoiListStackWindow(imp, canvas);
			stackWindow.show();
			new TwoCircleTool().run("");
		}

		TwoCircleShape tcs;
		Roi roi = imp.getRoi();
		if ( !(roi instanceof TwoCircleRoi) ) {
			new TwoCircleTool().run("");
		}

		// Display dialog, and wait for user clicks
		displayICWindow(imp);

		// Put the plugin to halt until the user presses the dialog's button
		try {
			while (true) {
				this.wait();
				// User has canceled?
				if (userHasCanceled) {
					dialog.dispose();
					IJ.showStatus("Two-circle fitter canceled.");
					return;
				}
				roi = imp.getRoi();
				if (roi instanceof TwoCircleRoi) {
					break;
				} else {
					IJ.error("Please specify a Two-Circle Roi.");
					return;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Retrieve the adjusted two-circle shape
		tcs = (TwoCircleShape) ( (TwoCircleRoi)imp.getRoi() ).getShape();
		imp.killRoi();

		// Retrieve dialog parameters
		method = DEFAULT_METHOD;
		targetFunction = dialog.getSelectedTargetFunction();
		sliceParameters = dialog.getSliceParameters();
		boolean doMonitor = dialog.doMonitor();

		// Close dialog
		dialog.dispose();

		// Infer bounds for minimization
		final int width = imp.getWidth();
		final int height = imp.getHeight();
		tcs.lowerBounds[0] = 0;
		tcs.lowerBounds[1] = 0;
		tcs.lowerBounds[2] = 0;
		tcs.lowerBounds[3] = 0;
		tcs.lowerBounds[4] = 0;
		tcs.lowerBounds[5] = 0;
		tcs.upperBounds[0] = width;
		tcs.upperBounds[1] = height;
		tcs.upperBounds[2] = Math.min(width, height);
		tcs.upperBounds[3] = width;
		tcs.upperBounds[4] = height;
		tcs.upperBounds[5] = Math.min(width, height);

		// Start calculation
		IJ.showStatus("Executing fit...");
		TwoCircleShape[] results = exec(tcs, doMonitor);
		IJ.showStatus("Fitting done.");

		//Display result table
		displayResults(results);
	}


	public TwoCircleShape[] exec(TwoCircleShape tcs, boolean doMonitor) {
		final int start = sliceParameters[0];
		final int stop  = sliceParameters[1];
		final int step  = sliceParameters[2];
		final Color origColor = Roi.getColor();

		// Prepare optimizer
		ShapeFitter optimizer = new ShapeFitter(tcs); // This shape will be modified by the optimizer all along
		optimizer.setFunction(targetFunction);
		optimizer.setMethod(method);
		optimizer.setNPoints((int) tcs.getPerimeter());
		if (doMonitor) {
			optimizer.setMonitor(this);
			Roi.setColor(Color.BLUE);
		}

		ImageProcessor ip = null;
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		TwoCircleRoi roiToStore;
		imp.setRoi(roi);
		TwoCircleShape[] results = new TwoCircleShape[ 1 + (int) Math.floor( (stop-start)/step) ];
		int index = 0;
		for (int i = start; i <= stop; i += step) {
			if (IJ.escapePressed()) {
				IJ.resetEscape();
				break;
			}
			if (launchedFromRunMethod) {
				IJ.showProgress(index*step/(double)(stop-start));
			}
			imp.setSlice(i);
			ip = imp.getImageStack().getProcessor(i);
			optimizer.setImageProcessor(ip);
			optimizer.optimize();
			results[index] = tcs.clone();
			if (imp.getStack().getSize() > 1) {
				roiToStore = new TwoCircleRoi(results[index]);
				stackWindow.setRoi(roiToStore, i);
			}
			imp.draw();
			index++;
		}
		if (launchedFromRunMethod) {
			IJ.showProgress(2.0); // to erase it
		}
		Roi.setColor(origColor);
		imp.draw();
		return results;
	}

	/**
	 * Display a {@link JTable} with the 6 parameters of the {@link TwoCircleShape} array
	 * given in argument. The value for the frame is derived from the {@link #sliceParameters}
	 * of this plugin instance.
	 */
	public void displayResults(TwoCircleShape[] results) {

		String[] tcsParams = TwoCircleShape.getParameterNames();
		String[] columnNames = new String[tcsParams.length + 1];
		columnNames[0] = "Frame";
		for (int i = 1; i < columnNames.length; i++) {
			columnNames[i] = tcsParams[i-1];
		}
		final int start = sliceParameters[0];
		final int step  = sliceParameters[2];

		Object[][] tableData = new Object[results.length][columnNames.length];
		TwoCircleShape tcs;
		double[] params;
		int index = start;
		for (int i = 0; i < tableData.length; i++) {
			tableData[i][0]	= index;
			index += step;
			tcs = results[i];
			if (tcs == null) continue;
			params = tcs.getParameters();
			for (int j = 0; j < params.length; j++) {
				tableData[i][j+1] 	= params[j];
			}
		}

		JTable table = new JTable(tableData, columnNames);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));

		JScrollPane scrollPane = new JScrollPane(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		JPanel tablePanel = new JPanel(new GridLayout());
		tablePanel.add(scrollPane);
	    JFrame frame = new JFrame("Two-circle fit for "+imp.getShortTitle());

	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    frame.setContentPane(tablePanel);
	    frame.pack();
	    frame.setVisible(true);
	}

	/*
	 * MAIN METHOD
	 */

	public static void main(String[] args) {
		String demoFile = "/Users/tinevez/Development/fiji/testTCS.tif";
		ij.io.Opener o = new ij.io.Opener();
		ImagePlus imp = o.openTiff("", demoFile);
		imp.show();

		Two_Circle_Fitter instance = new Two_Circle_Fitter();
		instance.setImagePlus(imp);
		instance.setTargetFunction(ParameterizedShape.EvalFunction.MEAN);

		TwoCircleShape startPoint = new TwoCircleShape(207.6, 210.0, 90.0, 328.4, 320.0, 60.0);
		System.out.println("Fitting from "+startPoint);
		TwoCircleShape[] results = instance.exec(startPoint, true);
		System.out.println("Fitting done:");
		for (int i = 0; i < results.length; i++) {
			System.out.println(results[i]);
		}

	}

	/**
	 * Display the user interface dialog.
	 */
	protected void displayICWindow(final ImagePlus imp) {
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
		if (e.getID() == TCSDialog.CANCELED) {
			userHasCanceled = true;
		}
	}



	/*
	 * SETTERS AND GETTERS
	 */

	public void setTargetFunction(ParameterizedShape.EvalFunction targetFunction) {	this.targetFunction = targetFunction; }
	public ParameterizedShape.EvalFunction getTargetFunction() { return targetFunction;	}
	public void setSliceParameters(int[] sliceParameters) { this.sliceParameters = sliceParameters; }
	public int[] getSliceParameters() { return sliceParameters; }
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