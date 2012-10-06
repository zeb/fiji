package fiji.plugin.multiviewtracker;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Toolbar;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;

public class MVSpotEditTool<T extends RealType<T> & NativeType<T>> extends AbstractTool implements ToolWithOptions, MouseMotionListener, MouseListener, KeyListener, DetectorKeys {

	private static final boolean DEBUG = false;

	private static final double COARSE_STEP = 2;
	private static final double FINE_STEP = 0.2f;
	private static final String TOOL_NAME = "Spot edit tool";
	private static final String TOOL_ICON = "CfffL0050CdddD60CdccD70CeeeD80CfffL90d0"
			+ "L0131CeddD41Ca87D51C764D61C754L7181Ca99D91CfffLa1d1"
			+ "L0222CccbD32C875D42Ca75L5262C965D72C854D82C643D92C987Da2CfffLb2d2"
			+ "L0313CfeeD23Ca86D33Cb86D43Cc87D53Cb86D63Cb76D73Ca75D83C964D93C643Da3CbaaDb3CfffLc3d3"
			+ "L0414CdccD24Cb86D34Cd98D44Cc97D54Cc87D64Cb86D74Cb76D84Ca75D94C864Da4C766Db4CfffLc4d4"
			+ "L0515CddcD25Cb97D35Cc87D45Cb86D55Cc87D65Cb86D75Ca75L8595C965Da5C876Db5CfffLc5d5"
			+ "L0616CedcD26Cb97D36Ca86D46C854D56C965D66C854D76C864D86C965D96C964Da6C987Db6CfffLc6d6"
			+ "L0717CdcbD27Cc97D37Cb97D47Ca76L5767C964D77C965D87Ca75D97C965Da7Ca87Db7CfffLc7d7"
			+ "L0818CedcD28Cc97L3848Cc87D58Cb76D68Ca75L7888Cb76D98Ca75Da8Cc98Db8CfffLc8d8"
			+ "L0929Cda8D39Cc97D49Cc87D59Cb86D69Ca75D79Cb76D89Cb75D99Cb76Da9CeccDb9CfffLc9d9"
			+ "L0a2aCedcD3aCc97D4aCb86D5aCb76D6aCa75L7a9aCdcbDaaCfffLbada"
			+ "CeefD0bCccdD1bCbbcD2bCbbbD3bCc97L4b5bCc86D6bCb86D7bCa86D8bCa75D9bCbaaDabCeeeDbbCfffLcbdb"
			+ "C99bL0c1cCaacD2cCaabD3cCb98D4cCb87D5cCa76L6c9cC989DacC89aDbcC99aDccCccdDdc"
			+ "C88aD0dC9abD1dCaacL2d3dCb98D4dCb87D5dCa76L6d8dCa77D9dC99aDadC88aDbdC779DcdC88aDdd"
			+ "D0eCaacD1eCbbdL2e3eCa9aD4eCb97D5eCa87L6e8eC988D9eC99bDaeC88aLbede"
			+ "D0fCbbdL1f2fCabdD3fCaabD4fCa99D5fCb98L6f7fCa87D8fC99aD9fC99bDafC89aLbfdf";

	/** Fall back default radius when the settings does not give a default radius to use. */
	private static final double FALL_BACK_RADIUS = 5;
	/**
	 *  The default stepping interval when moving in time using keystrokes.
	 * @see #steppingIncrement
	 */
	private static final int DEFAULT_STEPPING_INCREMENT = 3;


	@SuppressWarnings("rawtypes")
	private static MVSpotEditTool instance;
	private HashMap<ImagePlus, MultiViewDisplayer<T>> displayers = new HashMap<ImagePlus, MultiViewDisplayer<T>>();
	/** The radius of the previously edited spot. */
	private Double previousRadius = null;
	/** The spot currently moved. */
	private Spot quickEditedSpot;
	/** 
	 * If true, the next added spot will be automatically linked to the previously created one, given that 
	 * the new spot is created in a subsequent frame. 
	 */
	private boolean isLinkingMode = false;
	/**
	 * If false, then no modifications will happens through this tool.
	 */
	private boolean isEdtingEnabled = true;
	/**
	 * The increment by which to move in time when using keystroke in this tool.
	 */
	protected int steppingIncrement = DEFAULT_STEPPING_INCREMENT;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Singleton
	 */
	private MVSpotEditTool() {	}

