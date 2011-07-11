package fiji.plugin.constrainedshapes;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class CircleShape extends ParameterizedShape {
	/**
	 * Parameter array for this shape. As specified in the mother abstract class {@link ParameterizedShape},
	 * we store them as a double array of 3 elements. Array content is the following:
	 * <ul>
	 * 	<li> [0]: <code>xc</code>, the x coordinate of circle center
	 * 	<li> [1]: <code>yc</code>, the y coordinate of circle center
	 * 	<li> [2]: <code>r</code>, the radius of the circle
	 * </ul>
	 */
	protected double[] params = new double[3];


	/*
	 * CONSTRUCTORS
	 */

	public CircleShape() {
		this(Double.NaN, Double.NaN, Double.NaN);
	}

	public CircleShape(double xc, double yc, double r) {
		params[0] = xc;
		params[1] = yc;
		params[2] = r;
	}

	/*
	 * PUBLIC METHODS
	 */

	public void setCenter(Point2D p) {
		params[0] = p.getX();
		params[1] = p.getY();
	}

	public Point2D getCenter() {
		return new Point2D.Double(params[0], params[1]);
	}

	public double getRadius() {
		return params[2];
	}

	public void setRadius(double r) {
		params[2] = r;
	}

	public String toString() {
		return String.format("xc=%5.0f, yc=%5.0f, r=%5.0f",
				params[0], params[1], params[2]);
	}

	/*
	 * GEOMSHAPE METHODS
	 */


	@Override
	public double[][] sample(int nPoints) {
		final double xc = params[0];
		final double yc = params[1];
		final double r  = params[2];

		final double[] x = new double[nPoints];
		final double[] y = new double[nPoints];

		double angle;
		for (int i = 0; i < nPoints; i++) {
			angle = 2*Math.PI * i / nPoints;
			x[i] = xc + r * Math.cos(angle);
			y[i] = yc + r * Math.sin(angle);
		}
		return new double[][] {x, y};
	}

	@Override
	public ParameterizedShape clone() {
		CircleShape circle = new CircleShape();
		circle.setParameters(params.clone());
		return circle;
	}

	@Override
	public int getNumParameters() {
		return 3;
	}

	@Override
	public double[] getParameters() {
		return params;
	}

	@Override
	public void setParameters(double[] params) {
		this.params = params;
	}

	public boolean contains(Point2D p) {
		return getCircle().contains(p);
	}

	public boolean contains(Rectangle2D r) {
		return getCircle().contains(r);
	}

	public boolean contains(double x, double y) {
		return getCircle().contains(x, y);
	}

	public boolean contains(double x, double y, double w, double h) {
		return getCircle().contains(x, y, w, h);
	}

	public Rectangle getBounds() {
		final int xc = (int) params[0];
		final int yc = (int) params[1];
		final int d  = (int) (2*params[2]);
		return new Rectangle(xc-d/2, yc-d/2, d, d);
	}

	public Rectangle2D getBounds2D() {
		final double xc = params[0];
		final double yc = params[1];
		final double r  = params[2];
		return new Rectangle2D.Double(xc-2*r, yc-2*r, 2*r, 2*r);
	}

	public PathIterator getPathIterator(AffineTransform at) {
		return getCircle().getPathIterator(at);
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return getCircle().getPathIterator(at, flatness);
	}

	public boolean intersects(Rectangle2D r) {
		return getCircle().intersects(r);
	}

	public boolean intersects(double x, double y, double w, double h) {
		return getCircle().intersects(x, y, w, h);
	}

	protected Ellipse2D getCircle() {
		final double xc = params[0];
		final double yc = params[1];
		final double r  = params[2];
		return new Ellipse2D.Double(xc-r, yc-r, 2*r, 2*r);
	}
}