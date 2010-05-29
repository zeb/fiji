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
	private Point2D startDrag;
	private Snapper snapper;
	private Color savedRoiColor;

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

	protected class StopOptimizer extends RuntimeException {}

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
		protected ShapeFitter fitter;

		// Constructor autostarts thread
		protected Snapper(ShapeFitter fitter) {
			super("Circle snapper");
			setPriority(Thread.NORM_PRIORITY);
			this.fitter = fitter;
			fitter.setMonitor(this);
		}

		public void run() {
			try {
				Roi.setColor(Color.BLUE);
				fitter.optimize();
				Roi.setColor(savedRoiColor);
				imp.draw();
			} catch (StopOptimizer e) {
				Roi.setColor(savedRoiColor);
				imp.draw();
			}
		}
		

		/*
		 * MINIMIZERMONITOR METHODS
		 */
		

		public synchronized void newMinimum(double value, double[] parameterValues,
				MultivariateFunction beingOptimized) {
			if (isInterrupted())
				throw new StopOptimizer();
			imp.draw();
		}

		public void updateProgress(double progress) { }
	}
	
	/*
	 * RUN METHOD
	 */
	public void run(String arg) {
		savedRoiColor = Roi.getColor();
		super.run(arg);
	}
	
	
	
	/*
	 * MOUSE METHODS
	 */
	
	@Override
	protected void handleMousePress(MouseEvent e) {
		if (!getImageAndRoi())
			return;
		
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);
		ClickLocation cl = roi.getClickLocation(p);
		if (cl ==ClickLocation.OUTSIDE ) {
			if (status == InteractionStatus.CREATING) { 
				roi.shape.setCenter(p);
			}
		} else {
			status = cl.getInteractionStatus();
		}
		startDrag = p;
	}
	
	@Override
	protected void handleMouseDrag(MouseEvent e) {
		if (roi == null)
			return;
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);
		final double[] params = roi.shape.getParameters();
		
		switch (status) {
		case MOVING:
			params[0] += x-startDrag.getX();
			params[1] += y-startDrag.getY();
			break;
		case RESIZING:
		case CREATING:
			params[2] = roi.shape.getCenter().distance(p);			
			break;
		}
		
		// Tune fitter
		final double r = roi.shape.getRadius();
		roi.shape.lowerBounds[0] = p.getX() - r;
		roi.shape.lowerBounds[1] = p.getY() - r;
		roi.shape.lowerBounds[2] = 0.5 * r;
		roi.shape.upperBounds[0] = p.getX() + r;
		roi.shape.upperBounds[1] = p.getY() + r;
		roi.shape.upperBounds[2] = 1.5 * r;

		startDrag = p;
		imp.setRoi(roi);
		IJ.showStatus(roi.shape.toString());
	}

	protected boolean getImageAndRoi() {
		roi = null;
		imp = WindowManager.getCurrentImage();
		if (imp == null)
			return false;
		canvas = imp.getCanvas();

		Roi currentRoi = imp.getRoi();
		if ( (currentRoi != null) && (currentRoi instanceof CircleRoi) ) {
			roi = (CircleRoi) currentRoi;
			status = InteractionStatus.FREE;
		} else {
			roi = new CircleRoi();
			status = InteractionStatus.CREATING;
		}
		return roi != null;
	}

	@Override
	protected void handleMouseClick(MouseEvent e) {
		if (!getImageAndRoi())
			return;
		ClickLocation cl = roi.getClickLocation(e.getPoint());
		if (cl == ClickLocation.OUTSIDE ) {
			imp.killRoi();
			roi = new CircleRoi();
			status = InteractionStatus.CREATING;
		}
	}
	
	@Override
	protected void handleMouseRelease(MouseEvent e) {
		if (roi == null)
			return;

		if (snapper != null) {
			snapper.interrupt();
			for (;;) try {
				snapper.join();
				break;
			} catch (InterruptedException exception) { /* ignore */ }
		}

		snapper = new Snapper(new ShapeFitter(roi.shape));
		snapper.fitter.setFunction(ParameterizedShape.EvalFunction.MEAN);
		snapper.fitter.setImageProcessor(imp.getProcessor());
		snapper.fitter.setNPoints((int) roi.getLength());
		snapper.fitter.setMonitor(snapper);
		canvas = imp.getCanvas();
		snapper.start();
	}
	
	/*
	 * ABSTRACTTOOL METHODS
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
