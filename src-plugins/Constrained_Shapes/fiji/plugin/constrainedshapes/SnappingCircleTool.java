package fiji.plugin.constrainedshapes;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import fiji.plugin.constrainedshapes.CircleRoi.ClickLocation;
import fiji.util.AbstractTool;

import pal.math.MinimiserMonitor;
import pal.math.MultivariateFunction;

public class SnappingCircleTool extends AbstractTool implements PlugIn {

	/*
	 * FIELDS
	 */
	
	private ImagePlus imp;
	private ImageCanvas canvas;
	private CircleRoi roi;
	private InteractionStatus status;
	private Point2D start_drag;
	private Snapper snapper;
	private double[] lower_bounds = new double[3];
	private double[] upper_bounds = new double[3];
	private Color saved_roi_color;

	/*
	 * ENUM
	 */
	
	public static enum InteractionStatus {
		FREE,
		MOVING,
		RESIZING,
		CREATING;
	}
	
	/*
	 * INNER CLASS
	 */
	
	/**
	 * This is a helper class for the {@link SnappingCircleTool} plugin, that delegates the
	 * snapping of the circle to another thread. This class holds the fitter object
	 * that will optimize the shape over the image. It also implements {@link MinimiserMonitor}
	 * so that it will display the shape as it is optimized (the interface requires that
	 * the monitor is on the same thread that of the optimizer).
	 * <p>
	 * It is derived from a helper class Albert Cardona did for the Dynamic_Reslice plugin. 
	 * 
	 */
	private class Snapper extends Thread implements MinimiserMonitor {
		long request = 0;
		GeomShapeFitter fitter;

		// Constructor autostarts thread
		Snapper() {
			super("Circle snapper");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void snap() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
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
					// Call opmitize from this thread
					if (r > 0)
						Roi.setColor(Color.BLUE);
						fitter.optimize();
						Roi.setColor(saved_roi_color);
						imp.draw();
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {			}
			}
		}
		

		/*
		 * MINIMIZERMONITOR METHODS
		 */
		

		public synchronized void newMinimum(double value, double[] parameterValues,
				MultivariateFunction beingOptimized) {
			imp.draw();
		}

		public void updateProgress(double progress) {
		}
	}
	
	/*
	 * RUN METHOD
	 */
	
	public void run(String arg) {
		saved_roi_color = Roi.getColor();
		// Initialize snapper
		snapper = new Snapper();
		imp = WindowManager.getCurrentImage();
		if (imp != null) { 
			Roi current_roi = imp.getRoi(); 
			if ( (current_roi != null) && (current_roi instanceof CircleRoi) ) {
				roi = (CircleRoi) current_roi;
				status = InteractionStatus.FREE;
			} else {
				roi = new CircleRoi();
				status = InteractionStatus.CREATING;
			}
			snapper.fitter = new GeomShapeFitter(roi.shape);
			snapper.fitter.setFunction(GeomShape.EvalFunction.MEAN);
			snapper.fitter.setMonitor(snapper);			
			canvas = imp.getCanvas();
		}
		super.run(arg);
	}
	
	
	
	/*
	 * MOUSE METHODS
	 */
	
	@Override
	protected void handleMousePress(MouseEvent e) {
		// Deal with changing window
		ImageCanvas source = (ImageCanvas) e.getSource();
		if (source != canvas) {
			// We changed image window. Update fields accordingly
			ImageWindow window = (ImageWindow) source.getParent();
			imp = window.getImagePlus();
			canvas = source;
			Roi current_roi = imp.getRoi();
			if ( (current_roi == null) || !(current_roi instanceof CircleRoi)) {
				roi = new CircleRoi();
				status = InteractionStatus.CREATING;
				imp.setRoi(roi);
			} else {
				roi = (CircleRoi) current_roi;
				status = InteractionStatus.FREE;
			}
		} 
		
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);
		ClickLocation cl = roi.getClickLocation(p);
		
		// Tune fitter
		snapper.fitter.setShape(roi.shape);
		snapper.fitter.setImageProcessor(imp.getProcessor());
		
		if (cl ==ClickLocation.OUTSIDE ) {
			if (status == InteractionStatus.CREATING) { 
				roi.shape.setCenter(p);
			}
		} else {
			status = cl.getInteractionStatus();
		}
		start_drag = p;
	}
	
	@Override
	protected void handleMouseDrag(MouseEvent e) {
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);
		final double[] params = roi.shape.getParameters();
		
		switch (status) {
		case MOVING:
			params[0] += x-start_drag.getX();
			params[1] += y-start_drag.getY();
			break;
		case RESIZING:
		case CREATING:
			params[2] = roi.shape.getCenter().distance(p);			
			break;
		}
		
		// Tune fitter
		final double r = roi.shape.getRadius();
		lower_bounds[0] = p.getX() - r; 
		lower_bounds[1] = p.getY() - r;
		lower_bounds[2] = 0.5 * r;
		upper_bounds[0] = p.getX() + r; 
		upper_bounds[1] = p.getY() + r;
		upper_bounds[2] = 1.5 * r;
		snapper.fitter.setLowerBounds(lower_bounds);
		snapper.fitter.setUpperBounds(upper_bounds);
		snapper.fitter.setNPoints((int) roi.getLength());

		start_drag = p;
		imp.setRoi(roi); 
		IJ.showStatus(roi.shape.toString()); 
	}
	
	@Override
	protected void handleMouseClick(MouseEvent e) {
		if (roi == null) return;
		ClickLocation cl = roi.getClickLocation(e.getPoint());
		if (cl == ClickLocation.OUTSIDE ) {
			imp.killRoi();
			roi = new CircleRoi();
			status = InteractionStatus.CREATING;
		}
	}
	
	@Override
	protected void handleMouseRelease(MouseEvent e) {
		snapper.snap();
	}
	
	/*
	 * ABSTRACTTTOL METHODS
	 */
	
	@Override
	public String getToolIcon() {
		return "C900DbdDcdDceDddDdeDdfDedDeeDfdC03fD26D27D28D29D2aD34D35D36D3aD3bD3cD44D4c" +
				"D53D54D5cD5dD63D6dD73D7dD83D8dD93D94D9cD9dDa4DacDb4Db5Db6DbaDbbDbcDc6Dc7Dc8" +
				"Dc9DcaC555D45D46D47D48D52D58D59D5aD62D6aD71D72D7aD7bD81D8bD91D9bDa1DabDb1Db2" +
				"Dc2Dd2Dd3Dd4Dd8Dd9DdaDe4De5De6De7De8";
	}



	@Override
	public String getToolName() {		
		return "Snapping Circle Shape";
	}

	@Override
	public boolean hasOptionDialog() {
		return false;
	}

	@Override
	public void showOptionDialog() {}



}
