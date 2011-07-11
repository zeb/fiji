package fiji.plugin.constrainedshapes;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class TwoCircleShape extends ParameterizedShape   {
	/** Enum that describes the instance two circles arrangement. Useful for drawing.	 */
	public enum Arrangement { ISOLATED, CIRCLE_1_SWALLOWED, CIRCLE_2_SWALLOWED, INTERSECTING }

	/**
	 * Parameter array for this shape. As specified in the mother abstract class {@link ParameterizedShape},
	 * we store them as a double array of 6 elements. Array content is the following:
	 * <ul>
	 * 	<li> [0]: <code>xc1</code>, the x coordinate of circle 1 center
	 * 	<li> [1]: <code>yc1</code>, the y coordinate of circle 1 center
	 * 	<li> [2]: <code>r1</code>, the radius of circle 1
	 * 	<li> [3]: <code>xc2</code>, the x coordinate of circle 2 center
	 * 	<li> [4]: <code>yc2</code>, the y coordinate of circle 2 center
	 * 	<li> [5]: <code>r2</code>, the radius of circle 2
	 * </ul>
	 */
	protected double[] params = new double[6];

	/*
	 * CONSTRUCTORS
	 */

	public TwoCircleShape() {
		this(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
//		this(0, 0, 0, 0, 0, 0);
	}

	public TwoCircleShape(double xc1, double yc1, double r1, double xc2, double yc2, double r2) {
		params[0] 	= xc1;
		params[1]	= yc1;
		params[2]	= r1;
		params[3]	= xc2;
		params[4]	= yc2;
		params[5]	= r2;
	}

	/*
	 * PUBLIC METHODS
	 */


	public int getNumParameters() {
		return 6;
	}

	/**
	 * Return the parameter array for this shape.
	 * As specified in the mother abstract class {@link ParameterizedShape},
	 * we store them as a double array of 6 elements. Array content is the following:
	 * <ul>
	 * 	<li> [0]: <code>xc1</code>, the x coordinate of circle 1 center
	 * 	<li> [1]: <code>yc1</code>, the y coordinate of circle 1 center
	 * 	<li> [2]: <code>r1</code>, the radius of circle 1
	 * 	<li> [3]: <code>xc2</code>, the x coordinate of circle 2 center
	 * 	<li> [4]: <code>yc2</code>, the y coordinate of circle 2 center
	 * 	<li> [5]: <code>r2</code>, the radius of circle 2
	 * </ul>
	 * @see #setParameters(double[])
	 */
	public double[] getParameters() {
		return params;
	}

	public static String[] getParameterNames() {
		return new String[] {
				"xc1",
				"yc1",
				"r1",
				"xc2",
				"yc2",
				"r2"
		};
	}

	/**
	 * Sets the parameter array for this shape. This <b>replaces</b> the
	 * previous one, and as such, invalidates previous references to it.
	 * <p>
	 * Array content is the following:
	 * <ul>
	 * 	<li> [0]: <code>xc1</code>, the x coordinate of circle 1 center
	 * 	<li> [1]: <code>yc1</code>, the y coordinate of circle 1 center
	 * 	<li> [2]: <code>r1</code>, the radius of circle 1
	 * 	<li> [3]: <code>xc2</code>, the x coordinate of circle 2 center
	 * 	<li> [4]: <code>yc2</code>, the y coordinate of circle 2 center
	 * 	<li> [5]: <code>r2</code>, the radius of circle 2
	 * </ul>
	 * @see #getParameters()
	 */
	public void setParameters(double[] arr) {
		this.params = arr;
	}

	/**
	 * Return the perimeter of this shape.
	 */
	public double getPerimeter() {
		final double xc1 = params[0];
		final double yc1 = params[1];
		final double r1  = params[2];
		final double xc2 = params[3];
		final double yc2 = params[4];
		final double r2  = params[5];
		double l = Double.NaN;
		final double a = Math.sqrt((xc2-xc1)*(xc2-xc1) + (yc2-yc1)*(yc2-yc1)); // distance C1 to C2
		final boolean separatedCircles = a > r1+r2; // true if the two circles do not intersect, resulting in having 2 separated circles
		final boolean circle1Swallowed = r2 > r1 + a; // true if circle 1 is totally within circle 2, resulting in having only 1 circle
		final boolean circle2Swallowed = r1 > r2 + a;
		if (circle1Swallowed) {
			l = 2 * Math.PI * r2;
		} else if (circle2Swallowed) {
			l = 2 * Math.PI * r1;
		} else if (separatedCircles) {
			l = 2 * Math.PI * (r1+r2);
		} else {
			final double lx1 = ( a*a - r2*r2 + r1*r1 ) / (2*a); // distance C1 to cap
			final double lx2 = ( a*a + r2*r2 - r1*r1 ) / (2*a); // distance C2 to cap
			final double alpha1 = Math.acos(lx1/r1); // cap angle seen from C1
			final double alpha2 = Math.acos(lx2/r2); // cap angle seen from C1
			l = 2 * ( Math.PI - alpha1) * r1 + 2 * ( Math.PI - alpha2) * r2;
		}
		return l;
	}

	public double[][] sample(final int nPoints) {
		final double xc1 = params[0];
		final double yc1 = params[1];
		final double r1  = params[2];
		final double xc2 = params[3];
		final double yc2 = params[4];
		final double r2  = params[5];

		final double[] x = new double[nPoints];
		final double[] y = new double[nPoints];

		final double phi = Math.atan2(yc2-yc1, xc2-xc1); // angle of C1C2 with x axis
		final double a = Math.sqrt((xc2-xc1)*(xc2-xc1) + (yc2-yc1)*(yc2-yc1)); // distance C1 to C2

		final boolean separatedCircles = a > r1+r2; // true if the two circles do not intersect, resulting in having 2 separated circles
		final boolean circle1Swallowed = r2 > r1 + a; // true if circle 1 is totally within circle 2, resulting in having only 1 circle
		final boolean circle2Swallowed = r1 > r2 + a;

		if (circle1Swallowed) {
			double theta;
			for (int i=0; i<nPoints; i++) {
				theta = i * 2 * Math.PI / nPoints;
				x[i] = xc2 + r2 * Math.cos(theta);
				y[i] = yc2 + r2 * Math.sin(theta);
			}

		} else if (circle2Swallowed) {
			double theta;
			for (int i=0; i<nPoints; i++) {
				theta = i * 2 * Math.PI / nPoints;
				x[i] = xc1 + r1 * Math.cos(theta);
				y[i] = yc1 + r1 * Math.sin(theta);
			}

		}else 	if (separatedCircles) {
			final int N1 = (int) Math.round(nPoints / (1+r2/r1));
			final int N2 = nPoints - N1;
			double theta;
			for (int i=0; i<N1; i++) {
				theta = i * 2 * Math.PI / N1;
				x[i] = xc1 + r1 * Math.cos(theta);
				y[i] = yc1 + r1 * Math.sin(theta);
			}
			for (int i = N1; i<nPoints; i++) {
				theta = (i-N1) * 2 * Math.PI / N2;
				x[i] = xc2 + r2 * Math.cos(theta);
				y[i] = yc2 + r2 * Math.sin(theta);
			}

		} else {
			final double lx1 = ( a*a - r2*r2 + r1*r1 ) / (2*a); // distance C1 to cap
			final double lx2 = ( a*a + r2*r2 - r1*r1 ) / (2*a); // distance C2 to cap
			final double alpha1 = Math.acos(lx1/r1); // cap angle seen from C1
			final double alpha2 = Math.acos(lx2/r2); // cap angle seen from C1

			final double corr = (Math.PI-alpha1)/(Math.PI-alpha2) * r1/r2;
			final int N1 = (int) Math.round( nPoints/(1+1/corr)) - 1;
			final int N2 = nPoints - N1;
			double alpha;
			for (int i=0; i<N1; i++) {
				alpha = phi + alpha1 + i * 2 * (Math.PI-alpha1) / N1 ;
				x[i] = xc1 + r1*Math.cos(alpha);
				y[i] = yc1 + r1*Math.sin(alpha);
			}
			for (int i=N1; i<nPoints; i++) {
				alpha = Math.PI + phi + alpha2 + (i-N1) * 2 * (Math.PI-alpha2) / N2;
				x[i] = xc2 + r2*Math.cos(alpha);
				y[i] = yc2 + r2*Math.sin(alpha);
			}
		}
		return new double[][] {x, y};
	}

	public Arrangement getArrangement() {
		final double xc1 = params[0];
		final double yc1 = params[1];
		final double r1  = params[2];
		final double xc2 = params[3];
		final double yc2 = params[4];
		final double r2  = params[5];

		final double a = Math.sqrt((xc2-xc1)*(xc2-xc1) + (yc2-yc1)*(yc2-yc1)); // distance C1 to C2
		final boolean separatedCircles = a > r1+r2; // true if the two circles do not intersect, resulting in having 2 separated circles
		final boolean circle1Swallowed = r2 > r1 + a; // true if circle 1 is totally within circle 2, resulting in having only 1 circle
		final boolean circle2Swallowed = r1 > r2 + a;

		if (circle1Swallowed) {
			return Arrangement.CIRCLE_1_SWALLOWED;
		} else if (circle2Swallowed) {
			return Arrangement.CIRCLE_2_SWALLOWED;
		} else if (separatedCircles) {
			return Arrangement.ISOLATED;
		} else {
			return Arrangement.INTERSECTING;
		}
	}

	public Point2D getC1() {
		return new Point2D.Double(params[0], params[1]);
	}

	public void setC1(Point2D p) {
		params[0] = p.getX();
		params[1] = p.getY();
	}

	public Point2D getC2() {
		return new Point2D.Double(params[3], params[4]);
	}

	public void setC2(Point2D p) {
		params[3] = p.getX();
		params[4] = p.getY();
	}

	public TwoCircleShape clone() {
		TwoCircleShape newShape = new TwoCircleShape();
		newShape.setParameters(this.getParameters().clone());
		return newShape;
	}

	public String toString() {
		return String.format("xc1=%5.0f, yc1=%5.0f, r1=%5.0f, xc2=%5.0f, yc2=%5.0f, r2=%5.0f",
				params[0], params[1], params[2], params[3], params[4], params[5]);
	}

	/**
	 * Return a {@link GeneralPath} that describes the outline of this two-circle shape.
	 * As {@link Ellipse2D} are used internally, the path will be made of Bézier curves.
	 * The path generated by this method is then used in {@link Shape} methods.
	 */
	protected GeneralPath getPath() {
		final double xc1 = params[0];
		final double yc1 = params[1];
		final double r1  = params[2];
		final double xc2 = params[3];
		final double yc2 = params[4];
		final double r2  = params[5];

		GeneralPath path = new GeneralPath();
		if ( !( Double.isNaN(xc1) || Double.isNaN(yc1) || Double.isNaN(r1)) ) {
			final double xb1 = xc1 - r1;
			final double yb1 = yc1 - r1;
			final Ellipse2D circle1 = new Ellipse2D.Double(xb1, yb1, 2*r1, 2*r1);
			path.append(circle1, false);
		}
		if ( !( Double.isNaN(xc2) || Double.isNaN(yc2) || Double.isNaN(r2)) ) {
			final double xb2 = xc2 - r2;
			final double yb2 = yc2 - r2;
			final Ellipse2D circle2 = new Ellipse2D.Double(xb2, yb2, 2*r2, 2*r2);
			path.append(circle2, false);
		}
		Area area = new Area(path); // We want the outline
		return new GeneralPath(area);
	}

	/*
	 * MAIN METHOD
	 */

	public static void main(String[] args) {

		class TestCanvas extends Canvas {
			protected TwoCircleShape[] shape;
			public TestCanvas(TwoCircleShape[] shape) {
				this.shape = shape;
			}
			protected static final long serialVersionUID = 1L;
			public void paint(Graphics g) {
				super.paint(g);
				Graphics2D g2 = (Graphics2D) g;
				for (TwoCircleShape s : shape) {
					g2.draw(s);
				}
				g2.setStroke(new CircleStroke(2));
				for (TwoCircleShape s : shape) {
					g2.draw(s);
				}
			}
		}

		TwoCircleShape tcs1 = new TwoCircleShape(100, 100, 70, 150, 150, 50); // mingled
		TwoCircleShape tcs2 = new TwoCircleShape(50, 200, 30, 150, 250, 60); // separated
		TwoCircleShape tcs3 = new TwoCircleShape(100, 400, 70, 100, 410, 50); // inside
		TwoCircleShape[] shapes = new TwoCircleShape[] { tcs1, tcs2, tcs3 };
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		BorderLayout thisLayout = new BorderLayout();
		frame.getContentPane().setLayout(thisLayout);
		TestCanvas canvas = new TestCanvas(shapes);
		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.pack();
		frame.setSize(250, 500);
		frame.setVisible(true);
	}

	/*
	 * SHAPE METHODS
	 */

	public PathIterator getPathIterator(AffineTransform at) {
		return getPath().getPathIterator(at);
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return getPath().getPathIterator(at, flatness);
	}

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

	public boolean intersects(Rectangle2D r) {
		return getPath().intersects(r);
	}

	public boolean intersects(double x, double y, double w, double h) {
		return getPath().intersects(x, y, w, h);
	}
}