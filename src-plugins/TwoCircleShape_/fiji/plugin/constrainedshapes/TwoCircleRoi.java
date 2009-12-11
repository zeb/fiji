package fiji.plugin.constrainedshapes;

import ij.gui.Roi;
import ij.gui.ShapeRoi;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class TwoCircleRoi extends ShapeRoi {

	/*
	 * FIELDS
	 */
	
	private static final long serialVersionUID = 1L;

	private TwoCircleShape tcs;
	
	/*
	 * INNER CLASS
	 */
	
	private static class Handle {
		private static final long serialVersionUID = 1L;
		private static final Color HANDLE_COLOR = Color.WHITE;
		public enum Type { CIRCLE_1_CENTER, CIRCLE_2_CENTER, CIRCLE_1_RADIUS, CIRCLE_2_RADIUS }
		public Type type;
		public Point2D center = new Point2D.Double();
		public int size = 7;
		public Handle(double _x, double _y, Type _type) {
			this.center = new Point2D.Double(_x, _y);
			this.type = _type;
		}
		public void draw(Graphics g) {
			final int ix = (int) center.getX();
			final int iy = (int) center.getY();
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
		public void transform(AffineTransform at) {
			at.transform(this.center, this.center); // we do not touch the size
		}
	}
	
	/*
	 * CONSTRUCTOR
	 */
	
	public TwoCircleRoi(TwoCircleShape _tcs) {
		super(_tcs);
		this.tcs = _tcs;
	}

	
	
	/**
	 * Overrides the {@link ShapeRoi#draw(java.awt.Graphics)} method, so that 
	 * we can draw our shape with handles.
	 */
	public void draw(Graphics g) {
		if (ic == null) {return;}
		Graphics2D g2 = (Graphics2D) g;
		// Prepare affine transform to deal with magnification
		AffineTransform at = g2.getDeviceConfiguration().getDefaultTransform();
		final double mag = ic.getMagnification();
		final Rectangle r = ic.getSrcRect();
		at.setTransform(mag, 0.0, 0.0, mag, -r.x*mag, -r.y*mag);
		at.translate(tcs.getBounds().x, tcs.getBounds().y);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Roi.getColor());		
		g2.draw(at.createTransformedShape(tcs));
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		drawHandle(g, prepareHandles(at));
		
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	
	private ArrayList<Handle> prepareHandles(AffineTransform at) {
		ArrayList<Handle> handles = new ArrayList<Handle>(2);
		// Prepare handles
		final double xc1 = tcs.xc1;
		final double xc2 = tcs.xc2;
		final double yc1 = tcs.yc1;
		final double yc2 = tcs.yc2;
		final double r1 = tcs.r1 ;
		final double r2 = tcs.r2 ;
		final double a = Math.sqrt((xc2-xc1)*(xc2-xc1) + (yc2-yc1)*(yc2-yc1)); // distance C1 to C2
		final double lx1 = ( a*a - r2*r2 + r1*r1 ) / (2*a); // distance C1 to cap
		final double lx2 = ( a*a + r2*r2 - r1*r1 ) / (2*a); // distance C2 to cap
		// Center handles
		Handle hc1 = new Handle(xc1, yc1, Handle.Type.CIRCLE_1_CENTER);
		Handle hc2 = new Handle(xc2, yc2, Handle.Type.CIRCLE_2_CENTER);
		hc1.transform(at);
		hc2.transform(at);
		handles.add(hc1);
		handles.add(hc2);
		// The following handles depend on tcs arrangement
		final double phi = Math.atan2(yc2-yc1, xc2-xc1);
		final float dxt1 = (float) (r1 * Math.sin(phi));
		final float dyt1 = (float) (r1 * Math.cos(phi));
		final float dxt2 = (float) (r2 * Math.sin(phi));
		final float dyt2 = (float) (r2 * Math.cos(phi));
		TwoCircleShape.Arrangement arrangement = tcs.getArrangement();
		switch (arrangement) {
		case INTERSECTING:
		{
			// Top and bottom handles
			if (a>lx2) {
				Handle ht1 = new Handle(xc1-dxt1, yc1+dyt1, Handle.Type.CIRCLE_1_RADIUS);
				Handle hb1 = new Handle(xc1+dxt1, yc1-dyt1, Handle.Type.CIRCLE_1_RADIUS);
				ht1.transform(at);
				hb1.transform(at);
				handles.add(ht1);
				handles.add(hb1);
			}
			if (a>lx1) {
				Handle ht2 = new Handle(xc2-dxt2, yc2+dyt2, Handle.Type.CIRCLE_2_RADIUS);
				Handle hb2 = new Handle(xc2+dxt2, yc2-dyt2, Handle.Type.CIRCLE_2_RADIUS);
				ht2.transform(at);
				hb2.transform(at);
				handles.add(ht2);
				handles.add(hb2);
			}
			// Pole handles
			Handle hp1 = new Handle(xc1-dyt1, yc1-dxt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hp2 = new Handle(xc2+dyt2, yc2+dxt2, Handle.Type.CIRCLE_2_RADIUS);
			hp1.transform(at);
			hp2.transform(at);
			handles.add(hp1);
			handles.add(hp2);
			break;
		}
		case ISOLATED:
		{
			Handle ht1 = new Handle(xc1-dxt1, yc1+dyt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hb1 = new Handle(xc1+dxt1, yc1-dyt1, Handle.Type.CIRCLE_1_RADIUS);
			ht1.transform(at);
			hb1.transform(at);
			handles.add(ht1);
			handles.add(hb1);
			Handle ht2 = new Handle(xc2-dxt2, yc2+dyt2, Handle.Type.CIRCLE_2_RADIUS);
			Handle hb2 = new Handle(xc2+dxt2, yc2-dyt2, Handle.Type.CIRCLE_2_RADIUS);
			ht2.transform(at);
			hb2.transform(at);
			handles.add(ht2);
			handles.add(hb2);
			Handle hp1 = new Handle(xc1-dyt1, yc1-dxt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hp2 = new Handle(xc2+dyt2, yc2+dxt2, Handle.Type.CIRCLE_2_RADIUS);
			hp1.transform(at);
			hp2.transform(at);
			handles.add(hp1);
			handles.add(hp2);
			Handle hi1 = new Handle(xc1+dyt1, yc1+dxt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hi2 = new Handle(xc2-dyt2, yc2-dxt2, Handle.Type.CIRCLE_2_RADIUS);
			hi1.transform(at);
			hi2.transform(at);
			handles.add(hi1);
			handles.add(hi2);
		}
		case CIRCLE_1_SWALLOWED:
		{
			Handle ht2 = new Handle(xc2-dxt2, yc2+dyt2, Handle.Type.CIRCLE_2_RADIUS);
			Handle hb2 = new Handle(xc2+dxt2, yc2-dyt2, Handle.Type.CIRCLE_2_RADIUS);
			Handle hp2 = new Handle(xc2+dyt2, yc2+dxt2, Handle.Type.CIRCLE_2_RADIUS);
			Handle hi2 = new Handle(xc2-dyt2, yc2-dxt2, Handle.Type.CIRCLE_2_RADIUS);
			ht2.transform(at);
			hb2.transform(at);
			hp2.transform(at);
			hi2.transform(at);
			handles.add(ht2);
			handles.add(hb2);
			handles.add(hp2);
			handles.add(hi2);
		}
		case CIRCLE_2_SWALLOWED:
		{
			Handle ht1 = new Handle(xc1-dxt1, yc1+dyt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hb1 = new Handle(xc1+dxt1, yc1-dyt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hp1 = new Handle(xc1+dyt1, yc1+dxt1, Handle.Type.CIRCLE_1_RADIUS);
			Handle hi1 = new Handle(xc1-dyt1, yc1-dxt1, Handle.Type.CIRCLE_1_RADIUS);
			ht1.transform(at);
			hb1.transform(at);
			hp1.transform(at);
			hi1.transform(at);
			handles.add(ht1);
			handles.add(hb1);
			handles.add(hp1);
			handles.add(hi1);
		}
		}
		return handles;
	}
	
	
	/**
	 * Non destructively draw the list of handles given in argument, using the {@link Graphics}
	 * object given.
	 */
	private void drawHandle(Graphics g, ArrayList<Handle> handles) {
		double size = (tcs.getBounds().x * tcs.getBounds().y)*mag;
		int handle_size;
		if (size>600.0) {
			handle_size = 8;
		} else if (size>150.0) {
			handle_size = 6;
		} else {			
			handle_size = 4;
		}
		for (Handle h : handles) {
			h.size = handle_size;
			h.draw(g);
		}
	}
	
	/*
	 * MAIN METHOD
	 */
	
	/**
	 * For testing purposes.
	 */
	public static void main(String[] args) {
		ij.ImagePlus imp = ij.IJ.openImage("http://rsb.info.nih.gov/ij/images/AuPbSn40.jpg");
		imp.show();
		
		TwoCircleShape tcs = new TwoCircleShape(200, 150, 50, 250, 200, 100);
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		imp.setRoi(roi);
		imp.updateAndDraw();
		// test zoom
		imp.getCanvas().zoomIn(2, 2);
		imp.updateAndDraw();
	}

}
