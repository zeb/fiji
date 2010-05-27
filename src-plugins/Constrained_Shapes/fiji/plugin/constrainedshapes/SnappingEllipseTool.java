package fiji.plugin.constrainedshapes;

import fiji.plugin.constrainedshapes.EllipseRoi.ClickLocation;
import fiji.util.AbstractTool;

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

import pal.math.MinimiserMonitor;
import pal.math.MultivariateFunction;

public class SnappingEllipseTool extends AbstractTool implements PlugIn {

	/*
	 * FIELDS
	 */
	
	private ImagePlus imp;
	private ImageCanvas canvas;
	private EllipseRoi roi;
	private InteractionStatus status;
	private Point2D startDrag;
	private Snapper snapper;
	private double[] lowerBounds = new double[5];
	private double[] upperBounds = new double[5];
	private Color savedRoiColor;

	/*
	 * ENUM
	 */
	
	public static enum InteractionStatus {
		FREE,
		MOVING,
		RESIZING_MAJOR,
		RESIZING_MINOR,
		CREATING;
	}

	/*
	 * INNER CLASS
	 */
	
	/**
	 * This is a helper class for the {@link SnappingEllipseTool} plugin, that delegates the
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
						Roi.setColor(savedRoiColor);
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
		savedRoiColor = Roi.getColor();
		// Initialize snapper
		snapper = new Snapper();
		imp = WindowManager.getCurrentImage();
		if (imp != null) { 
			Roi currentRoi = imp.getRoi();
			
			if (arg.equalsIgnoreCase("test")) {
				final int w = imp.getWidth();
				final int h = imp.getHeight();
				final int a = Math.min(w, h)/4;
				final int b = a/2;
				final int x = w/2;
				final int y = h/2;
				EllipseShape el = new EllipseShape(x, y, a, b, 0);
				roi = new EllipseRoi(el);
				status = InteractionStatus.FREE;
				imp.setRoi(roi);
				
			} else {

				if ( (currentRoi != null) && (currentRoi instanceof EllipseRoi) ) {
					roi = (EllipseRoi) currentRoi;
					status = InteractionStatus.FREE;
				} else {
					roi = new EllipseRoi();
					status = InteractionStatus.CREATING;
				}
				canvas = imp.getCanvas();
			}
			snapper.fitter = new GeomShapeFitter(roi.shape);
			snapper.fitter.setFunction(GeomShape.EvalFunction.MEAN);
			snapper.fitter.setMonitor(snapper);
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
			Roi currentRoi = imp.getRoi();
			if ( (currentRoi == null) || !(currentRoi instanceof EllipseRoi)) {
				roi = new EllipseRoi();
				status = InteractionStatus.CREATING;				
			} else {
				roi = (EllipseRoi) currentRoi;
				status = InteractionStatus.FREE;
			}
			imp.setRoi(roi);
		} 
		
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);
		ClickLocation cl = roi.getClickLocation(p);
		
		// Tune fitter
		snapper.fitter.setShape(roi.shape);
		snapper.fitter.setImageProcessor(imp.getProcessor());

		if (cl == ClickLocation.OUTSIDE ) {
			if ( status == InteractionStatus.CREATING ) {
				double[] params = roi.shape.getParameters();
				params[0] = x;
				params[1] = y;
				params[2] = 0;
				params[3] = 0;
				params[4] = 0;
			}
		} else {
			status = cl.getInteractionStatus();
		}
		startDrag = p;
	}
	
	@Override
	protected void handleMouseDrag(MouseEvent e) {
		final double[] params = roi.shape.getParameters();
		final double xc  = params[0];
		final double yc  = params[1];
		final double a   = params[2];
		final double b   = params[3];
		final double phi = params[4]; 
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);
		
		switch (status) {
		case MOVING:
			params[0] += x-startDrag.getX();
			params[1] += y-startDrag.getY();
			break;
		case RESIZING_MAJOR:
			params[4] = Math.atan2(y-yc, x-xc);
			params[2] = roi.shape.getCenter().distance(p);
			break;
		case RESIZING_MINOR:
			params[4] = Math.PI/2 + Math.atan2(y-yc, x-xc);
			params[3] = roi.shape.getCenter().distance(p);
			break;
		case CREATING: // We create an ellipse with major axis along the X axis
			params[2] = Math.abs(xc - x);
			params[3] = Math.abs(yc - y);
			break;
		}
		
		// Tune fitter
		final double range = Math.max(a, b);
		lowerBounds[0] = xc - range;
		lowerBounds[1] = yc - range;
		lowerBounds[2] = a/2;
		lowerBounds[3] = b/2;
		lowerBounds[4] = phi - Math.PI/8;
		upperBounds[0] = xc + range; 
		upperBounds[1] = yc + range;
		upperBounds[2] = a + range;
		upperBounds[3] = b + range;
		upperBounds[4] = phi + Math.PI/8;
		snapper.fitter.setLowerBounds(lowerBounds);
		snapper.fitter.setUpperBounds(upperBounds);
		snapper.fitter.setNPoints((int) roi.shape.getCircumference());

		startDrag = p;
		imp.setRoi(roi); 
		IJ.showStatus(roi.shape.toString()); 
	}
	
	@Override
	protected void handleMouseClick(MouseEvent e) {
		if (roi == null) return;
		ClickLocation cl = roi.getClickLocation(e.getPoint());
		if (cl == ClickLocation.OUTSIDE ) {
			imp.killRoi();
			roi = new EllipseRoi();
			status = InteractionStatus.CREATING;
		}
	}
	
	@Override
	protected void handleMouseRelease(MouseEvent e) {
		snapper.snap();
	}
	
	/*
	 * ABSTRACTTOOL METHODS
	 */
	
	@Override
	public String getToolIcon() {
		return "C444D13D14D15D16D22D23D26D27D32D37D41D42D51D58D61D68D71D78D81D88D91D92D97D98Da2Da7" +
				"Db2Db3Db6Db7Dc3Dc6C03fD46D47D48D49D55D56D59D5aD65D6aD74D75D7aD7bD84D8bD94D9bDa4Dab" +
				"Db4DbbDc4Dc5DcaDcbDd5DdaDe5De6De9DeaDf6Df7Df8Df9C900DbdDcdDceDddDdeDdfDedDeeDfd";
	}
	
	@Override
	public String getToolName() {
		return "Snapping Ellipse Shape";
	}

	@Override
	public boolean hasOptionDialog() {
		return false;
	}

	@Override
	public void showOptionDialog() {	}
	

}