	/**
	 * Return the singleton instance for this tool. If it was not previously instantiated, this calls
	 * instantiates it. 
	 */
	@SuppressWarnings("unchecked")
	public static <T extends RealType<T> & NativeType<T>> MVSpotEditTool<T> getInstance() {
		if (null == instance) {
			instance = new MVSpotEditTool<T>();
			if (DEBUG)
				System.out.println("[MVSpotEditTool] Instantiating: "+instance);
		}
		if (DEBUG)
			System.out.println("[MVSpotEditTool] Returning instance: "+instance);
		return instance;
	}

	/**
	 * Return true if the tool is currently present in ImageJ toolbar.
	 */
	public static boolean isLaunched() {
		Toolbar toolbar = Toolbar.getInstance();
		if (null != toolbar && toolbar.getToolId(TOOL_NAME) >= 0) 
			return true;
		return false;
	}

	/*
	 * METHODS
	 */

	@Override
	public String getToolName() {
		return TOOL_NAME;
	}	

	@Override
	public String getToolIcon() {
		return TOOL_ICON;
	}

	/**
	 * Register the given {@link HyperStackDisplayer}. If this method id not called, the tool will not
	 * respond.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
	 */
	public void register(final ImagePlus imp, final MultiViewDisplayer<T> displayer) {
		if (DEBUG)
			System.out.println("[MVSpotEditTool] Registering "+imp+" and "+displayer);
		displayers.put(imp, displayer);
	}

	
	
	
	/*
	 * MOUSE AND MOUSE MOTION
	 */

	@Override
	public void mouseClicked(MouseEvent e) {

		final ImagePlus imp = getImagePlus(e);
		final MultiViewDisplayer<T> displayer = displayers.get(imp);
		if (DEBUG) {
			System.out.println("[MVSpotEditTool] @mouseClicked");
			System.out.println("[MVSpotEditTool] Got "+imp+ " as ImagePlus");
			System.out.println("[MVSpotEditTool] Matching displayer: "+displayer);

			for (MouseListener ml : imp.getCanvas().getMouseListeners()) {
				System.out.println("[MVSpotEditTool] mouse listener: "+ml);
			}

		}

		if (null == displayer)
			return;

		final Spot clickLocation = displayer.getCLickLocation(e.getPoint(), imp);
		final int frame = imp.getFrame() - 1;
		final TrackMateModel<T> model = displayer.getModel();
		Spot target = model.getFilteredSpots().getSpotAt(clickLocation, frame);

		// Change selection
		// only if we are not currently editing and if target is non null
		if (target == null)
			return;
		updateStatusBar(target, imp.getCalibration().getUnits());
		final int addToSelectionMask = MouseEvent.SHIFT_DOWN_MASK;
		if ((e.getModifiersEx() & addToSelectionMask) == addToSelectionMask) { 
			if (model.getSpotSelection().contains(target)) {
				model.removeSpotFromSelection(target);
				IJ.showStatus("Removed spot " + target.getName() + "from selection.");
			} else {
				model.addSpotToSelection(target);
				IJ.showStatus("Added spot " + target.getName() + " to selection.");
			}
		} else {
			model.clearSpotSelection();
			model.addSpotToSelection(target);
			IJ.showStatus("Selected spot " + target.getName() + ".");
		}
	}



	@Override
	public void mousePressed(MouseEvent e) {}


	@Override
	public void mouseReleased(MouseEvent e) {}


