package fiji.plugin.constrainedshapes;

import ij.IJ;
import ij.gui.ShapeRoi;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

public class TwoCircleRoi extends ShapeRoi {

	/*
	 * FIELDS
	 */
	
	private static final long serialVersionUID = 1L;

	private TwoCircleShape tcs;
	
	/*
	 * INNER CLASS
	 */
	
	private static class Handle extends Polygon {
		private static final long serialVersionUID = 1L;
		public enum Type { CIRCLE_1_CENTER, CIRCLE_2_CENTER, CIRCLE_1_RADIUS, CIRCLE_2_RADIUS }
		public  Type type;
		public Handle(double x, double y, Type _type) {
			super(	new int[] { (int)x-1,(int)x-1,(int)x+1,(int)x+1 } ,
					new int[] { (int)y-1,(int)y+1,(int)y+1,(int)y-1 } ,
					4);
			this.type = _type;
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
		g2.setColor(Color.YELLOW);
		
		g2.draw(at.createTransformedShape(tcs));
		// Prepare handles
		final double xc1 = tcs.xc1;
		final double xc2 = tcs.xc2;
		final double yc1 = tcs.yc1;
		final double yc2 = tcs.yc2;
		final double r1 = tcs.r1 ;
		final double r2 = tcs.r2 ;
//		final double xc1 = ic.offScreenXD((int) tcs.xc1);
//		final double xc2 = ic.offScreenXD((int) tcs.xc2);
//		final double yc1 = ic.offScreenXD((int) tcs.yc1);
//		final double yc2 = ic.offScreenXD((int) tcs.yc2);
//		final double r1 = tcs.r1 / ic.getMagnification();
//		final double r2 = tcs.r2 / ic.getMagnification();
		Handle hc1 = new Handle(xc1, yc1, Handle.Type.CIRCLE_1_CENTER);
		Handle hc2 = new Handle(xc2, yc2, Handle.Type.CIRCLE_2_CENTER);
		final double phi = Math.atan2(yc2-yc1, xc2-xc1);
		final float dxt1 = (float) (r1 * Math.sin(phi));
		final float dyt1 = (float) (r1 * Math.cos(phi));
		final float dxt2 = (float) (r2 * Math.sin(phi));
		final float dyt2 = (float) (r2 * Math.cos(phi));
		Handle ht1 = new Handle(xc1-dxt1, yc1+dyt1, Handle.Type.CIRCLE_1_RADIUS);
		Handle hb1 = new Handle(xc1+dxt1, yc1-dyt1, Handle.Type.CIRCLE_1_RADIUS);
		Handle ht2 = new Handle(xc2-dxt2, yc2+dyt2, Handle.Type.CIRCLE_2_RADIUS);
		Handle hb2 = new Handle(xc2+dxt2, yc2-dyt2, Handle.Type.CIRCLE_2_RADIUS);
		g2.setColor(Color.RED);
		g2.draw(at.createTransformedShape(hc1));
		g2.draw(at.createTransformedShape(hc2));
		g2.draw(at.createTransformedShape(ht1));
		g2.draw(at.createTransformedShape(ht2));
		g2.draw(at.createTransformedShape(hb1));
		g2.draw(at.createTransformedShape(hb2));
	}
	
	
	
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		ij.ImagePlus imp = ij.IJ.openImage("http://rsb.info.nih.gov/ij/images/AuPbSn40.jpg");
		imp.show();
		
		TwoCircleShape tcs = new TwoCircleShape(200, 100, 100, 300, 150, 150);
		TwoCircleRoi roi = new TwoCircleRoi(tcs);
		imp.setRoi(roi);
		imp.updateAndDraw();
		// test zoom
		imp.getCanvas().zoomIn(2, 2);
		imp.updateAndDraw();
	}

}
