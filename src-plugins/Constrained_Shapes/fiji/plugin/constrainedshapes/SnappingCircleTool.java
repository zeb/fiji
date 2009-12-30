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
import fiji.util.optimization.MinimiserMonitor;
import fiji.util.optimization.MultivariateFunction;

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
		
		switch (cl) {
		case OUTSIDE:
			switch (status) {
			case CREATING:
				roi.shape.setCenter(p);
				break;
			default:
				// If drag is small, then we will kill this roi
			}
			break;
		default:
			status = cl.getInteractionStatus();
		}
		IJ.showStatus(cl.name() + " - " + status.name());
		start_drag = p;
	}
	
	@Override
	public void handleMouseDrag(MouseEvent e) {
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
		lower_bounds[0] = p.getX() - roi.shape.getRadius(); 
		lower_bounds[1] = p.getY() - roi.shape.getRadius();
		lower_bounds[2] = 0.5 * roi.shape.getRadius();
		upper_bounds[0] = p.getX() + roi.shape.getRadius(); 
		upper_bounds[1] = p.getY() + roi.shape.getRadius();
		upper_bounds[2] = 1.5 * roi.shape.getRadius();
		snapper.fitter.setLowerBounds(lower_bounds);
		snapper.fitter.setUpperBounds(upper_bounds);
		snapper.fitter.setNPoints((int) roi.getLength());

		start_drag = p;
		imp.setRoi(roi); 
		IJ.showStatus(roi.shape.toString()); 
	}
	
	@Override
	public void handleMouseClick(MouseEvent e) {
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
		return "C000DccDd5DdaDdbDe6De7De8De9DeaC000Db3Dd4C000Dc4C000De5" +
				"C000DbcC000C111D8dD9dC111Df8C111Dd6C111Df7C111DadDcb" +
				"C111DebC111C222D5cDdcC222D4bC222D7dDc3C222Df9C222Da3C222" +
				"C333Dd9C333DbdC333C444D39C444D45C444D38C444C555D37C555D6c" +
				"C555Df6C555C666Dd7C666D4aC666D63C666D54C666C777Dd8C777Dfa" +
				"C777C888D6dD73C888De4C888D36DcdC888D3aC888C999D93C999Dac" +
				"C999D44CaaaDd3CaaaD4cD53CbbbD5bDc5CbbbDf5CbbbD83DecCbbbD35Db4" +
				"CbbbDa2CbbbCcccD3bD5dD92CcccDfbCcccDb2CcccDddCcccD82CcccD72" +
				"CcccD46CcccCdddD62Dc2CdddD43De3CdddD7cDf4CdddD34CdddD7eD8eD9e" +
				"CdddDaeCdddD27D28D29CdddD3cD4dCdddD26CdddD6eDbeDcaDedDfc" +
				"CdddD2aD52Dd2CeeeD49CeeeDceCeeeD9cCeeeD25CeeeDbbCeeeD5e" +
				"CeeeD2bD33Df3CeeeD8cCeeeDdeCeeeCfffD64CfffD42D47De2CfffD3d" +
				"CfffDfdCfffD24D48D55CfffD2cD4eD61D71D81D91Da1Db1Dc1Dc6Dee" +
				"CfffD32D51Dd1Df2CfffD23D5aD6bD6fD7fD8fD9fDa4DafDc9CfffD15D16" +
				"D17D18D19D1aD1bD2dD3eD41D5fD74DabDb5DbfDcfDe1DfeCfffD14D56D94" +
				"DbaDc7Dc8DdfCfffD1cD22D4fD59D65D7bD84DefCfffD2eD31D6aD9bDb6Df1" +
				"CfffD13D57D8bDa5Db9CfffD1dD3fD50D58D60D70D75D80D90Da0DaaDb0Dc0" +
				"Dd0DffCfffD21D40D66De0";
	}

	@Override
	public String getToolName() {		
		return "Snapping_Circle_Shape";
	}

	@Override
	public boolean hasOptionDialog() {
		return false;
	}

	@Override
	public void showOptionDialog() {}



}
