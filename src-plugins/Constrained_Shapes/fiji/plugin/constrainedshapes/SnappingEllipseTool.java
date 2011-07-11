package fiji.plugin.constrainedshapes;

import fiji.plugin.constrainedshapes.EllipseRoi.ClickLocation;

import fiji.tool.AbstractTool;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;

import ij.plugin.PlugIn;

import java.awt.Color;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.awt.geom.Point2D;

import pal.math.MinimiserMonitor;
import pal.math.MultivariateFunction;

public class SnappingEllipseTool extends AbstractTool implements MouseListener, MouseMotionListener, PlugIn {

	protected EllipseRoi roi;
	protected InteractionStatus status;
	protected Point2D startDrag;
	protected Snapper snapper;
	protected double[] lowerBounds = new double[5];
	protected double[] upperBounds = new double[5];
	protected Color savedRoiColor;

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

	protected static class StopOptimizer extends RuntimeException {}

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
	protected class Snapper extends Thread implements MinimiserMonitor {
		protected long request = 0;
		protected ShapeFitter fitter;
		protected ImagePlus imp;
		protected Roi roi;

		// Constructor autostarts thread
		Snapper(ImagePlus imp) {
			super("Ellipse snapper");
			this.imp = imp;
			roi = imp.getRoi();
			setPriority(Thread.NORM_PRIORITY);
		}

		void snap() {
			if (isInterrupted())
				return;
			start();
		}

		public void run() {
			try {
				Roi.setColor(Color.BLUE);
				fitter.optimize();
				Roi.setColor(savedRoiColor);
				imp.setRoi(roi);
			} catch (StopOptimizer e) {}
		}


		/*
		 * MINIMIZERMONITOR METHODS
		 */

		@Override
		public synchronized void newMinimum(double value, double[] parameterValues,
				MultivariateFunction beingOptimized) {
			if (isInterrupted())
				throw new StopOptimizer();
			imp.setRoi(roi);
		}

		@Override
		public void updateProgress(double progress) {}
	}

	/*
	 * MOUSE METHODS
	 */

	protected EllipseRoi getRoi(ImagePlus imp, boolean createIfNotExists) {
		Roi roi = imp.getRoi();
		if ((roi == null) || !(roi instanceof EllipseRoi)) {
			if (!createIfNotExists)
				return null;
			status = InteractionStatus.CREATING;
			EllipseRoi newRoi = new EllipseRoi();
			imp.setRoi(newRoi);
			return newRoi;
		} else {
			status = InteractionStatus.FREE;
			return (EllipseRoi)roi;
		}
	}


	@Override
	public void mousePressed(MouseEvent e) {
		ImagePlus imp = getImagePlus(e);
		ImageCanvas canvas = imp.getCanvas();
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);

		EllipseRoi roi = getRoi(imp, true);
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
	public void mouseDragged(MouseEvent e) {
		final double[] params = roi.shape.getParameters();
		final double xc  = params[0];
		final double yc  = params[1];
		final double a   = params[2];
		final double b   = params[3];
		final double phi = params[4];
		ImageCanvas canvas = getImageCanvas(e);
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
		roi.shape.lowerBounds[0] = xc - range;
		roi.shape.lowerBounds[1] = yc - range;
		roi.shape.lowerBounds[2] = a/2;
		roi.shape.lowerBounds[3] = b/2;
		roi.shape.lowerBounds[4] = phi - Math.PI/8;
		roi.shape.upperBounds[0] = xc + range;
		roi.shape.upperBounds[1] = yc + range;
		roi.shape.upperBounds[2] = a + range;
		roi.shape.upperBounds[3] = b + range;
		roi.shape.upperBounds[4] = phi + Math.PI/8;
		snapper.fitter.setNPoints((int) roi.shape.getCircumference());

		startDrag = p;
		getImagePlus(e).setRoi(roi);
		IJ.showStatus(roi.shape.toString());
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		ImagePlus imp = getImagePlus(e);
		EllipseRoi roi = getRoi(imp, false);
		if (roi == null) return;
		ClickLocation cl = roi.getClickLocation(e.getPoint());
		if (cl == ClickLocation.OUTSIDE ) {
			imp.killRoi();
			roi = new EllipseRoi();
			status = InteractionStatus.CREATING;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		snapper.snap();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}

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
}