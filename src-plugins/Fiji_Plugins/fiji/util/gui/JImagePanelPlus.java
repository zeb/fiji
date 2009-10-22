package fiji.util.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.Menus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RoiBrush;
import ij.gui.StackWindow;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.gui.ShapeRoi;
import ij.macro.Interpreter;
import ij.macro.MacroRunner;
import ij.plugin.WandToolOptions;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.util.Java2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.IndexColorModel;
import java.util.Hashtable;
import java.util.Vector;

public class JImagePanelPlus extends JImagePanel implements MouseListener, MouseMotionListener
{
	/** current cursor offscreen x location */
	protected int xMouse = 0;  
	/** current cursor offscreen y location */
	protected int yMouse = 0;
	
	/** mouse exited flag */
	private boolean mouseExited = true;

	/** cursor status */
	private boolean showCursorStatus = true;
	/** disable popup menu flag */
	private boolean disablePopupMenu;
	/** show all ROIs flag */
	private boolean showAllROIs = false;
	/** custom ROI flag */
	private boolean customRoi = false;
	
	/** allowed zoom levels */
	private static final double[] zoomLevels = {
		1/72.0, 1/48.0, 1/32.0, 1/24.0, 1/16.0, 1/12.0, 
		1/8.0, 1/6.0, 1/4.0, 1/3.0, 1/2.0, 0.75, 1.0, 1.5,
		2.0, 3.0, 4.0, 6.0, 8.0, 12.0, 16.0, 24.0, 32.0 };
	
	private Vector<Roi> displayList, showAllList;
	private Rectangle[] labelRects;
	private Color listColor;
	private static Font smallFont, largeFont;
	private static final int LIST_OFFSET = 100000;
	private static Color showAllColor = Prefs.getColor(Prefs.SHOW_ALL_COLOR, new Color(128, 255, 255));
    private static Color labelColor;
    private BasicStroke listStroke;
    private boolean labelListItems;
    private static Color zoomIndicatorColor;
    private boolean maxBoundsReset;
    
    private Image offScreenImage;
	private int offScreenWidth = 0;
	private int offScreenHeight = 0;
	
	/** ROI starting screen x- coordinate */
	private int sx2 = 0;
	/** ROI starting screen y- coordinate */
	private int sy2 = 0;
	
