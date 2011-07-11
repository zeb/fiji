package fiji.plugin.constrainedshapes;

import static fiji.plugin.constrainedshapes.SnappingCircleTool.InteractionStatus;

import ij.gui.ImageCanvas;
import ij.gui.OvalRoi;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;


public class CircleRoi extends OvalRoi {
	protected static final long serialVersionUID = 1L;
	/** Min dist to drag a handle, in unzoomed pixels */
	protected static final double DRAG_TOLERANCE = 10;
	public CircleShape shape;
	protected Handle[] handles = new Handle[4];
	AffineTransform canvasAffineTransform = new AffineTransform();

	/*
	 * ENUM
	 */

	public static enum ClickLocation {
		INSIDE, OUTSIDE, HANDLE;

		/**
		 * Return the {@link InteractionStatus} expected when clicking this location.
		 */
		public InteractionStatus getInteractionStatus() {
			InteractionStatus is = InteractionStatus.FREE;
			switch (this) {
			case INSIDE:
				is = InteractionStatus.MOVING;
				break;
			case  HANDLE:
				is = InteractionStatus.RESIZING;
				break;
			case OUTSIDE:
				is = InteractionStatus.FREE; // dummy
				break;
			}
			return is;
		}
	};

	/*
	 * INNER CLASS
	 */

	/**
	 * This internal class is used to deal with the small handles that appear on
	 * the ROI. They are used to resize the shape when the user click-drags on
	 * them.
	 */
	protected static class Handle {
		protected static final long serialVersionUID = 1L;
		protected static final Color HANDLE_COLOR = Color.WHITE;
		public Point2D center = new Point2D.Double();
		public int size = 7;

		public Handle(double x, double y) {
			this.center = new Point2D.Double(x, y);
		}

		public void draw(Graphics g, AffineTransform at) {
			Point2D dest = new Point2D.Double();
			if ( at == null) {
				dest = center;
			} else {
				at.transform(center, dest);
			}
			final int ix = (int) dest.getX();
			final int iy = (int) dest.getY();
			g.setColor(Color.black);
			g.fillRect(ix-size/2, iy-size/2, size, size);
			g.setColor(HANDLE_COLOR);
			g.fillRect(ix-size/2+1, iy-size/2+1, size-2, size-2);
		}
	}

	/*
	 * CONSTRUCTORS
	 */

	public CircleRoi() {
		this(new CircleShape());
	}

	public CircleRoi(CircleShape shape) {
		super(1,1,1,1); // but we don't care
		this.shape = shape;
		constrain = true;
	}

	/*
	 * OVALROI METHODS
	 */

	@Override
	public void draw(Graphics g) {
		refreshAffineTransform();
		Graphics2D g2 = (Graphics2D) g;
		g.setColor(strokeColor!=null? strokeColor:ROIColor);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.draw(canvasAffineTransform.createTransformedShape(shape));
		prepareHandles();
		drawHandles(g);
	}

	@Override
	public void drawPixels(ImageProcessor ip) {
		Rectangle r = shape.getBounds();
		new OvalRoi(r.x, r.y, r.width, r.height).drawPixels(ip);
	}

	@Override
	public Rectangle getBounds() {
		return shape.getBounds();
	}

	@Override
	public ImageProcessor getMask() {
		Rectangle r = shape.getBounds();
		return new OvalRoi(r.x, r.y, r.width, r.height).getMask();
	}

	@Override
	public boolean contains(int x, int y) {
		return shape.contains(x, y);
	}

	@Override
	public Polygon getConvexHull() {
		Rectangle r = shape.getBounds();
		return new OvalRoi(r.x, r.y, r.width, r.height).getConvexHull();
	}

	@Override
	public double getLength() {
		return shape.getRadius()*2*Math.PI;
	}

	/*
	 * PUBLIC METHODS
	 */

	public ClickLocation getClickLocation(Point2D p) {
		if (shape == null)  { // There is no shape yet
			return ClickLocation.OUTSIDE;
		}
		double[] params = shape.getParameters();
		if ( Double.isNaN(params[0]) || Double.isNaN(params[1]) || Double.isNaN(params[2]) ) {
			return ClickLocation.OUTSIDE;
		}
		double dist;
		Point2D coords = new Point2D.Double();
		for (Handle h : handles) {
			if (h == null) continue;
			coords = h.center;
			canvasAffineTransform.transform(h.center, coords);
			dist = coords.distance(p);
			if (dist < DRAG_TOLERANCE) {
				return ClickLocation.HANDLE;
			}
		}
		if (canvasAffineTransform.createTransformedShape(shape).contains(p)) {
			return ClickLocation.INSIDE;
		}
		return ClickLocation.OUTSIDE;
	}

	/**
	 * Regenerate the {@link #handles} field. The {@link Handle} coordinates are generated
	 * with respect to the {@link TwoCircleShape} object. They will be transformed in the
	 * {@link ImageCanvas} coordinates when drawn.
	 */
	protected void prepareHandles() {
		final double[] params = shape.getParameters();
		final double xc = params[0];
		final double yc = params[1];
		final double r  = params[2];
		final Handle ht = new Handle(xc, yc+r);
		final Handle hb = new Handle(xc, yc-r);
		final Handle hl = new Handle(xc-r, yc);
		final Handle hr = new Handle(xc+r, yc);
		handles[0] = ht;
		handles[1] = hb;
		handles[2] = hl;
		handles[3] = hr;
	}

	/**
	 * Non destructively draw the handles of this ROI, using the {@link Graphics}
	 * object given. The {@link #canvasAffineTransform} is used to position
	 * the handles correctly with respect to the canvas zoom level.
	 */
	protected void drawHandles(Graphics g) {
		final double r = shape.getParameters()[2];
		double size = 4;
		if (!Double.isNaN(r)) {
			size = r * ic.getMagnification();
		}
		int handleSize;
		if (size>10) {
			handleSize = 8;
		} else if (size>5) {
			handleSize = 6;
		} else {
			handleSize = 4;
		}
		for (Handle h : handles) {
			h.size = handleSize;
			h.draw(g, canvasAffineTransform);
		}
	}

	/**
	 * Refresh the {@link AffineTransform} of this shape, according to current {@link ImageCanvas}
	 * settings. It is normally called only by the {@link #draw(Graphics)} method, for it
	 * is called every time something changed in the canvas, and that is when this transform needs
	 * to be updated.
	 */
	protected void refreshAffineTransform() {
		canvasAffineTransform = new AffineTransform();
		if (ic == null) { return; }
		final double mag = ic.getMagnification();
		final Rectangle r = ic.getSrcRect();
		canvasAffineTransform.setTransform(mag, 0.0, 0.0, mag, -r.x*mag, -r.y*mag);
	}

	/*
	 * MAIN METHOD
	 */

	/**
	 * For testing purposes.
	 */
	public static void main(String[] args) {
		final Point2D.Float C = new Point2D.Float(100 , 100);
		final float R = 50;
		//
		ij.ImagePlus imp = ij.IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		imp.show();

		CircleShape c = new CircleShape(C.x, C.y, R);
		CircleRoi roi = new CircleRoi(c);
		imp.setRoi(roi);
		// test zoom
		imp.getCanvas().zoomIn(2, 2);
		imp.updateAndDraw();
	}
}