	@Override
	public void mouseEntered(MouseEvent e) {}


	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) { }

	@Override
	public void mouseMoved(MouseEvent e) {
		
		if (!isEdtingEnabled || quickEditedSpot == null)
			return;

		final ImagePlus imp = getImagePlus(e);
		final MultiViewDisplayer<T> displayer = displayers.get(imp);
		if (null == displayer)
			return;

		final double ix = displayer.getCanvas(imp).offScreenXD(e.getX()) + 0.5;  // relative to pixel center
		final double iy = displayer.getCanvas(imp).offScreenYD(e.getY()) + 0.5;
		final double iz =  imp.getSlice() - 1;
		final double[] pixelCoords = new double[] { ix, iy, iz };

		final double[] physicalCoords = new double[3];
		displayer.getTransform(imp).applyInverse(physicalCoords, pixelCoords);
		quickEditedSpot.setPosition(physicalCoords);

		imp.updateAndDraw();

	}
	
	/*
	 * OPTION DIALOG
	 */
	

	@Override
	public void showOptionDialog() {
		new MVSpotEditToolOptionDialog<T>(this).setVisible(true);
	}

	/*
	 * KEYLISTENER
	 */

	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void keyPressed(KeyEvent e) { 

		if (DEBUG) 
			System.out.println("[MVSpotEditTool] keyPressed: "+KeyEvent.getKeyText(e.getKeyCode()));

		final ImagePlus imp = getImagePlus(e);
		if (imp == null)
			return;
		final MultiViewDisplayer<T> displayer = displayers.get(imp);
		if (null == displayer)
			return;

		TrackMateModel<T> model = displayer.getModel();

		int keycode = e.getKeyCode(); 

		switch (keycode) {

		// Delete spot & edge selection
		case KeyEvent.VK_DELETE: {

			deleteSpotSelection(model);
			imp.updateAndDraw();
			e.consume();
			break;
		}

		// Quick add spot at mouse
		case KeyEvent.VK_A: {

			addNewSpot(model, displayer, imp);
			imp.updateAndDraw();
			e.consume();
			break;
		}

		// Quick delete spot under mouse
		case KeyEvent.VK_D: {

			deleteLocalSpot(model, displayer, imp);
			imp.updateAndDraw();
			e.consume();
			break;
		}

		// Quick move spot under the mouse
		case KeyEvent.VK_SPACE: {

			storeLocalSpot(model, displayer, imp, e);
			break;
		}

		// Quick change spot radius
		case KeyEvent.VK_Q:
		case KeyEvent.VK_E: {

			changeRadiusLocalSpot(model, displayer, imp, e);
			break;
		}

		// Copy spots from previous frame
		case KeyEvent.VK_V: {

			if (e.isShiftDown()) {
				copySpotsFromPreviousFrame(model, displayer, imp);
				e.consume();
			}
			break;
		}

		case KeyEvent.VK_W: {
			e.consume(); // consume it: we do not want IJ to close the window
			break;
		}
		
		// Toggle edit mode on / off
		case KeyEvent.VK_I: {
			switchEditingMode();
			e.consume(); 
			break;
		}

		// Switch auto-linking mode on & off 
		case KeyEvent.VK_L: {
			switchLinkingMode();
			e.consume();
			break;
		}
		
		// Move in time stepwise
		case KeyEvent.VK_O:
		case KeyEvent.VK_P: {
			boolean forward = true;
			if (keycode == KeyEvent.VK_O) {
				forward = false;
			}
			stepInTime(imp, forward);
			e.consume(); 
			break;
		}
				



		}

	}
	
	@Override
	public void keyReleased(KeyEvent e) { 
		if (DEBUG) 
			System.out.println("[MVSpotEditTool] keyReleased: "+e.getKeyChar());

		final ImagePlus imp = getImagePlus(e);
		if (imp == null)
			return;
		final MultiViewDisplayer<T> displayer = displayers.get(imp);
		if (null == displayer)
			return;

		TrackMateModel<T> model = displayer.getModel();

		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE: {
			finishMovingLocalSpot(model);
			break;
		}
		}

	}	

	/*
	 * PRIVATE METHODS
	 */
	
	private void stepInTime(final ImagePlus imp, final boolean forward) {
		int currentFrame = imp.getT();
		// Round down to nearest multiple, plus 1 because in ImagePlus, everything is 1-based
		int nearestMultiple = ( currentFrame / steppingIncrement ) * steppingIncrement + 1;
		// Compute target frame
		int targetFrame = nearestMultiple;
		if (forward) {
			targetFrame += steppingIncrement;
		} else {
			if (nearestMultiple == currentFrame) {
				targetFrame -= steppingIncrement;
			}
		}
		// Set target T for all views
		if (targetFrame >0 && targetFrame <= imp.getNFrames()) {
			displayers.get(imp).setViewsT(targetFrame);
		}
	}

	private void switchEditingMode() {
		this.isEdtingEnabled = !isEdtingEnabled;
		IJ.showStatus("Switched editing mode " +  (isEdtingEnabled ? "on." : "off.") );
	}

	private void switchLinkingMode() {
		this.isLinkingMode = !isLinkingMode;
		IJ.showStatus("Switched auto-linking mode " +  (isLinkingMode ? "on." : "off.") );
	}

	private void updateStatusBar(final Spot spot, final String units) {
		if (null == spot)
			return;
		String statusString = "";
		if (null == spot.getName() || spot.getName().equals("")) { 
			statusString = String.format("Spot ID%d, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", 
					spot.ID(), spot.getFeature(Spot.POSITION_X), spot.getFeature(Spot.POSITION_Y), 
					spot.getFeature(Spot.POSITION_Z), spot.getFeature(Spot.RADIUS), units );
		} else {
			statusString = String.format("Spot %s, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", 
					spot.getName(), spot.getFeature(Spot.POSITION_X), spot.getFeature(Spot.POSITION_Y), 
					spot.getFeature(Spot.POSITION_Z), spot.getFeature(Spot.RADIUS), units );
		}
		IJ.showStatus(statusString);
	}

	private void deleteSpotSelection(final TrackMateModel<T> model) {
		
		if (!isEdtingEnabled) {
			return;
		}
		
		ArrayList<Spot> spotSelection = new ArrayList<Spot>(model.getSpotSelection());
		ArrayList<DefaultWeightedEdge> edgeSelection = new ArrayList<DefaultWeightedEdge>(model.getEdgeSelection());
		IJ.showStatus("Deleted selection (" + spotSelection.size() + " spots & " + edgeSelection.size() + " links).");
		model.beginUpdate();
		try {
			model.clearSelection();
			for(DefaultWeightedEdge edge : edgeSelection) {
				model.removeEdge(edge);
			}
			for(Spot spot : spotSelection) {
				model.removeSpotFrom(spot, null);
			}
		} finally {
			model.endUpdate();
		}
	}

	private void addNewSpot(final TrackMateModel<T> model, final MultiViewDisplayer<T> displayer, final ImagePlus imp) {
		
		if (!isEdtingEnabled) {
			return;
		}
		
		// Create and drop a new spot
		double radius;
		if (null != previousRadius) {
			radius = previousRadius; 
		} else { 
			Map<String, Object> ss = displayer.getSttings().detectorSettings;
			if (null == ss) {
				radius = FALL_BACK_RADIUS;
			} else {
				Object obj = ss.get(DetectorKeys.DEFAULT_RADIUS);
				if (null == obj) {
					radius = FALL_BACK_RADIUS;
				} else {
					if (Double.class.isInstance(obj)) {
						radius = (Double) obj;
					} else {
						radius = FALL_BACK_RADIUS;
					}
				}
			}
		}

		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(mouseLocation, displayer.getCanvas(imp));
		Spot newSpot = displayer.getCLickLocation(mouseLocation, imp);

		int frame = imp.getFrame() - 1;
		newSpot.putFeature(Spot.POSITION_T, frame * displayer.getSttings().dt);
		newSpot.putFeature(Spot.FRAME, frame);
		newSpot.putFeature(Spot.RADIUS, radius);
		// Update image
		SpotImageUpdater<T> spotImageUpdater = new SpotImageUpdater<T>(model);
		spotImageUpdater.update(newSpot);
		
		// First the spot
		model.beginUpdate();
		try {
			model.addSpotTo(newSpot, frame);
		} finally {
			model.endUpdate();
		}
		

		// Update model
		String message;
		
		
		
		// Then, possibly, the edge. We must do it in a subsequent update, otherwise the model gets confused.
		final Set<Spot> spotSelection = model.getSpotSelection();
		if (isLinkingMode && spotSelection.size() == 1) { // if we are in the right mode & if there is only one spot in selection
			Spot targetSpot = spotSelection.iterator().next();
			if (targetSpot.getFeature(Spot.FRAME).intValue() != newSpot.getFeature(Spot.FRAME).intValue()) { // & if they are on different frames
				model.beginUpdate();
				try {
					model.addEdge(targetSpot, newSpot, -1);
				} finally {
					model.endUpdate();
				}
				message = "Added new spot "+newSpot+" to frame " + frame + ", linked to spot " + targetSpot + ".";
			} else {
				message = "Added new spot "+newSpot+" to frame " + frame + ".";
			}
		} else {
			message = "Added new spot "+newSpot+" to frame " + frame + ".";
		}

		// Store new spot as the sole selection for this model
		model.clearSpotSelection();
		model.addSpotToSelection(newSpot);

		IJ.showStatus(message);
	}

	private void deleteLocalSpot(final TrackMateModel<T> model, final MultiViewDisplayer<T> displayer, final ImagePlus imp) {
		
		if (!isEdtingEnabled) {
			return;
		}
		
		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(mouseLocation, displayer.getCanvas(imp));
		int frame = imp.getFrame() - 1;
		Spot clickLocation = displayer.getCLickLocation(mouseLocation, imp);
		Spot target = model.getFilteredSpots().getSpotAt(clickLocation, frame);
		if (null == target) {
			return; 
		}

		model.beginUpdate();
		try {
			model.removeSpotFrom(target, frame);
		} finally {
			model.endUpdate();
		}
		IJ.showStatus("Removed spot " + target + " from frame " + frame + ".");
	}

	private void storeLocalSpot(final TrackMateModel<T> model, final MultiViewDisplayer<T> displayer, final ImagePlus imp, final KeyEvent event) {
		
		if (!isEdtingEnabled) {
			return;
		}
		
		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(mouseLocation, displayer.getCanvas(imp));
		if (null == quickEditedSpot) {
			int frame = imp.getFrame() - 1;
			Spot clickLocation = displayer.getCLickLocation(mouseLocation, imp);
			quickEditedSpot = model.getFilteredSpots().getSpotAt(clickLocation, frame);
			if (null == quickEditedSpot) {
				return; // un-consumed event
			}
		}
		event.consume();
	}

	private void finishMovingLocalSpot(final TrackMateModel<T> model) {

		if (!isEdtingEnabled) {
			return;
		}
		
		SpotImageUpdater<T> spotImageUpdater = new SpotImageUpdater<T>(model);
		spotImageUpdater.update(quickEditedSpot);
		
		if (null == quickEditedSpot)
			return;
		model.beginUpdate();
		try {
			model.updateFeatures(quickEditedSpot);
		} finally {
			model.endUpdate();
		}

		String spaceUnit = model.getSettings().spaceUnits;
		IJ.showStatus(String.format("Moved spot %s to x=%.1f %s, y=%.1f %s, z=%.1f %s, frame = %d.", 
				quickEditedSpot.getName(), 
				quickEditedSpot.getFeature(Spot.POSITION_X), spaceUnit,
				quickEditedSpot.getFeature(Spot.POSITION_Y), spaceUnit,
				quickEditedSpot.getFeature(Spot.POSITION_Z), spaceUnit,
				quickEditedSpot.getFeature(Spot.FRAME).intValue()				));
		quickEditedSpot = null;
	}

	private void changeRadiusLocalSpot(final TrackMateModel<T> model, final MultiViewDisplayer<T> displayer, final ImagePlus imp, final KeyEvent event) {
		
		if (!isEdtingEnabled) {
			return;
		}
		
		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(mouseLocation, displayer.getCanvas(imp));
		int frame = imp.getFrame() - 1;
		Spot clickLocation = displayer.getCLickLocation(mouseLocation, imp);
		Spot target = model.getFilteredSpots().getSpotAt(clickLocation, frame);
		if (null == target) {
			return; // un-consumed event
		}

		int factor;
		if (event.getKeyCode() == KeyEvent.VK_Q) {
			factor = -1;
		} else {
			factor = 1;
		}

		double radius = target.getFeature(Spot.RADIUS);
		// We need to get some idea of the current pixel scale, so that our increment is not
		// out of range for all possible calibration.
		final double[] transformMatrix = displayer.getTransform(imp).getRowPackedCopy();
		final double roughScale = Util.computeAverage(new double[] { 
				transformMatrix[0],  transformMatrix[1], transformMatrix[2], 
				transformMatrix[4],  transformMatrix[5], transformMatrix[6], 
				transformMatrix[8],  transformMatrix[9], transformMatrix[10] } );
		if (event.isShiftDown()) 
			radius += factor / roughScale * COARSE_STEP;
		else 
			radius += factor / roughScale * FINE_STEP;

		if (radius <= 0)
			return;

		previousRadius = radius;
		target.putFeature(Spot.RADIUS, radius);
		// Update image
		SpotImageUpdater<T> spotImageUpdater = new SpotImageUpdater<T>(model);
		spotImageUpdater.update(target);
		
		model.beginUpdate();
		try {
			model.updateFeatures(target);
		} finally {
			model.endUpdate();
		}

		String spaceUnits = model.getSettings().spaceUnits;
		IJ.showStatus(String.format("Changed spot %s radius to %.1f %s.", 
				target.getName(), radius, spaceUnits) );
		imp.updateAndDraw();
		event.consume();
	}

	private void copySpotsFromPreviousFrame(final TrackMateModel<T> model, final MultiViewDisplayer<T> displayer, final ImagePlus imp) {
		
		if (!isEdtingEnabled) {
			return;
		}
		
		int currentFrame = imp.getFrame() - 1;
		if (currentFrame > 0) {

			List<Spot> previousFrameSpots = model.getFilteredSpots().get(currentFrame-1);
			if (previousFrameSpots.isEmpty()) {
				return;
			}
			ArrayList<Spot> copiedSpots = new ArrayList<Spot>(previousFrameSpots.size());
			HashSet<String> featuresKey = new HashSet<String>(previousFrameSpots.get(0).getFeatures().keySet());
			featuresKey.remove(Spot.POSITION_T); // Deal with time separately
			featuresKey.remove(Spot.FRAME); // Deal with time separately
			double dt = model.getSettings().dt;
			if (dt == 0)
				dt = 1;

			IJ.showStatus("Copied " + previousFrameSpots.size() + " spots from frame " + (currentFrame-1) + " to frame " + currentFrame + ".");

			SpotImageUpdater<T> spotImageUpdater = new SpotImageUpdater<T>(model);
			for(Spot spot : previousFrameSpots) {
				Spot newSpot = new SpotImp(spot, spot.getName());
				// Deal with features
				Double val;
				for(String key : featuresKey) {
					val = spot.getFeature(key);
					if (val == null) {
						continue;
					}
					newSpot.putFeature(key, val);
				}
				newSpot.putFeature(Spot.POSITION_T, spot.getFeature(Spot.POSITION_T) + dt);
				newSpot.putFeature(Spot.FRAME, spot.getFeature(Spot.FRAME) + 1);
				// Update image
				spotImageUpdater.update(newSpot);
				copiedSpots.add(newSpot);
			}
			

			model.beginUpdate();
			try {
				// Remove old ones
				for(Spot spot : new ArrayList<Spot>(model.getFilteredSpots().get(currentFrame))) {
					model.removeSpotFrom(spot, currentFrame);
				}
				// Add new ones
				for(Spot spot : copiedSpots) {
					model.addSpotTo(spot, currentFrame);
				}
			} finally {
				model.endUpdate();
				imp.updateAndDraw();
			}
		}
	}

}
