package fiji.plugin.constrainedshapes;

import fiji.plugin.constrainedshapes.TwoCircleTool.InteractionStatus;
import ij.gui.ImageCanvas;
import ij.gui.ShapeRoi;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class TwoCircleRoi extends ShapeRoi  {
	protected static final long serialVersionUID = 1L;
	/** Min dist to drag a handle, in unzoomed pixels */
	protected static final double DRAG_TOLERANCE = 10;
	protected TwoCircleShape tcs;
	protected ArrayList<Handle> handles = new ArrayList<Handle>(4);
	protected AffineTransform canvasAffineTransform = new AffineTransform();

	/**
	 * Enum type to return where the user clicked relative to this ROI.
	 */
	public static enum ClickLocation {
		OUTSIDE, INSIDE, HANDLE_C1, HANDLE_C2, HANDLE_R1, HANDLE_R2;
		/**
		 * Return the {@link InteractionStatus} expected when clicking this location.
		 */
		public InteractionStatus getInteractionStatus() {
			InteractionStatus is = InteractionStatus.FREE;
			switch (this) {
			case INSIDE:
				is = InteractionStatus.MOVING_ROI;
				break;
			case  HANDLE_C1:
				is = InteractionStatus.MOVING_C1;
				break;
			case HANDLE_C2:
				is = InteractionStatus.MOVING_C2;
				break;
			case HANDLE_R1:
				is = InteractionStatus.RESIZING_C1;
				break;
			case HANDLE_R2:
				is = InteractionStatus.RESIZING_C2;
				break;
			}
			return is;
		}
	}

	/**
	 * This internal class is used to deal with the small handles that appear on
	 * the ROI. They are used to resize the shape when the user click-drags on
	 * them.
	 */
	protected static class Handle {
		protected static final long serialVersionUID = 1L;
		protected static final Color HANDLE_COLOR = Color.WHITE;

		public enum Type {
			CIRCLE_1_CENTER, CIRCLE_2_CENTER, CIRCLE_1_RADIUS, CIRCLE_2_RADIUS;
			/**
			 * Return the {@link ClickLocation} corresponding to this handle.
			 */
			public ClickLocation getClickLocation() {
				ClickLocation cl = ClickLocation.HANDLE_C1;
				switch (this) {
				case CIRCLE_1_CENTER:
					cl = ClickLocation.HANDLE_C1;
					break;
				case CIRCLE_2_CENTER:
					cl = ClickLocation.HANDLE_C2;
					break;
				case CIRCLE_1_RADIUS:
					cl = ClickLocation.HANDLE_R1;
					break;
				case CIRCLE_2_RADIUS:
					cl = ClickLocation.HANDLE_R2;
					break;
				}
				return cl;
			}
		}
		public Type type;
		public Point2D center = new Point2D.Double();
		public int size = 7;
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
			switch (type) {
			case CIRCLE_1_RADIUS:
			case CIRCLE_2_RADIUS:
				g.setColor(Color.black);
				g.fillRect(ix-size/2, iy-size/2, size, size);
				g.setColor(HANDLE_COLOR);
				g.fillRect(ix-size/2+1, iy-size/2+1, size-2, size-2);
				break;
			case CIRCLE_1_CENTER:
			case CIRCLE_2_CENTER:
				g.setColor(Color.black);
				g.fillOval(ix-size/2, iy-size/2, size, size);
				g.setColor(HANDLE_COLOR);
				g.fillOval(ix-size/2+1, iy-size/2+1, size-2, size-2);
				break;
			}
		}
	}


	/*
	 * CONSTRUCTOR
	 */

	public TwoCircleRoi() {
		this(new TwoCircleShape());
	}

	public TwoCircleRoi(TwoCircleShape tcs) {
		super(tcs);
		this.tcs = tcs;
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Return the location of the given point with respect to this shape. The point
	 * coordinates are supposed to be in the {@link ImageCanvas} coordinates.
	 * @param p  The point to locate
	 * @return  The point location with respect to this shape.
	 */
	public ClickLocation getClickLocation(Point2D p) {
		if (tcs == null) { // There is no shape yet
			return ClickLocation.OUTSIDE;
		}
		double dist;
		Point2D coords = new Point2D.Double();
		for (Handle h : handles) {
			coords = h.center;
			canvasAffineTransform.transform(h.center, coords);
			dist = coords.distance(p);
			if (dist < DRAG_TOLERANCE) {
				return h.type.getClickLocation();
			}
		}
		if (canvasAffineTransform.createTransformedShape(tcs).contains(p)) {
			return ClickLocation.INSIDE;
		}
		return ClickLocation.OUTSIDE;
	}

	/*
	 * SHAPEROI METHODS
	 */

	/**
	 * Returns the {@link TwoCircleShape} encapsulated by this object. This method
	 * is problematic, because we want this ROI to be immutable. But modifying the shape
	 * which reference is given by this method will modify the ROI internally. However,
	 * the ROI mechanics in ImageJ will ignore these changes.
	 */
	public TwoCircleShape getShape() {
		return tcs;
	}

	/**
	 * Draw this ROI on the current {@link ImageCanvas}.
	 * Overrides the {@link ShapeRoi#draw(java.awt.Graphics)} method, so that
	 * we can draw our shape with handles.
	 */
	@Override
	public void draw(Graphics g) {
		refreshAffineTransform();
		Graphics2D g2 = (Graphics2D) g;
		g.setColor(strokeColor!=null? strokeColor:ROIColor);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.draw(canvasAffineTransform.createTransformedShape(tcs));
		prepareHandles();
		drawHandles(g2);
	}

	/**
	 * Regenerate the {@link #handles} field. The {@link Handle} coordinates are generated
	 * with respect to the {@link TwoCircleShape} object. They will be transformed in the
	 * {@link ImageCanvas} coordinates when drawn.
	 */
	protected void prepareHandles() {
		handles.clear();
		// Prepare handles
		final double[] params = tcs.getParameters();
		final double xc1 = params[0];
		final double yc1 = params[1];
		final double r1  = params[2];
		final double xc2 = params[3];
		final double yc2 = params[4];
		final double r2  = params[5];

		final double dx = xc2 - xc1;
		final double dy = yc2 - yc1;

		final double a = Math.sqrt(dx*dx + dy*dy); // distance C1 to C2
		final double lx1 = ( a*a - r2*r2 + r1*r1 ) / (2*a); // distance C1 to cap
		final double lx2 = ( a*a + r2*r2 - r1*r1 ) / (2*a); // distance C2 to cap
		// Center handles
		Handle hc1 = new Handle(xc1, yc1, Handle.Type.CIRCLE_1_CENTER);
		Handle hc2 = new Handle(xc2, yc2, Handle.Type.CIRCLE_2_CENTER);
		handles.add(hc1);
		handles.add(hc2);
		// The following handles depend on tcs arrangement
		final double phi = Math.atan2(dy, dx);
		final double dxt1 = r1 * Math.sin(phi);
		final double dyt1 = r1 * Math.cos(phi);
		final double dxt2 = r2 * Math.sin(phi);
		final double dyt2 = r2 * Math.cos(phi);
		TwoCircleShape.Arrangement arrangement = tcs.getArrangement();
		switch (arrangement) {
		case INTERSECTING:
		{
			// Top and bottom handles
			if (a>lx2) {
				Handle ht1 = new Handle(xc1-dxt1, yc1+dyt1, Handle.Type.CIRCLE_1_RADIUS);
				Handle hb1 = new Handle(xc1+dxt1, yc1-dyt1, Handle.Type.CIRCLE_1_RADIUS);
				handles.add(ht1);
				handles.add(hb1);
			}
			if (a>lx1) {
				Handle ht2 = new Handle(xc2-dxt2, yc2+dyt2, Handle.Type.CIRCLE_2_RADIUS);
				Handle hb2 = new Handle(xc2+dxt2, yc2-dyt2, Handle.Type.CIRCLE_2_RADIUS);
				handles.add(ht2);
				handles.add(hb2);
			}
			// Pole handles
			Handle hp1 = new Handle(xc1-dyt1, yc1-dxt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hp2 = new Handle(xc2+dyt2, yc2+dxt2, Handle.Type.CIRCLE_2_RADIUS);
			handles.add(hp1);
			handles.add(hp2);
			break;
		}
		case ISOLATED:
		{
			Handle ht1 = new Handle(xc1-dxt1, yc1+dyt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hb1 = new Handle(xc1+dxt1, yc1-dyt1, Handle.Type.CIRCLE_1_RADIUS);
			handles.add(ht1);
			handles.add(hb1);
			Handle ht2 = new Handle(xc2-dxt2, yc2+dyt2, Handle.Type.CIRCLE_2_RADIUS);
			Handle hb2 = new Handle(xc2+dxt2, yc2-dyt2, Handle.Type.CIRCLE_2_RADIUS);
			handles.add(ht2);
			handles.add(hb2);
			Handle hp1 = new Handle(xc1-dyt1, yc1-dxt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hp2 = new Handle(xc2+dyt2, yc2+dxt2, Handle.Type.CIRCLE_2_RADIUS);
			handles.add(hp1);
			handles.add(hp2);
			Handle hi1 = new Handle(xc1+dyt1, yc1+dxt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hi2 = new Handle(xc2-dyt2, yc2-dxt2, Handle.Type.CIRCLE_2_RADIUS);
			handles.add(hi1);
			handles.add(hi2);
			break;
		}
		case CIRCLE_1_SWALLOWED:
		{
			Handle ht2 = new Handle(xc2-dxt2, yc2+dyt2, Handle.Type.CIRCLE_2_RADIUS);
			Handle hb2 = new Handle(xc2+dxt2, yc2-dyt2, Handle.Type.CIRCLE_2_RADIUS);
			Handle hp2 = new Handle(xc2+dyt2, yc2+dxt2, Handle.Type.CIRCLE_2_RADIUS);
			Handle hi2 = new Handle(xc2-dyt2, yc2-dxt2, Handle.Type.CIRCLE_2_RADIUS);
			handles.add(ht2);
			handles.add(hb2);
			handles.add(hp2);
			handles.add(hi2);
			break;
		}
		case CIRCLE_2_SWALLOWED:
		{
			Handle ht1 = new Handle(xc1-dxt1, yc1+dyt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hb1 = new Handle(xc1+dxt1, yc1-dyt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hp1 = new Handle(xc1+dyt1, yc1+dxt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hi1 = new Handle(xc1-dyt1, yc1-dxt1, Handle.Type.CIRCLE_1_RADIUS);
			handles.add(ht1);
			handles.add(hb1);
			handles.add(hp1);
			handles.add(hi1);
			break;
		}
		}
	}

	/**
	 * Non destructively draw the handles of this ROI, using the {@link Graphics}
	 * object given. The {@link #canvasAffineTransform} is used to position
	 * the handles correctly with respect to the canvas zoom level.
	 */
	protected void drawHandles(Graphics g) {
		final double[] params = tcs.getParameters();
		final double r1 = params[2];
		final double r2 = params[5];
		double size = 4;
		if (Double.isNaN(r2)) {
			size = r1 * ic.getMagnification();
		} else if (Double.isNaN(r1)) {
			size = r2 * ic.getMagnification();
		} else {
			size = Math.min(r1,r2) * ic.getMagnification();
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
		final Point2D.Float C1 = new Point2D.Float(100 ,50);
		final Point2D.Float C2 = new Point2D.Float(150 ,100);
		final float R1 = 50;
		final float R2 = 75;
		//
		ij.ImagePlus imp = ij.IJ.openImage("http://rsb.info.nih.gov/ij/images/blobs.gif");
		imp.show();

		TwoCircleShape tcs = new TwoCircleShape(C1.x, C1.y, R1, C2.x, C2.y, R2);
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		imp.setRoi(roi);
		// test zoom
		imp.getCanvas().zoomIn(2, 2);
		imp.updateAndDraw();
	}
}