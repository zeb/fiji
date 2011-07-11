package fiji.plugin.constrainedshapes;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class EllipseShape extends ParameterizedShape {
	/**
	 * Parameter array for this shape. As specified in the mother abstract class {@link ParameterizedShape},
	 * we store them as a double array of 5 elements. In this class, the ellipse is implemented
	 * using the parametric formula.
	 * <ul>
	 * 	<li> [0]: <code>xc</code>, the x coordinate of the ellipse center
	 * 	<li> [1]: <code>yc</code>, the y coordinate of the ellipse center
	 * 	<li> [2]: <code>a</code>, the semi-major length
	 * 	<li> [3]: <code>b</code>, the semi-minor length
	 *  <li> [4]: <code>phi</code>, the angle between the X axis and the major axis, in radians.
	 * </ul>
	 */
	protected double[] params = new double[5];

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Create a new ellipse using its parametric form:
	 * @param xc  the ellipse center X coordinate
	 * @param yc  the ellipse center Y coordinate
	 * @param a   the ellipse semi-major length
	 * @param b   the ellipse semi-minor length
	 * @param phi the angle between the X axis and the major axis, in radians
	 */
	public EllipseShape(double xc, double yc, double a, double b, double phi) {
		params[0] = xc;
		params[1] = yc;
		params[2] = a;
		params[3] = b;
		params[4] = phi;
	}

	public EllipseShape() {
		this(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
	}

	/*
	 * PUBLIC METHODS
	 */

	public Point2D getCenter() {
		return new Point2D.Double(params[0], params[1]);
	}

	public void setCenter(Point2D c) {
		params[0] = c.getX();
		params[1] = c.getY();
	}

	public String toString() {
		return String.format("xc=%5.0f, yc=%5.0f, a=%5.0f, b=%5.0f, phi=%.2f",
				params[0], params[1], params[2], params[3], params[4]);
	}

	/**
	 * Ramanujan's approximation of an ellipse circumference.
	 */
	public double getCircumference() {
		final double a = params[2];
		final double b = params[3];
		return Math.PI * ( 3*(a+b) - Math.sqrt(10*a*b + 3*(a*a+b*b)) );
	}


	/*
	 * GEOMSHAPE METHODS
	 */

	@Override
	public EllipseShape clone() {
		EllipseShape newEl = new EllipseShape();
		newEl.setParameters(params);
		return newEl;
	}

	@Override
	public int getNumParameters() {
		return 5;
	}

	@Override
	public double[] getParameters() {
		return params;
	}

	@Override
	public double[][] sample(int nPoints) {
		final double xc  = params[0];
		final double yc  = params[1];
		final double a   = params[2];
		final double b   = params[3];
		final double phi = params[4];
		final double sinphi = Math.sin(phi);
		final double cosphi = Math.cos(phi);
		final double[] x = new double[nPoints];
		final double[] y = new double[nPoints];
		double alpha, sinalpha, cosalpha;

		for (int i = 0; i < nPoints; i++ ) 		  {
			alpha = i * 2 * Math.PI / nPoints ;
			sinalpha = Math.sin(alpha);
			cosalpha = Math.cos(alpha);
			x[i] = xc + a * cosalpha * cosphi - b * sinalpha * sinphi;
			y[i] = yc + a * cosalpha * sinphi + b * sinalpha * cosphi;
		}
		return new double[][] {x, y};
	}

	@Override
	public void setParameters(double[] params) {
		this.params = params;
	}

	public boolean contains(Point2D p) {
		return getPrimitiveShape().contains(p);
	}

	public boolean contains(Rectangle2D r) {
		return getPrimitiveShape().contains(r);
	}

	public boolean contains(double x, double y) {
		return getPrimitiveShape().contains(x, y);
	}

	public boolean contains(double x, double y, double w, double h) {
		return getPrimitiveShape().contains(x, y, w, h);
	}

	public Rectangle getBounds() {
		return getPrimitiveShape().getBounds();
	}

	public Rectangle2D getBounds2D() {
		return getPrimitiveShape().getBounds2D();
	}

	public PathIterator getPathIterator(AffineTransform at) {
		return getPrimitiveShape().getPathIterator(at);
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return getPrimitiveShape().getPathIterator(at, flatness);
	}

	public boolean intersects(Rectangle2D r) {
		return getPrimitiveShape().intersects(r);
	}

	public boolean intersects(double x, double y, double w, double h) {
		return getPrimitiveShape().intersects(x, y, w, h);
	}

	protected Shape getPrimitiveShape() {
		final double xc  = params[0];
		final double yc  = params[1];
		final double a   = params[2];
		final double b   = params[3];
		final double phi = params[4];
		final Ellipse2D el = new Ellipse2D.Double(xc-a, yc-b, 2*a, 2*b);
		return AffineTransform.getRotateInstance(phi, xc, yc).createTransformedShape(el);
	}
}