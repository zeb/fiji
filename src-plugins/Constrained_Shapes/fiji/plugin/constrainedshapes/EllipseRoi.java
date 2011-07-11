package fiji.plugin.constrainedshapes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import fiji.plugin.constrainedshapes.SnappingEllipseTool.InteractionStatus;

import ij.gui.ImageCanvas;
import ij.gui.ShapeRoi;
import ij.process.ImageProcessor;

public class EllipseRoi extends ShapeRoi {
	protected static final long serialVersionUID = 1L;
	/** Min dist to drag a handle, in unzoomed pixels */
	protected static final double DRAG_TOLERANCE = 10;
	public EllipseShape shape;
	protected Handle[] handles = new Handle[4];
	AffineTransform canvasAffineTransform = new AffineTransform();

	/*
	 * ENUM
	 */

	public static enum ClickLocation {
		INSIDE, OUTSIDE, HANDLE_MAJOR, HANDLE_MINOR;

		/**
		 * Return the {@link InteractionStatus} expected when clicking this location.
		 */
		public InteractionStatus getInteractionStatus() {
			switch (this) {
			case INSIDE:
				return InteractionStatus.MOVING;
			case HANDLE_MAJOR:
				return InteractionStatus.RESIZING_MAJOR;
			case HANDLE_MINOR:
				return InteractionStatus.RESIZING_MINOR;
			case OUTSIDE:
			default:
				return InteractionStatus.FREE; // dummy
			}
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
		public Type type;
		public int size = 7;
		public enum Type {
			MAJOR_AXIS, MINOR_AXIS;
			public ClickLocation getClickLocation() {
				switch (this) {
				default:
				case MAJOR_AXIS:
					return ClickLocation.HANDLE_MAJOR;
				case MINOR_AXIS:
					return ClickLocation.HANDLE_MINOR;
				}
			}
		}

		public Handle(double x, double y, Type type) {
			this.center = new Point2D.Double(x, y);
			this.type = type;
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

	public EllipseRoi() {
		this(new EllipseShape());
	}

	public EllipseRoi(EllipseShape shape) {
		super(1, 1, shape); // but we don't care
		this.shape = shape;
	}

	/*
	 * SHAPEROI METHODS
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
	public ShapeRoi and(ShapeRoi sr) {
		ShapeRoi hiddenRoi = new ShapeRoi(shape);
		return hiddenRoi.and(sr);
	}

	@Override
	public ShapeRoi xor(ShapeRoi sr) {
		ShapeRoi hiddenRoi = new ShapeRoi(shape);
		return hiddenRoi.xor(sr);
	}

	@Override
	public ShapeRoi or(ShapeRoi sr) {
		ShapeRoi hiddenRoi = new ShapeRoi(shape);
		return hiddenRoi.or(sr);
	}

	@Override
	public String toString() {
		return "EllipseRoi: "+shape.toString();
	}

	@Override
	public boolean contains(int x, int y) {
		return shape.contains(x, y);
	}

	@Override
	public synchronized Object clone() {
		return new EllipseRoi(shape.clone());
	}

	@Override
	public void drawPixels(ImageProcessor ip) {
		ShapeRoi hiddenRoi = new ShapeRoi(shape);
		hiddenRoi.drawPixels(ip);
	}

	@Override
	public Rectangle getBoundingRect() {
		return shape.getBounds();
	}

	@Override
	public Rectangle getBounds() {
		Rectangle bounds = shape.getBounds();
		if (bounds.width == 0 && bounds.height == 0)
			bounds.width = 1; // force ImagePlus to accept this ROI
		return bounds;
	}

	@Override
	public ImageProcessor getMask() {
		ShapeRoi hiddenRoi = new ShapeRoi(shape);
		return hiddenRoi.getMask();
	}

	@Override
	public boolean isArea() {
		return true;
	}



	/*
	 * PUBLIC METHODS
	 */

	public ClickLocation getClickLocation(Point2D p) {
		if (shape == null)  { // There is no shape yet
			return ClickLocation.OUTSIDE;
		}
		double[] params = shape.getParameters();
		if ( Double.isNaN(params[0]) || Double.isNaN(params[1]) || Double.isNaN(params[2]) ||
				Double.isNaN(params[3]) || Double.isNaN(params[4]) ) {
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
				return h.type.getClickLocation();
			}
		}
		if (canvasAffineTransform.createTransformedShape(shape).contains(p)) {
			return ClickLocation.INSIDE;
		}
		return ClickLocation.OUTSIDE;
	}

	protected void prepareHandles() {
		final double[] params = shape.getParameters();
		final double xc  = params[0];
		final double yc  = params[1];
		final double a   = params[2];
		final double b   = params[3];
		final double phi = params[4];
		final double cosphi = Math.cos(phi);
		final double sinphi = Math.sin(phi);
		final Handle h1 = new Handle(xc + a*cosphi, yc + a*sinphi, Handle.Type.MAJOR_AXIS);
		final Handle h2 = new Handle(xc - a*cosphi, yc - a*sinphi, Handle.Type.MAJOR_AXIS);
		final Handle h3 = new Handle(xc - b*sinphi, yc + b*cosphi, Handle.Type.MINOR_AXIS);
		final Handle h4 = new Handle(xc + b*sinphi, yc - b*cosphi, Handle.Type.MINOR_AXIS);
		handles[0] = h1;
		handles[1] = h2;
		handles[2] = h3;
		handles[3] = h4;
	}

	/**
	 * Non destructively draw the handles of this ROI, using the {@link Graphics}
	 * object given. The {@link #canvasAffineTransform} is used to position
	 * the handles correctly with respect to the canvas zoom level.
	 */
	protected void drawHandles(Graphics g) {
		final double[] params = shape.getParameters();
		final double a = params[2];
		final double b = params[3];
		double size = 4;
		if (Double.isNaN(a)) {
			size = b * ic.getMagnification();
		} else if (Double.isNaN(b)) {
			size = a * ic.getMagnification();
		} else {
			size = Math.min(a,b) * ic.getMagnification();
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
		final double a = 50;
		final double b = 100;
		final double phi = Math.PI/3;
		//
		ij.ImagePlus imp = ij.IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		imp.show();

		EllipseShape e = new EllipseShape(C.x, C.y, a, b, phi);
		EllipseRoi roi = new EllipseRoi(e);
		imp.setRoi(roi);
		// test zoom
		imp.getCanvas().zoomIn(2, 2);
		imp.updateAndDraw();
	}
}