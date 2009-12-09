package fiji.util;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class TwoCircleShape implements Shape {

	
	/*
	 * FIELDS
	 */
	
	/**
	 * Circle 1 & 2 coordinates and radius. We store them as array to be able to deal with 
	 * multiple shapes.
	 */
	private float xc1, yc1, r1, xc2, yc2, r2;
	
	
	/*
	 * CONSTRUCTORS
	 */
	
	public TwoCircleShape(float _xc1, float _yc1, float _r1, float _xc2, float _yc2, float _r2) {
		xc1 = _xc1;
		yc1 = _yc1;
		r1 = _r1;
		xc2 = _xc2;
		yc2 = _yc2;
		r2 = _r2;
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void getXY() {
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		PathIterator it = getPathIterator(null);
		float[] coords = new float[6];
		int segment_type;
		int count = 0;
		while (!it.isDone()) {			
			segment_type = it.currentSegment(coords);
			switch (segment_type) {
			case PathIterator.SEG_CLOSE:
				str.append(String.format("Point %d, type SEG_CLOSE", count ) );
				break;
			case PathIterator.SEG_LINETO:
				str.append(String.format("Point %d, type LINETO: %4.1f, %4.1f",
						count, coords[0], coords[1]) );
				break;
			case PathIterator.SEG_MOVETO:
				str.append(String.format("Point %d, type MOVETO: %4.1f, %4.1f",
						count, coords[0], coords[1]) );
				break;
			case PathIterator.SEG_QUADTO:
				str.append(String.format("Point %d, type QUADTO: %4.1f, %4.1f - %4.1f, %4.1f",
						count, coords[0], coords[1], coords[2], coords[3]) );
				break;
			case PathIterator.SEG_CUBICTO:
				str.append(String.format("Point %d, type CUBICTO: %4.1f, %4.1f - %4.1f, %4.1f - %4.1f, %4.1f",
						count, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]) );
				break;				
			}
			it.next();
			count++;
		}
		return str.toString();
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private GeneralPath getPath() {
		final float xb1 = xc1 - r1;
		final float yb1 = yc1 - r1;		
		final float xb2 = xc2 - r2;
		final float yb2 = yc2 - r2;		
		final Ellipse2D circle1 = new Ellipse2D.Float(xb1, yb1, 2*r1, 2*r1);
		final Ellipse2D circle2 = new Ellipse2D.Float(xb2, yb2, 2*r2, 2*r2);
		GeneralPath path = new GeneralPath();
		path.append(circle1, false);
		path.append(circle2, false);
		Area area = new Area(path); // We want the outline
		return new GeneralPath(area);		
	}
	
	
	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] args) {
		
		class TestCanvas extends Canvas {
			private Shape[] shape;
			public TestCanvas(Shape[] _shape) {
				this.shape = _shape;
			}
			private static final long serialVersionUID = 1L;
			public void paint(Graphics g) {
				super.paint(g);
				Graphics2D g2 = (Graphics2D) g;
				for (Shape s : shape) {					
					g2.draw(s);
				}
			}
		}
		
		TwoCircleShape tcs1 = new TwoCircleShape(100, 100, 50, 150, 100, 25); // mingled
		TwoCircleShape tcs2 = new TwoCircleShape(50, 200, 30, 150, 200, 25); // separated
		TwoCircleShape tcs3 = new TwoCircleShape(100, 300, 50, 100, 300, 25); // inside
		Shape[] shapes = new Shape[] { tcs1, tcs2, tcs3 };
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		BorderLayout thisLayout = new BorderLayout();
		frame.getContentPane().setLayout(thisLayout);
		TestCanvas canvas = new TestCanvas(shapes);
		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.pack();
		frame.setSize(200, 400);
		frame.setVisible(true);
	}
	
	/*
	 * SHAPE METHODS
	 */
	
	public boolean contains(Point2D p) {
		return getPath().contains(p);
	}

	public boolean contains(Rectangle2D r) {
		return getPath().contains(r);
	}

	public boolean contains(double x, double y) {
		return getPath().contains(x, y);
	}

	public boolean contains(double x, double y, double w, double h) {
		return getPath().contains(x, y, w, h);
	}

	public Rectangle getBounds() {
		return getPath().getBounds();
	}

	public Rectangle2D getBounds2D() {
		return getPath().getBounds2D();
	}

	public PathIterator getPathIterator(AffineTransform at) {
		return getPath().getPathIterator(at);
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return getPath().getPathIterator(at, flatness);
	}

	public boolean intersects(Rectangle2D r) {
		return getPath().intersects(r);
	}

	public boolean intersects(double x, double y, double w, double h) {
		return getPath().intersects(x, y, w, h);
	}
}
