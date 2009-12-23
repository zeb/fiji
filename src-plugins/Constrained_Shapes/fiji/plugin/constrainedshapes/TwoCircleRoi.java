package fiji.plugin.constrainedshapes;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class TwoCircleRoi extends ShapeRoi implements MouseListener, MouseMotionListener, ImageListener, PlugIn {


	/*
	 * FIELDS
	 */
	
	private static final long serialVersionUID = 1L;
	/** Min dist to drag a handle, in unzoomed pixels */
	private static final double DRAG_TOLERANCE = 10;
	
	private TwoCircleShape tcs;
	private ArrayList<Handle> handles = new ArrayList<Handle>(4);
	private AffineTransform canvas_affine_transform = new AffineTransform();
	private InteractionStatus status;
	private Point start_drag;
	private Toolbar toolbar;
	private int toolID = -1;
	
	/**
	 * If true, handles for user interaction will be drawn.
	 */
	public boolean displayHandle = true;
	
	/*
	 * INNER CLASS * ENUMS
	 */
	
	
	/**
	 * This internal class is used to deal with the small handles that appear on
	 * the ROI. They are used to resize the shape when the user click-drags on 
	 * them.
	 */
	private static class Handle {
		private static final long serialVersionUID = 1L;
		private static final Color HANDLE_COLOR = Color.WHITE;
		
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
		public Handle(double _x, double _y, Type _type) {
			this.center = new Point2D.Double(_x, _y);
			this.type = _type;
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
	 * Enum type to specify the current user interaction status.
	 */
	private static enum InteractionStatus { FREE, MOVING_ROI, MOVING_C1, MOVING_C2, RESIZING_C1, RESIZING_C2, CREATING_C1, CREATING_C2 };


	/*
	 * CONSTRUCTOR
	 */
	
	/**
	 * Empty constructor, needed to be run as a plugin
	 */
	public TwoCircleRoi() {
		super(new Roi(0,0,1,1)); // dummy super constructor, we only want the ROI methods  
		this.tcs = new TwoCircleShape();
		this.status = InteractionStatus.CREATING_C1;
	}
	
	public TwoCircleRoi(TwoCircleShape _tcs) {
		super(new Roi(0,0,1,1)); // dummy super constructor, we only want the ROI methods  
		this.tcs = _tcs;
		this.status = InteractionStatus.FREE;
	}

	
	/*
	 * RUN METHOD
	 */
	
	public void run(String arg) {
		toolbar = Toolbar.getInstance();
		if (toolbar == null) {
			IJ.error("No toolbar found");
			return;
		}

		toolID = toolbar.addTool(getToolName() + " - "	+ getToolIcon());
		if (toolID < 0) {
			IJ.error("Could not register tool");
			return;
		}
		toolbar.setTool(toolID);
		registerTool();
	}
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Draw this ROI on the current {@link ImageCanvas}. 
	 * Overrides the {@link ShapeRoi#draw(java.awt.Graphics)} method, so that 
	 * we can draw our shape with handles.
	 */
	public void draw(Graphics g) {
		if (ic == null) {return;}
		refreshAffineTransform();
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Roi.getColor());		
		g2.draw(canvas_affine_transform.createTransformedShape(tcs));
		if (displayHandle) {
			prepareHandles();
			drawHandles(g);
		}
	}
	
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
			canvas_affine_transform.transform(h.center, coords);
			dist = coords.distance(p);
			if (dist < DRAG_TOLERANCE) {
				return h.type.getClickLocation();
			}
		}
		if (canvas_affine_transform.createTransformedShape(tcs).contains(p)) {
			return ClickLocation.INSIDE;
		} 
		return ClickLocation.OUTSIDE;
	}
	
	public String getToolIcon() {
		return "C000D38D39D3dD53D62D63D7dD8cD9cDc2Dc3Dc9DcaDd3Dd9" +
				"C000D29D2aD2cD2dD37D3aD3cD3eD43D44D45D47D48D4dD4e" +
				"D54D55D61D6dD6eD71D72D7cD7eD81D82D8dD9bDa1Da2Daa" +
				"DabDb1Db2DbaDbbDc1DcbDd4Dd5Dd7Dd8De3De4De5De7De8De9" +
				"C000C111C222C333C444D1aD1bD1cD28D2bD2eD35D36D3bD46" +
				"D49D4fD52D56D57D5dD5eD5fD6fD80D8bD8eD90D91D92D9aDa0" +
				"DacDd2Dd6DdaDe6Df5Df6Df7C444C555C666C777C888D19D1dD34" +
				"D3fD42D51D58D64D70D73D7bD7fD8aD9dDb0Db3Db9DbcDc4Dc8Dd1" +
				"DdbDe2DeaDf4Df8C888C999CaaaCbbbD18D1eD27D2fD33D41D4aD4c" +
				"D59D60D65D6cD7aD83D8fD9eDa3Da9Dc0Dc5Dc7DccDe1DebDf3Df9" +
				"CbbbCcccCdddCeeeCfff";
	}
	
	public String getToolName() {
		return "Two_circle_shape";
	}
	
	public GeomShape getShape() {
		return tcs;
	}
	
	
	/*
	 * DEFAULT VISIBILITY METHODS
	 */
	
	void registerTool() {
		int[] ids = WindowManager.getIDList();
		if (ids != null)
			for (int id : ids)
				registerTool(WindowManager.getImage(id));
		ImagePlus.addImageListener(this);
	}

	void registerTool(ImagePlus image) {
		if (image == null)
			return;
		registerTool(image.getCanvas());
	}

	void registerTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
	}

	void unregisterTool() {
		for (int id : WindowManager.getIDList())
			unregisterTool(WindowManager.getImage(id));
		ImagePlus.removeImageListener(this);
	}

	void unregisterTool(ImagePlus image) {
		if (image == null)
			return;
		unregisterTool(image.getCanvas());
	}

	void unregisterTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
	}

	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Refresh the {@link AffineTransform} of this shape, according to current {@link ImageCanvas}
	 * settings. It is normally called only by the {@link #draw(Graphics)} method, for it
	 * is called every time something changed in the canvas, and that is when this transform needs
	 * to be updated.
	 */
	private void refreshAffineTransform() {
		canvas_affine_transform = new AffineTransform();
		if (ic == null) { return; }
		final double mag = ic.getMagnification();
		final Rectangle r = ic.getSrcRect();
		canvas_affine_transform.setTransform(mag, 0.0, 0.0, mag, -r.x*mag, -r.y*mag);
	}
	
	/**
	 * Regenerate the {@link #handles} field. The {@link Handle} coordinates are generated
	 * with respect to the {@link TwoCircleShape} object. They will be transformed in the 
	 * {@link ImageCanvas} coordinates when drawn.
	 */
	private void prepareHandles() {	
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
	 * object given. The {@link #canvas_affine_transform} is used to position
	 * the handles correctly with respect to the canvas zoom level.
	 */
	private void drawHandles(Graphics g) {
		double size = Math.min(tcs.getParameters()[2],tcs.getParameters()[5]) * mag;
		int handle_size;
		if (size>10) {
			handle_size = 8;
		} else if (size>5) {
			handle_size = 6;
		} else {			
			handle_size = 4;
		}
		for (Handle h : handles) {
			h.size = handle_size;
			h.draw(g, canvas_affine_transform);
		}
	}
	
	/**
	 * Reset this ROI to null, and make it ready to be re-drawn by user interaction
	 */
	private void reset() {
		this.tcs = new TwoCircleShape();
		this.status = InteractionStatus.CREATING_C1;
		handles.clear();
	}
	
	/*
	 * MAIN METHOD
	 */
	
	/**
	 * For testing purposes.
	 */
	public static void main(String[] args) {
		final Point2D.Float C1 = new Point2D.Float(200 ,150);
		final Point2D.Float C2 = new Point2D.Float(250 ,200);
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

	/*
	 * MOUSELISTENER METHODS
	 */
	
	
	public void mousePressed(MouseEvent e) { 

		if (Toolbar.getInstance() != toolbar) {
			unregisterTool();
			IJ.showStatus("unregistered " + getToolName() + " Tool");
			return;
		}
		if (Toolbar.getToolId() != toolID)
			return;

		
		ImageCanvas canvas = (ImageCanvas) e.getSource();
		ic = canvas;
		imp = ic.getImage();
		
		Point p = e.getPoint();		
		ClickLocation cl = getClickLocation(p);
		switch (cl) {
		case OUTSIDE:
			switch (status) {
			case CREATING_C1:
				tcs.setC1(p);
				break;
			case CREATING_C2:
				tcs.setC2(p);
				break;
			default:
				// If drag is small, then we will kill this roi
			}
			imp.setRoi(this);
			break;
		default:
			status = cl.getInteractionStatus();
		}
		start_drag = p;
	}

	public void mouseDragged(MouseEvent e) {
		
		if (Toolbar.getToolId() != toolID) return;
		
		refreshAffineTransform();
		Point p = e.getPoint();
		Point2D c = new Point2D.Float();
		final double[] params = tcs.getParameters();
		
		switch (status) {
		case MOVING_ROI:
			params[0] += (p.x-start_drag.x)/canvas_affine_transform.getScaleX();
			params[3] += (p.x-start_drag.x)/canvas_affine_transform.getScaleX();
			params[1] += (p.y-start_drag.y)/canvas_affine_transform.getScaleY();
			params[4] += (p.y-start_drag.y)/canvas_affine_transform.getScaleY();
			break;
		case MOVING_C1:
			params[0] += (p.x-start_drag.x)/canvas_affine_transform.getScaleX();
			params[1] += (p.y-start_drag.y)/canvas_affine_transform.getScaleY();
			break;
		case MOVING_C2:
			params[3] += (p.x-start_drag.x)/canvas_affine_transform.getScaleX();
			params[4] += (p.y-start_drag.y)/canvas_affine_transform.getScaleY();
			break;
		case RESIZING_C1:
		case CREATING_C1:
			canvas_affine_transform.transform(tcs.getC1(), c);
			params[2] = c.distance(p)/canvas_affine_transform.getScaleX();			
			break;
		case RESIZING_C2:
		case CREATING_C2:
			canvas_affine_transform.transform(tcs.getC2(), c);
			params[5] = c.distance(p)/canvas_affine_transform.getScaleX();			
			break;
			
		}
		start_drag = p;
		imp.draw(); // This will in turn call the draw(Graphics) method of this object
	}

	public void mouseReleased(MouseEvent e) {
		
		if (Toolbar.getToolId() != toolID) return;
		
		switch (status) {
		case CREATING_C1:
			status = InteractionStatus.CREATING_C2;
			break;
		case CREATING_C2:
			status = InteractionStatus.FREE;
			break;
		}
	}
	
	public void mouseClicked(MouseEvent e) {
		
		if (Toolbar.getToolId() != toolID) return;

		ClickLocation cl = getClickLocation(e.getPoint());
		if (cl == ClickLocation.OUTSIDE ) {
			reset();
			imp.killRoi();
			status = InteractionStatus.CREATING_C1;
		}
	}
	
	public void mouseMoved(MouseEvent e) {	}
	public void mouseEntered(MouseEvent e) {	}
	public void mouseExited(MouseEvent e) {	}

	/*
	 * IMAGELISTENER METHODS
	 */

	public void imageClosed(ImagePlus imp) {
		unregisterTool(imp);
	}

	public void imageOpened(ImagePlus imp) {
		registerTool(imp);		
	}

	public void imageUpdated(ImagePlus imp) {	}
	
	
}
