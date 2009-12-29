package fiji.plugin.constrainedshapes;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.PlugIn;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import fiji.plugin.constrainedshapes.CircleRoi.ClickLocation;
import fiji.util.AbstractTool;

public class SnappingCircleTool extends AbstractTool implements PlugIn {

	/*
	 * FIELDS
	 */
	
	ImagePlus imp;
	ImageCanvas canvas;
	CircleRoi roi;
	InteractionStatus status;
	private Point2D start_drag;

	/*
	 * ENUM
	 */
	
	public static enum InteractionStatus {
		FREE,
		MOVING,
		RESIZING,
		CREATING;
	}
	
	/*
	 * RUN METHOD
	 */
	
	public void run(String arg) {
		imp = WindowManager.getCurrentImage();
		if (imp != null) { 
			Roi current_roi = imp.getRoi(); 
			if ( (current_roi != null) && (current_roi instanceof CircleRoi) ) {
				roi = (CircleRoi) current_roi;
				status = InteractionStatus.FREE;
			} else {
				roi = new CircleRoi();
				status = InteractionStatus.CREATING;
			}
		}
		canvas = imp.getCanvas();
		super.run(arg);
	}
	
	
	
	/*
	 * MOUSE METHODS
	 */
	
	@Override
	protected void handleMousePress(MouseEvent e) {
		// Deal with changing window
		ImageCanvas source = (ImageCanvas) e.getSource();
		if (source != canvas) {
			// We changed image window. Update fields accordingly
			ImageWindow window = (ImageWindow) source.getParent();
			imp = window.getImagePlus();
			canvas = source;
			Roi current_roi = imp.getRoi();
			if ( (current_roi == null) || !(current_roi instanceof CircleRoi)) {
				roi = new CircleRoi();
				status = InteractionStatus.CREATING;
				imp.setRoi(roi);
			} else {
				roi = (CircleRoi) current_roi;
				status = InteractionStatus.FREE;
			}
		} 
		
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);
		ClickLocation cl = roi.getClickLocation(p);
		
		switch (cl) {
		case OUTSIDE:
			switch (status) {
			case CREATING:
				roi.shape.setCenter(p);
				break;
			default:
				// If drag is small, then we will kill this roi
			}
			break;
		default:
			status = cl.getInteractionStatus();
		}
		IJ.showStatus(cl.name() + " - " + status.name());
		start_drag = p;
	}
	
	@Override
	public void handleMouseDrag(MouseEvent e) {
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final Point2D p = new Point2D.Double(x, y);
		final double[] params = roi.shape.getParameters();
		
		switch (status) {
		case MOVING:
			params[0] += x-start_drag.getX();
			params[1] += y-start_drag.getY();
			break;
		case RESIZING:
		case CREATING:
			params[2] = roi.shape.getCenter().distance(p);			
			break;
		}
		start_drag = p;
		imp.setRoi(roi); 
		IJ.showStatus(roi.shape.toString()+" - "+status.name()); 
	}
	
	@Override
	public void handleMouseClick(MouseEvent e) {
		if (roi == null) return;
		ClickLocation cl = roi.getClickLocation(e.getPoint());
		if (cl == ClickLocation.OUTSIDE ) {
			imp.killRoi();
			roi = new CircleRoi();
			status = InteractionStatus.CREATING;
		}
	}
	
	
	/*
	 * ABSTRACTTTOL METHODS
	 */
	
	@Override
	public String getToolIcon() {
		return "C000DccDd5DdaDdbDe6De7De8De9DeaC000Db3Dd4C000Dc4C000De5" +
				"C000DbcC000C111D8dD9dC111Df8C111Dd6C111Df7C111DadDcb" +
				"C111DebC111C222D5cDdcC222D4bC222D7dDc3C222Df9C222Da3C222" +
				"C333Dd9C333DbdC333C444D39C444D45C444D38C444C555D37C555D6c" +
				"C555Df6C555C666Dd7C666D4aC666D63C666D54C666C777Dd8C777Dfa" +
				"C777C888D6dD73C888De4C888D36DcdC888D3aC888C999D93C999Dac" +
				"C999D44CaaaDd3CaaaD4cD53CbbbD5bDc5CbbbDf5CbbbD83DecCbbbD35Db4" +
				"CbbbDa2CbbbCcccD3bD5dD92CcccDfbCcccDb2CcccDddCcccD82CcccD72" +
				"CcccD46CcccCdddD62Dc2CdddD43De3CdddD7cDf4CdddD34CdddD7eD8eD9e" +
				"CdddDaeCdddD27D28D29CdddD3cD4dCdddD26CdddD6eDbeDcaDedDfc" +
				"CdddD2aD52Dd2CeeeD49CeeeDceCeeeD9cCeeeD25CeeeDbbCeeeD5e" +
				"CeeeD2bD33Df3CeeeD8cCeeeDdeCeeeCfffD64CfffD42D47De2CfffD3d" +
				"CfffDfdCfffD24D48D55CfffD2cD4eD61D71D81D91Da1Db1Dc1Dc6Dee" +
				"CfffD32D51Dd1Df2CfffD23D5aD6bD6fD7fD8fD9fDa4DafDc9CfffD15D16" +
				"D17D18D19D1aD1bD2dD3eD41D5fD74DabDb5DbfDcfDe1DfeCfffD14D56D94" +
				"DbaDc7Dc8DdfCfffD1cD22D4fD59D65D7bD84DefCfffD2eD31D6aD9bDb6Df1" +
				"CfffD13D57D8bDa5Db9CfffD1dD3fD50D58D60D70D75D80D90Da0DaaDb0Dc0" +
				"Dd0DffCfffD21D40D66De0";
	}

	@Override
	public String getToolName() {		
		return "Circle_Shape";
	}

	@Override
	public boolean hasOptionDialog() {
		return false;
	}

	@Override
	public void showOptionDialog() {}

}
