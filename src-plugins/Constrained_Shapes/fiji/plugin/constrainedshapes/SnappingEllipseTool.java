package fiji.plugin.constrainedshapes;

import fiji.plugin.constrainedshapes.EllipseRoi.ClickLocation;
import fiji.util.AbstractTool;
import fiji.util.optimization.MinimiserMonitor;
import fiji.util.optimization.MultivariateFunction;
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

public class SnappingEllipseTool extends AbstractTool implements PlugIn {

	/*
	 * FIELDS
	 */
	
	private ImagePlus imp;
	private ImageCanvas canvas;
	private EllipseRoi roi;
	private InteractionStatus status;
	private Point2D start_drag;
	private Snapper snapper;
	private double[] lower_bounds = new double[5];
	private double[] upper_bounds = new double[5];
	private Color saved_roi_color;

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

				if ( (current_roi != null) && (current_roi instanceof EllipseRoi) ) {
					roi = (EllipseRoi) current_roi;
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
			Roi current_roi = imp.getRoi();
			if ( (current_roi == null) || !(current_roi instanceof EllipseRoi)) {
				roi = new EllipseRoi();
				status = InteractionStatus.CREATING;				
			} else {
				roi = (EllipseRoi) current_roi;
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
		start_drag = p;
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
			params[0] += x-start_drag.getX();
			params[1] += y-start_drag.getY();
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
		lower_bounds[0] = xc - range;
		lower_bounds[1] = yc - range;
		lower_bounds[2] = a/2;
		lower_bounds[3] = b/2;
		lower_bounds[4] = phi - Math.PI/8;
		upper_bounds[0] = xc + range; 
		upper_bounds[1] = yc + range;
		upper_bounds[2] = a + range;
		upper_bounds[3] = b + range;
		upper_bounds[4] = phi + Math.PI/8;
		snapper.fitter.setLowerBounds(lower_bounds);
		snapper.fitter.setUpperBounds(upper_bounds);
		snapper.fitter.setNPoints((int) roi.shape.getCircumference());

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
		return "C000D12D21D41D88D94C37dD45C900DbcDccDcdDdcDddDdeDecDedDfcCacfD44C013D77C7bfD76C777D51Ceee" +
				"D10C000D62Db6C6afD6aC555D32Da4CcefD37C444D52C9bfD39CbbbD63CeefD4aC000D56C6afD35D49D55D7bCbdf" +
				"Da9C222D31D83C8bfD8bDbaCaaaD72CffeD33C111D67C7afD15D5aC665Db7CdefD86C246D98C9cfD97CcccD03" +
				"D93CfffD34C59fD28Da8CbdfD59DacC222D11C8bfD25D9bDb9DcaC888D78CeeeD20D40C111Da5C7afD9cCdef" +
				"D17D7aC444Da6C9cfD38DabCbccDc8CeffDbbCbdfD65C333Dc7CbbbD57Dc6CfffD89Db4C111D13D73C666Db5Cdef" +
				"D06D26DaaC359Db8CacfD8cCddcD99CfffD29D36D54C49fD16D27CaaaD02C259D24C222D23C888D84C444D46Cccc" +
				"D42CeffD14CcdfD6bC26bD66C47dD87";
	}

	@Override
	public String getToolName() {
		return "Snapping_Ellipse_Shape";
	}

	@Override
	public boolean hasOptionDialog() {
		return false;
	}

	@Override
	public void showOptionDialog() {	}
	

}