	/**
	 * Constructor with ImageJ interaction
	 * @param imp input image
	 */
	public JImagePanelPlus(ImagePlus imp) 
	{
		super(imp);
		super.ij = IJ.getInstance();
		super.imageWidth = imp.getWidth();
		super.imageHeight = imp.getHeight();		
		srcRect = new Rectangle(0, 0, super.imageWidth, super.imageHeight);
		setDrawingSize(super.imageWidth, (int)(super.imageHeight));
		magnification = 1.0;
 		addMouseListener(this);
 		addMouseMotionListener(this);
 		addKeyListener(super.ij);  // ImageJ handles keyboard shortcuts
		setFocusTraversalKeysEnabled(false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) 
	{
		//autoScroll(e);
		ImageWindow win = imp.getWindow();
		if (win!=null)
			setCursor(defaultCursor);
		IJ.showStatus("");
		mouseExited = true;		
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		if (super.ij == null) 
			return;
		showCursorStatus = true;
		
		final int toolID = Toolbar.getToolId();
		final ImageWindow win = imp.getWindow();
		if (win!=null && win.running2 && toolID!=Toolbar.MAGNIFIER) 
		{
			win.running2 = false;
			return;
		}
		
		final int x = e.getX();
		final int y = e.getY();
		flags = e.getModifiers();
		
		//IJ.log("Mouse pressed: " + e.isPopupTrigger() + "  " + ij.modifiers(flags) + " button: " + e.getButton() + ": " + e);		
		//if (toolID!=Toolbar.MAGNIFIER && e.isPopupTrigger()) {
		if (toolID!=Toolbar.MAGNIFIER && ((e.isPopupTrigger() && e.getButton() != 0)||(!IJ.isMacintosh()&&(flags&Event.META_MASK)!=0))) 
		{
			handlePopupMenu(e);
			return;
		}

		int ox = offScreenX(x);
		int oy = offScreenY(y);
		xMouse = ox; 
		yMouse = oy;
		
		if (IJ.spaceBarDown()) 
		{
			// temporarily switch to "hand" tool of space bar down
			setupScroll(ox, oy);
			return;
		}
		if (showAllROIs) 
		{
			final int size = Roi.HANDLE_SIZE+3;
			final int halfSize = size/2;
			final Roi roi = imp.getRoi();
			
			if (!(roi!=null && (roi.contains(ox, oy) || 
					roi.isHandle(x, y, screenX(roi.getBounds().x) - halfSize, screenY(roi.getBounds().y) - halfSize, screenX(roi.getBounds().x+roi.getBounds().width) - halfSize, screenY(roi.getBounds().y+roi.getBounds().height) - halfSize)>=0)) 
					&& roiManagerSelect(x, y))
 				return;
		}
		if (customRoi && displayList!=null)
			return;

		switch (toolID) 
		{
			case Toolbar.MAGNIFIER:
				if (IJ.shiftKeyDown())
					zoomToSelection(ox, oy);
				else if ((flags & (Event.ALT_MASK|Event.META_MASK|Event.CTRL_MASK))!=0)
					//IJ.run("Out");
					this.zoomOut(this.screenX(ox), this.screenY(oy));
				else
					//IJ.run("In");
					this.zoomIn(this.screenX(ox), this.screenY(oy));
				break;
			case Toolbar.HAND:
				setupScroll(ox, oy);
				break;
			case Toolbar.DROPPER:
				setDrawingColor(ox, oy, IJ.altKeyDown());
				break;
			case Toolbar.WAND:
				final Roi roi = imp.getRoi();
				final int size = Roi.HANDLE_SIZE+3;
				final int halfSize = size/2;
				
				if (roi!=null && roi.contains(ox, oy)) {
					Rectangle r = roi.getBounds();
					if (r.width==imageWidth && r.height==imageHeight)
						imp.killRoi();
					else if (!e.isAltDown()) {
						handleRoiMouseDown(e);
						return;
					}
				}
				if (roi!=null) 
				{
					int handle = roi.isHandle(x, y, screenX(roi.getBounds().x) - halfSize, screenY(roi.getBounds().y) - halfSize, screenX(roi.getBounds().x+roi.getBounds().width) - halfSize, screenY(roi.getBounds().y+roi.getBounds().height) - halfSize);
					if (handle>=0) {
						roi.mouseDownInHandle(handle, x, y);
						return;
					}
				}
				setRoiModState(e, roi, -1);
				String mode = WandToolOptions.getMode();
				double tolerance = WandToolOptions.getTolerance();
				int npoints = IJ.doWand(ox, oy, tolerance, mode);
				if (Recorder.record && npoints>0) {
					if (tolerance==0.0 && mode.equals("Legacy"))
						Recorder.record("doWand", ox, oy);
					else
						Recorder.recordString("doWand("+ox+", "+oy+", "+tolerance+", \""+mode+"\");\n");
				}
				break;
			case Toolbar.OVAL:
				if (Toolbar.getBrushSize()>0)
					new RoiBrush();
				else
					handleRoiMouseDown(e);
				break;
			case Toolbar.SPARE1: case Toolbar.SPARE2: case Toolbar.SPARE3: 
			case Toolbar.SPARE4: case Toolbar.SPARE5: case Toolbar.SPARE6:
			case Toolbar.SPARE7: case Toolbar.SPARE8: case Toolbar.SPARE9:
				Toolbar.getInstance().runMacroTool(toolID);
				break;
			default:  //selection tool
				handleRoiMouseDown(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) 
	{
		flags = e.getModifiers();
		flags &= ~InputEvent.BUTTON1_MASK; // make sure button 1 bit is not set
		flags &= ~InputEvent.BUTTON2_MASK; // make sure button 2 bit is not set
		flags &= ~InputEvent.BUTTON3_MASK; // make sure button 3 bit is not set
		final Roi roi = imp.getRoi();
		if (roi != null) 
		{
			final Rectangle r = roi.getBounds();
			final int type = roi.getType();
			if ((r.width==0 || r.height==0)
					&& !(type == Roi.POLYGON||type == Roi.POLYLINE||type == Roi.ANGLE || type == Roi.LINE)
					&& !(roi instanceof TextRoi)
					&& roi.getState() == Roi.CONSTRUCTING
					&& type != Roi.POINT)
				imp.killRoi();
			else 
			{
				roi.handleMouseUp(e.getX(), e.getY(), this.getModifiers());
				if (roi.getType()==Roi.LINE && roi.getLength()==0.0)
					imp.killRoi();
			}
		}
		repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) 
	{
		int x = e.getX();
		int y = e.getY();
		xMouse = offScreenX(x);
		yMouse = offScreenY(y);
		flags = e.getModifiers();
		//IJ.log("mouseDragged: "+flags);
		if (flags==0)  // workaround for Mac OS 9 bug
			flags = InputEvent.BUTTON1_MASK;
		if (Toolbar.getToolId()==Toolbar.HAND || IJ.spaceBarDown())
			scroll(x, y);
		else {
			IJ.setInputEvent(e);
			Roi roi = imp.getRoi();
			if (roi != null)
				roi.handleMouseDragOffScreenCoords(xMouse, yMouse, flags);
		}
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) 
	{
		if (super.ij == null) 
			return;
		int sx = e.getX();
		int sy = e.getY();
		int ox = offScreenX(sx);
		int oy = offScreenY(sy);
		super.flags = e.getModifiers();
		setCursor(sx, sy, ox, oy);
		IJ.setInputEvent(e);
		final Roi roi = imp.getRoi();
		if (roi!=null && (roi.getType()==Roi.POLYGON || roi.getType()==Roi.POLYLINE || roi.getType()==Roi.ANGLE) 
				&& roi.getState()==Roi.CONSTRUCTING) 
		{
			PolygonRoi pRoi = (PolygonRoi)roi;
			pRoi.handleMouseMove(ox, oy);
		} 
		else 
		{
			if (ox<imageWidth && oy<imageHeight) 
			{
				ImageWindow win = imp.getWindow();
				// Cursor must move at least 12 pixels before text
				// displayed using IJ.showStatus() is overwritten.
				if ((sx-sx2)*(sx-sx2)+(sy-sy2)*(sy-sy2)>144)
					showCursorStatus = true;
				if (win!=null&&showCursorStatus) win.mouseMoved(ox, oy);
			} 
			else
				IJ.showStatus("");

		}						
	}
	
	/** 
	 * Sets the cursor based on the current tool and cursor location. 
	 * */
	public void setCursor(int sx, int sy, int ox, int oy) 
	{
		this.xMouse = ox;
		this.yMouse = oy;
		this.mouseExited = false;
		final Roi roi = super.imp.getRoi();
		final int size = Roi.HANDLE_SIZE+3;
		final int halfSize = size/2;
		
		ImageWindow win = super.imp.getWindow();
		if (win==null)
			return;
		if (IJ.spaceBarDown()) 
		{
			setCursor(handCursor);
			//IJ.log("hand cursor 1");
			return;
		}
		int id = Toolbar.getToolId();
		switch (Toolbar.getToolId()) 
		{
			case Toolbar.MAGNIFIER:
				setCursor(moveCursor);
				//IJ.log("move cursor 1");
				break;
			case Toolbar.HAND:
				setCursor(handCursor);
				//IJ.log("hand cursor 2");
				break;
			default:  //selection tool
				if (id==Toolbar.SPARE1 || id>=Toolbar.SPARE2) {
					if (Prefs.usePointerCursor)
					{
						setCursor(defaultCursor);
						//IJ.log("default cursor 1");
					}
					else
					{
						setCursor(crosshairCursor);
						//IJ.log("cross cursor 1");
					}
				} 
				else if (roi!=null && roi.getState() != Roi.CONSTRUCTING && 
						roi.isHandle(sx, sy, screenX(roi.getBounds().x) - halfSize, screenY(roi.getBounds().y) - halfSize, screenX(roi.getBounds().x+roi.getBounds().width) - halfSize, screenY(roi.getBounds().y+roi.getBounds().height) - halfSize) >= 0)
				{
					setCursor(handCursor);
					//IJ.log("hand cursor 2");
				}
				else if (Prefs.usePointerCursor || (roi!=null && roi.getState()!=Roi.CONSTRUCTING && roi.contains(ox, oy)))
				{
					setCursor(defaultCursor);
					//IJ.log("default cursor 2");
				}
				else
				{
					setCursor(crosshairCursor);
					//IJ.log("cross cursor 2");
				}
		}
	}
	
	/** 
	 * Called by IJ.showStatus() to prevent status bar text from
	 * being overwritten until the cursor moves at least 12 pixels. 
	 */
	public void setShowCursorStatus(boolean status) 
	{
		showCursorStatus = status;
		if (status==true)
			sx2 = sy2 = -1000;
		else 
		{
			sx2 = screenX(xMouse);
			sy2 = screenY(yMouse);
		}
	}
	
	/**
	 * Handle popup menu 
	 * @param e mouse event
	 */
	protected void handlePopupMenu(MouseEvent e) 
	{
		if (disablePopupMenu) 
			return;
		if (IJ.debugMode) IJ.log("show popup: " + (e.isPopupTrigger()?"true":"false"));
		int x = e.getX();
		int y = e.getY();
		Roi roi = imp.getRoi();
		if (roi!=null && (roi.getType()==Roi.POLYGON || roi.getType()==Roi.POLYLINE || roi.getType()==Roi.ANGLE)
		&& roi.getState()==Roi.CONSTRUCTING) 
		{
			roi.handleMouseUp(x, y, this.getModifiers()); // simulate double-click to finalize
			roi.handleMouseUp(x, y, this.getModifiers()); // polygon or polyline selection
			return;
		}
		PopupMenu popup = Menus.getPopupMenu();
		if (popup!=null) {
			add(popup);
			if (IJ.isMacOSX()) IJ.wait(10);
			popup.show(this, x, y);
		}
	}
	
	/**
	 * Setup scroll origin
	 * @param ox x- coordinate of scroll origin
	 * @param oy y- coordinate of scroll origin
	 */
	protected void setupScroll(int ox, int oy) 
	{
		xMouseStart = ox;
		yMouseStart = oy;
		xSrcStart = srcRect.x;
		ySrcStart = srcRect.y;
	}
	
	/**
	 * Zoom to selection
	 * 
	 * @param x
	 * @param y
	 */
	void zoomToSelection(int x, int y) 
	{
		IJ.setKeyUp(IJ.ALL_KEYS);
		String macro =
			"args = split(getArgument);\n"+
			"x1=parseInt(args[0]); y1=parseInt(args[1]); flags=20;\n"+
			"while (flags&20!=0) {\n"+
				"getCursorLoc(x2, y2, z, flags);\n"+
				"if (x2>=x1) x=x1; else x=x2;\n"+
				"if (y2>=y1) y=y1; else y=y2;\n"+
				"makeRectangle(x, y, abs(x2-x1), abs(y2-y1));\n"+
				"wait(10);\n"+
			"}\n"+
			"run('To Selection');\n";
		new MacroRunner(macro, x+" "+y);
		repaint();
	}

	/**
	 * ROI Manager selection
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
    boolean roiManagerSelect(int x, int y) 
    {
		final RoiManager rm = RoiManager.getInstance();
		if (rm==null) return false;
		Hashtable rois = rm.getROIs();
		java.awt.List list = rm.getList();
		int n = list.getItemCount();
		if (labelRects==null || labelRects.length!=n) return false;
		for (int i=0; i<n; i++) 
		{
			if (labelRects[i]!=null && labelRects[i].contains(x,y)) 
			{
				//rm.select(i);
				// this needs to run on a separate thread, at least on OS X
				// "update2" does not clone the ROI so the "Show All"
				// outline moves as the user moves the RO.
				new ij.macro.MacroRunner("roiManager('select', "+i+"); roiManager('update2');");
				return true;
			}
		}
		return false;
    }

    /**
     * Set drawing color 
     * 
     * @param ox
     * @param oy
     * @param setBackground
     */
	protected void setDrawingColor(int ox, int oy, boolean setBackground) 
	{		
		int type = imp.getType();
		int[] v = imp.getPixel(ox, oy);
		switch (type) {
			case ImagePlus.GRAY8: {
				if (setBackground)
					setBackgroundColor(getColor(v[0]));
				else
					setForegroundColor(getColor(v[0]));
				break;
			}
			case ImagePlus.GRAY16: case ImagePlus.GRAY32: {
				double min = imp.getProcessor().getMin();
				double max = imp.getProcessor().getMax();
				double value = (type==ImagePlus.GRAY32)?Float.intBitsToFloat(v[0]):v[0];
				int index = (int)(255.0*((value-min)/(max-min)));
				if (index<0) index = 0;
				if (index>255) index = 255;
				if (setBackground)
					setBackgroundColor(getColor(index));
				else
					setForegroundColor(getColor(index));
				break;
			}
			case ImagePlus.COLOR_RGB: case ImagePlus.COLOR_256: {
				Color c = new Color(v[0], v[1], v[2]);
				if (setBackground)
					setBackgroundColor(c);
				else
					setForegroundColor(c);
				break;
			}
		}
		Color c;
		if (setBackground)
			c = Toolbar.getBackgroundColor();
		else {
			c = Toolbar.getForegroundColor();
			imp.setColor(c);
		}
		IJ.showStatus("("+c.getRed()+", "+c.getGreen()+", "+c.getBlue()+")");
	}

	/**
	 * Get index color
	 * @param index
	 * @return Color at specified index
	 */
	Color getColor(int index)
	{
		IndexColorModel cm = (IndexColorModel)imp.getProcessor().getColorModel();
		//IJ.write(""+index+" "+(new Color(cm.getRGB(index))));
		return new Color(cm.getRGB(index));
	}
	
	/**
	 * Set foreground color
	 * @param c new foreground color
	 */
	private void setForegroundColor(Color c) {
		Toolbar.setForegroundColor(c);
		if (Recorder.record)
			Recorder.record("setForegroundColor", c.getRed(), c.getGreen(), c.getBlue());
	}
	/**
	 * Set background color
	 * @param c new background color
	 */
	private void setBackgroundColor(Color c) {
		Toolbar.setBackgroundColor(c);
		if (Recorder.record)
			Recorder.record("setBackgroundColor", c.getRed(), c.getGreen(), c.getBlue());
	}
	
	/**
	 * Handle ROI when mouse down
	 * 
	 * @param e
	 */
	protected void handleRoiMouseDown(MouseEvent e) 
	{
		final int sx = e.getX();
		final int sy = e.getY();
		final int ox = offScreenX(sx);
		final int oy = offScreenY(sy);
		final Roi roi = imp.getRoi();
		final int size = Roi.HANDLE_SIZE+3;
		final int halfSize = size/2;
		
		int handle = roi!=null?roi.isHandle(sx, sy, screenX(roi.getBounds().x) - halfSize, screenY(roi.getBounds().y) - halfSize, screenX(roi.getBounds().x+roi.getBounds().width) - halfSize, screenY(roi.getBounds().y+roi.getBounds().height) - halfSize):-1;		
		setRoiModState(e, roi, handle);
		if (roi!=null) {
			if (handle>=0) {
				roi.mouseDownInHandle(handle, sx, sy);
				return;
			}
			Rectangle r = roi.getBounds();
			int type = roi.getType();
			if (type==Roi.RECTANGLE && r.width==imp.getWidth() && r.height==imp.getHeight()
			&& roi.getPasteMode()==Roi.NOT_PASTING) {
				imp.killRoi();
				return;
			}
			if (roi.contains(ox, oy)) {
				if (roi.modState==Roi.NO_MODS)
					roi.handleMouseDownScreenCoords(offScreenX(sx), offScreenY(sy));
				else {
					imp.killRoi();
					imp.createNewRoi(sx,sy);
				}
				return;
			}
			if ((type==Roi.POLYGON || type==Roi.POLYLINE || type==Roi.ANGLE)
			&& roi.getState()==Roi.CONSTRUCTING)
				return;
			int tool = Toolbar.getToolId();
			if ((tool==Toolbar.POLYGON||tool==Toolbar.POLYLINE||tool==Toolbar.ANGLE)&& !(IJ.shiftKeyDown()||IJ.altKeyDown())) {
				imp.killRoi();
				return;
			}
		}
		imp.createNewRoi(sx,sy);
	}
	
	void setRoiModState(MouseEvent e, Roi roi, int handle) 
	{
		if (roi==null || (handle>=0 && roi.modState==Roi.NO_MODS))
			return;
		if (roi.state==Roi.CONSTRUCTING)
			return;
		int tool = Toolbar.getToolId();
		if (tool>Toolbar.FREEROI && tool!=Toolbar.WAND && tool!=Toolbar.POINT)
			{roi.modState = Roi.NO_MODS; return;}
		if (e.isShiftDown())
			roi.modState = Roi.ADD_TO_ROI;
		else if (e.isAltDown())
			roi.modState = Roi.SUBTRACT_FROM_ROI;
		else
			roi.modState = Roi.NO_MODS;
		//IJ.log("setRoiModState: "+roi.modState+" "+ roi.state);
	}
	
	
	public void update(Graphics g) {
		this.paint(g);
	}
	
	/**
	 * Paint method
	 * @param g graphics 
	 */
	public void paint(Graphics g) 
	{
		super.paint(g);
		
		final Roi roi = imp.getRoi();
		if (roi!=null || showAllROIs || displayList!=null) 
		{
			if (roi != null) 
			{
				roi.updatePaste();
				super.setImageUpdated();
			}
			if (!IJ.isMacOSX()) 
			{
				paintDoubleBuffered(g);
				return;
			}
		}
		
		if (roi != null) 
			roi.draw(g, this.getMagnification(), screenX(xMouse), screenY(yMouse) );
		if (showAllROIs) 
			showAllROIs(g);
		if (srcRect.width<imageWidth || srcRect.height<imageHeight)
			drawZoomIndicator(g);
		if (IJ.debugMode) 
			showFrameRate(g);				
		
	}
	
	
	// Use double buffer to reduce flicker when drawing complex ROIs.
	// Author: Erik Meijering
	void paintDoubleBuffered(Graphics g) 
	{
		final int srcRectWidthMag = (int)(srcRect.width*magnification);
		final int srcRectHeightMag = (int)(srcRect.height*magnification);
		if (offScreenImage==null || 
				offScreenWidth!=srcRectWidthMag || 
				offScreenHeight!=srcRectHeightMag) 
		{
			offScreenImage = createImage(srcRectWidthMag, srcRectHeightMag);
			offScreenWidth = srcRectWidthMag;
			offScreenHeight = srcRectHeightMag;
		}
		Roi roi = imp.getRoi();
		try {
			if (super.imageUpdated) 
			{
				super.imageUpdated = false;
				imp.updateImage();
			}
			Graphics offScreenGraphics = offScreenImage.getGraphics();
			Java2.setBilinearInterpolation(offScreenGraphics, Prefs.interpolateScaledImages);
			Image img = imp.getImage();
			if (img!=null)
				offScreenGraphics.drawImage(img, 0, 0, srcRectWidthMag, srcRectHeightMag,
					srcRect.x, srcRect.y, srcRect.x+srcRect.width, srcRect.y+srcRect.height, null);
			if (displayList!=null) 
				drawDisplayList(offScreenGraphics);
			if (roi!=null) 
				roi.draw(offScreenGraphics, magnification, this.screenX(roi.getBounds().x), this.screenY(roi.getBounds().y));
			if (showAllROIs) 
				showAllROIs(offScreenGraphics);
			if (srcRect.width<imageWidth ||srcRect.height<imageHeight)
				drawZoomIndicator(offScreenGraphics);
			if (IJ.debugMode) 
				showFrameRate(offScreenGraphics);
			g.drawImage(offScreenImage, 0, 0, null);
		}
		catch(OutOfMemoryError e) {IJ.outOfMemory("Paint");}
	}
	
	/**
	 * Draw the zoom indicator on the image
	 * @param g
	 */
	void drawZoomIndicator(Graphics g) 
	{
		int x1 = 10;
		int y1 = 10;
		double aspectRatio = (double)imageHeight/imageWidth;
		int w1 = 64;
		if (aspectRatio>1.0)
			w1 = (int)(w1/aspectRatio);
		int h1 = (int)(w1*aspectRatio);
		if (w1<4) w1 = 4;
		if (h1<4) h1 = 4;
		int w2 = (int)(w1*((double)srcRect.width/imageWidth));
		int h2 = (int)(h1*((double)srcRect.height/imageHeight));
		if (w2<1) w2 = 1;
		if (h2<1) h2 = 1;
		int x2 = (int)(w1*((double)srcRect.x/imageWidth));
		int y2 = (int)(h1*((double)srcRect.y/imageHeight));
		if (zoomIndicatorColor==null)
			zoomIndicatorColor = new Color(128, 128, 255);
		g.setColor(zoomIndicatorColor);
		g.drawRect(x1, y1, w1, h1);
		if (w2*h2<=200 || w2<10 || h2<10)
			g.fillRect(x1+x2, y1+y2, w2, h2);
		else
			g.drawRect(x1+x2, y1+y2, w2, h2);
	}
	
	/**
	 * Show the frame rate
	 * @param g
	 */
	void showFrameRate(Graphics g) 
	{
		frames++;
		if (System.currentTimeMillis()>firstFrame+1000) {
			firstFrame=System.currentTimeMillis();
			fps = frames;
			frames=0;
		}
		g.setColor(Color.white);
		g.fillRect(10, 12, 50, 15);
		g.setColor(Color.black);
		g.drawString((int)(fps+0.5) + " fps", 10, 25);
	}
	
	/**
	 * Show all ROIs
	 * @param g
	 */
    void showAllROIs(Graphics g) 
    {
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) 
		{
			rm = Interpreter.getBatchModeRoiManager();
			if (rm!=null && rm.getList().getItemCount()==0)
				rm = null;
		}
		if (rm==null) 
		{
			if (showAllList!=null)
				displayList = showAllList;
			showAllROIs = false;
			repaint();
			return;
		}
		initGraphics(g, null);
		Hashtable rois = rm.getROIs();
		java.awt.List list = rm.getList();
		boolean drawLabels = rm.getDrawLabels();
		int n = list.getItemCount();
		if (labelRects==null || labelRects.length!=n)
			labelRects = new Rectangle[n];
		if (!drawLabels)
			showAllList = new Vector<Roi>();
		else
			showAllList = null;
		for (int i=0; i<n; i++) 
		{
			String label = list.getItem(i);
			Roi roi = (Roi)rois.get(label);
			if (roi==null) continue;
			if (showAllList!=null)
				showAllList.addElement(roi);
			if (Prefs.showAllSliceOnly && imp.getStackSize()>1) 
			{
				int slice = getSliceNumber(roi.getName());
				if (slice==-1 || slice==imp.getCurrentSlice())
					drawRoi(g, roi, drawLabels?i:-1);
			} else
				drawRoi(g, roi, drawLabels?i:-1);
			if (i<100 && drawLabels && imp!=null && roi==imp.getRoi() && !(roi instanceof TextRoi)) 
			{
				Color lineColor = roi.getLineColor();
				Color fillColor = roi.getFillColor();
				int lineWidth = roi.getLineWidth();
				roi.setLineColor(null);
				roi.setFillColor(null);
				roi.setLineWidth(1);
				roi.draw(g, this.getMagnification(), screenX(xMouse), screenY(yMouse) );
				roi.setLineColor(lineColor);
				roi.setFillColor(fillColor);
				roi.setLineWidth(lineWidth);
			}
		}
    }
    
    /**
     * Draw ROI
     * 
     * @param g
     * @param roi
     * @param index
     */
    void drawRoi(Graphics g, Roi roi, int index) 
    {
		final ImagePlus imp2 = roi.getImage();
		
		roi.setImage(imp);
		
		final Color saveColor = roi.getLineColor();
		
		if (saveColor==null) {
			if (index>=0 || listColor==null)
				roi.setLineColor(showAllColor);
			else
				roi.setLineColor(listColor);
		}
		if (roi instanceof TextRoi)
			((TextRoi)roi).drawText(g);
		else
			roi.drawDisplayList(g, this.getMagnification(), screenX(roi.getBounds().x), screenY(roi.getBounds().y) );
		roi.setLineColor(saveColor);
		if (index >= 0) 
		{
			g.setColor(showAllColor);
			drawRoiLabel(g, index, roi.getBounds());
		}
		if (imp2!=null)
			roi.setImage(imp2);
		else
			roi.setImage(null);
    }
	
	void drawRoiLabel(Graphics g, int index, Rectangle r) 
	{
		int x = screenX(r.x);
		int y = screenY(r.y);
		
		final double mag = getMagnification();
		final int width = (int)(r.width*mag);
		final int height = (int)(r.height*mag);
		final int size = width>40 && height>40?12:9;
		
		if (size==12)
			g.setFont(largeFont);
		else
			g.setFont(smallFont);
		boolean drawingList = index >= LIST_OFFSET;
		if (drawingList) index -= LIST_OFFSET;
		String label = "" + (index+1);
		FontMetrics metrics = g.getFontMetrics();
		int w = metrics.stringWidth(label);
		x = x + width/2 - w/2;
		y = y + height/2 + Math.max(size/2,6);
		int h =  metrics.getHeight();
		g.fillRoundRect(x-1, y-h+2, w+1, h-3, 5, 5);
		if (!drawingList)
			labelRects[index] = new Rectangle(x-1, y-h+2, w+1, h-3);
		g.setColor(labelColor);
		g.drawString(label, x, y-2);
		g.setColor(showAllColor);
	}

	/**
	 * Draw display list
	 * @param g
	 */
    void drawDisplayList(Graphics g) 
    {
		initGraphics(g, listColor);
    	int n = displayList.size();
    	for (int i=0; i<n; i++)
    		drawRoi(g, (Roi)displayList.elementAt(i), labelListItems?i+LIST_OFFSET:-1);
    	if (listStroke!=null) ((Graphics2D)g).setStroke(new BasicStroke());
    }
	
	/**
	 * Init Graphics
	 */
    void initGraphics(Graphics g, Color c) 
    {
		if (smallFont==null) {
			smallFont = new Font("SansSerif", Font.PLAIN, 9);
			largeFont = new Font("SansSerif", Font.PLAIN, 12);
		}
		if (labelColor==null) {
			int red = showAllColor.getRed();
			int green = showAllColor.getGreen();
			int blue = showAllColor.getBlue();
			if ((red+green+blue)/3<128)
				labelColor = Color.white;
			else
				labelColor = Color.black;
		}
		if (c!=null) {
			g.setColor(c);
			if (listStroke!=null) ((Graphics2D)g).setStroke(listStroke);
		} else
			g.setColor(showAllColor);
    }

	/** 
	 * Installs a list of ROIs (a "display list") that will be drawn on this image as a non-destructive overlay.
	 * @see ij.gui.Roi#setLineColor
	 * @see ij.gui.Roi#setLineWidth
	 * @see ij.gui.Roi#setLocation
	 * @see ij.gui.Roi#setNonScalable
	 * @see ij.gui.TextRoi#setBackgroundColor
	 */
	public void setDisplayList(Vector<Roi> list) 
	{
		displayList = list;
		listColor = null;
		if (list==null) customRoi = false;
		if (list!=null&&list.size()>0&&((Roi)list.elementAt(0)).getLineColor()!=null)
			labelListItems = false;
		else
			labelListItems = true;
		repaint();
	}
	
	/** 
	 * Creates a single ShapeRoi display list from the specified 
	 * Shape, Color and BasicStroke, and activates it.
	 * @see #setDisplayList(Vector)
	 * @see ij.gui.Roi#setLineColor
	 * @see ij.gui.Roi#setLineWidth
	 */
	public void setDisplayList(Shape shape, Color color, BasicStroke stroke) 
	{
		if (shape==null)
			{setDisplayList(null); return;}
		Roi roi = new ShapeRoi(shape);
		roi.setLineColor(color);
		Vector<Roi> list = new Vector<Roi>();
		list.addElement(roi);
		displayList = list;
		labelListItems = false;
		listColor = color;
		listStroke = stroke;
		repaint();
	}
	
	/** 
	 * Creates a single ROI display list from the specified
	 * ROI and Color, and activates it.
	 * @see #setDisplayList(Vector)
	 * @see ij.gui.Roi#setLineColor
	 * @see ij.gui.Roi#setLineWidth
	 * @see ij.gui.TextRoi#setBackgroundColor
	 */
	public void setDisplayList(Roi roi, Color color) 
	{
		roi.setLineColor(color);
		Vector<Roi> list = new Vector<Roi>();
		list.addElement(roi);
		setDisplayList(list);
	}

	/**
	 * Scroll mouse
	 * 
	 * @param sx
	 * @param sy
	 */
	protected void scroll(int sx, int sy) 
	{
		int ox = xSrcStart + (int)(sx/magnification);  //convert to offscreen coordinates
		int oy = ySrcStart + (int)(sy/magnification);
		//IJ.log("scroll: "+ox+" "+oy+" "+xMouseStart+" "+yMouseStart);
		int newx = xSrcStart + (xMouseStart-ox);
		int newy = ySrcStart + (yMouseStart-oy);
		if (newx<0) newx = 0;
		if (newy<0) newy = 0;
		if ((newx+srcRect.width)>imageWidth) newx = imageWidth-srcRect.width;
		if ((newy+srcRect.height)>imageHeight) newy = imageHeight-srcRect.height;
		srcRect.x = newx;
		srcRect.y = newy;
		//IJ.log(sx+"  "+sy+"  "+newx+"  "+newy+"  "+srcRect);
		imp.draw();
		Thread.yield();
	}
	
	/** 
	 * Enables/disables the ROI Manager "Show All" mode. 
	 */
	public void setShowAllROIs(boolean showAllROIs) 
	{
		this.showAllROIs = showAllROIs;
	}

	/** 
	 * Returns the state of the ROI Manager "Show All" flag. 
	 */
	public boolean getShowAllROIs() 
	{
		return showAllROIs;
	}

	/** 
	 * Returns the color used for "Show All" mode. 
	 */
	public static Color getShowAllColor() {
			return showAllColor;
	}

	/** 
	 * Sets the color used used for "Show All" mode.
	 * 
	 * @param c color
	 */
	public static void setShowAllColor(Color c) 
	{
		if (c==null) return;
		showAllColor = c;
		labelColor = null;
		final ImagePlus img = WindowManager.getCurrentImage();
		if (img!=null) 
		{
			final ImageCanvas ic = img.getCanvas();
			if (ic!=null && ic.getShowAllROIs()) 
				img.draw();
		}
	}
	
	
	/**
	 * Zooms out by making the source rectangle (srcRect)
	 * larger and centering it on (x,y). If we can't make it larger,
	 * then make the window smaller.
	 * @param x center x- coordinate
	 * @param y center y- coordinate
	 */
	public void zoomOut(int x, int y) 
	{
		if (magnification<=0.03125)
			return;
		double oldMag = magnification;
		double newMag = getLowerZoomLevel(magnification);
		double srcRatio = (double)srcRect.width/srcRect.height;
		double imageRatio = (double)imageWidth/imageHeight;
		//double initialMag = imp.getWindow().getInitialMagnification();
		if (Math.abs(srcRatio-imageRatio)>0.05) 
		{
			double scale = oldMag/newMag;
			int newSrcWidth = (int)Math.round(srcRect.width*scale);
			int newSrcHeight = (int)Math.round(srcRect.height*scale);
			if (newSrcWidth>imageWidth) newSrcWidth=imageWidth;
			if (newSrcHeight>imageHeight) newSrcHeight=imageHeight;
			int newSrcX = srcRect.x - (newSrcWidth - srcRect.width)/2;
			int newSrcY = srcRect.y - (newSrcHeight - srcRect.height)/2;
			if (newSrcX<0) newSrcX = 0;
			if (newSrcY<0) newSrcY = 0;
			srcRect = new Rectangle(newSrcX, newSrcY, newSrcWidth, newSrcHeight);
			//IJ.log(newMag+" "+srcRect+" "+dstWidth+" "+dstHeight);
			int newDstWidth = (int)(srcRect.width*newMag);
			int newDstHeight = (int)(srcRect.height*newMag);
			setMagnification(newMag);
			setMaxBounds();
			//IJ.log(newDstWidth+" "+dstWidth+" "+newDstHeight+" "+dstHeight);
			if (newDstWidth<dstWidth || newDstHeight<dstHeight) {
				//IJ.log("pack");
				setDrawingSize(newDstWidth, newDstHeight);
				imp.getWindow().pack();
			} else
				repaint();
			return;
		}
		
		if (imageWidth*newMag>dstWidth) 
		{
			int w = (int)Math.round(dstWidth/newMag);
			if (w*newMag<dstWidth) w++;
			int h = (int)Math.round(dstHeight/newMag);
			if (h*newMag<dstHeight) h++;
			x = offScreenX(x);
			y = offScreenY(y);
			Rectangle r = new Rectangle(x-w/2, y-h/2, w, h);
			if (r.x<0) r.x = 0;
			if (r.y<0) r.y = 0;
			if (r.x+w>imageWidth) r.x = imageWidth-w;
			if (r.y+h>imageHeight) r.y = imageHeight-h;
			srcRect = r;
		} 
		else 
		{
			srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
			setDrawingSize((int)(imageWidth*newMag), (int)(imageHeight*newMag));
			//setDrawingSize(dstWidth/2, dstHeight/2);
			imp.getWindow().pack();
		}
		//IJ.write(newMag + " " + srcRect.x+" "+srcRect.y+" "+srcRect.width+" "+srcRect.height+" "+dstWidth + " " + dstHeight);
		setMagnification(newMag);
		//IJ.write(srcRect.x + " " + srcRect.width + " " + dstWidth);
		setMaxBounds();
		repaint();
	}

	/** 
	 * Implements the Image/Zoom/Original Scale command. 
	 */
	public void unzoom() 
	{
		double imag = imp.getWindow().getInitialMagnification();
		if (magnification==imag)
			return;
		srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
		ImageWindow win = imp.getWindow();
		setDrawingSize((int)(imageWidth*imag), (int)(imageHeight*imag));
		setMagnification(imag);
		setMaxBounds();
		win.pack();
		setMaxBounds();
		repaint();
	}

	/** Implements the Image/Zoom/View 100% command. */
	public void zoom100Percent() 
	{
		if (magnification==1.0)
			return;
		double imag = imp.getWindow().getInitialMagnification();
		if (magnification!=imag)
			unzoom();
		if (magnification==1.0)
			return;
		if (magnification<1.0) {
			while (magnification<1.0)
				zoomIn(imageWidth/2, imageHeight/2);
		} else if (magnification>1.0) {
			while (magnification>1.0)
				zoomOut(imageWidth/2, imageHeight/2);
		} else
			return;
		int x=xMouse, y=yMouse;
		if (mouseExited) {
			x = imageWidth/2;
			y = imageHeight/2;
		}
		int sx = screenX(x);
		int sy = screenY(y);
		adjustSourceRect(1.0, sx, sy);
		repaint();
	}
	
	public static double getLowerZoomLevel(double currentMag) 
	{
		double newMag = zoomLevels[0];
		for (int i=0; i<zoomLevels.length; i++) {
		if (zoomLevels[i] < currentMag)
			newMag = zoomLevels[i];
		else
			break;
		}
		return newMag;
	}

	public static double getHigherZoomLevel(double currentMag) 
	{
		double newMag = 32.0;
		for (int i=zoomLevels.length-1; i>=0; i--) {
			if (zoomLevels[i]>currentMag)
				newMag = zoomLevels[i];
			else
				break;
		}
		return newMag;
	}

	/** 
	 * Zooms in by making the window bigger. If it can't
	 * be made bigger, then make the source rectangle
	 * (srcRect) smaller and center it at (x,y). 
	 */
	public void zoomIn(int x, int y) 
	{
		if (magnification>=32) 
			return;
		
		final double newMag = getHigherZoomLevel(magnification);
		final int newWidth = (int)(imageWidth*newMag);
		final int newHeight = (int)(imageHeight*newMag);
		final Dimension newSize = canEnlarge(newWidth, newHeight);
		
		if (newSize!=null) 
		{
			setDrawingSize(newSize.width, newSize.height);
			if (newSize.width!=newWidth || newSize.height!=newHeight)
				adjustSourceRect(newMag, x, y);
			else
				setMagnification(newMag);
			imp.getWindow().pack();
		} else
			adjustSourceRect(newMag, x, y);
		repaint();
		
		if (srcRect.width<imageWidth || srcRect.height<imageHeight)
			resetMaxBounds();
	}
	
	/**
	 * Set maximum bounds
	 */
	void setMaxBounds() 
	{
		if (maxBoundsReset) {
			maxBoundsReset = false;
			ImageWindow win = imp.getWindow();
			if (win!=null && !IJ.isLinux() && win.maxBounds!=null) {
				win.setMaximizedBounds(win.maxBounds);
				win.setMaxBoundsTime = System.currentTimeMillis();
			}
		}
	}

	/**
	 * Reset the maximum bounds
	 */
	void resetMaxBounds() 
	{
		final ImageWindow win = imp.getWindow();
		if (win!=null && (System.currentTimeMillis()-win.setMaxBoundsTime)>500L) 
		{
			win.setMaximizedBounds(win.maxWindowBounds);
			maxBoundsReset = true;
		}
	}
	
	protected Dimension canEnlarge(int newWidth, int newHeight) 
	{
		//if ((flags&Event.CTRL_MASK)!=0 || IJ.controlKeyDown()) return null;
		final ImageWindow win = imp.getWindow();
		if (win == null) 
			return null;
		final Rectangle r1 = win.getBounds();
		final Insets insets = win.getInsets();
		final Point loc = getLocation();
		
		if (loc.x>insets.left+5 || loc.y>insets.top+5) 
		{
			r1.width = newWidth+insets.left+insets.right+10;
			r1.height = newHeight+insets.top+insets.bottom+10;
			if (win instanceof StackWindow) r1.height+=20;
		} 
		else 
		{
			r1.width = r1.width - dstWidth + newWidth+10;
			r1.height = r1.height - dstHeight + newHeight+10;
		}
		
		final Rectangle max = win.getMaxWindow(r1.x, r1.y);
		final boolean fitsHorizontally = r1.x+r1.width<max.x+max.width;
		final boolean fitsVertically = r1.y+r1.height<max.y+max.height;
		
		if (fitsHorizontally && fitsVertically)
			return new Dimension(newWidth, newHeight);
		else if (fitsVertically && newHeight<dstWidth)
			return new Dimension(dstWidth, newHeight);
		else if (fitsHorizontally && newWidth<dstHeight)
			return new Dimension(newWidth, dstHeight);
		else
			return null;
	}
	
}// end class JImagePanelPlus
