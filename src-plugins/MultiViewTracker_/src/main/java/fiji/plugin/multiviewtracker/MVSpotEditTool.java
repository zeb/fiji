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

import javax.swing.SwingUtilities;

import mpicbg.models.NoninvertibleModelException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.tool.AbstractTool;

public class MVSpotEditTool<T extends RealType<T> & NativeType<T>> extends AbstractTool implements MouseMotionListener, MouseListener, KeyListener, DetectorKeys {

	private static final boolean DEBUG = true;

	private static final double COARSE_STEP = 2;
	private static final double FINE_STEP = 0.2f;
	private static final String TOOL_NAME = "Spot edit tool";
	private static final String TOOL_ICON = "C444D01C777D11C999D21C000D31C777L4151C222D61"
			+ "CcccD71C222D81C331D91Ceb5Da1Cd95Lb1c1Cda3Dd1Ca82De1C000Df1"
			+ "CbbbD02C000D32CdddD42CcccD62C777D82C100D92Ca85Da2CfedLb2c2Cd94Dd2C641De2C111Df2"
			+ "C000D33CdddD43C761D83C664D93C544Da3CfedLb3c3CdcaDd3C863De3C111Df3"
			+ "C000D34CdddD44Cec3D74C776D84Cdc9D94C000Da4Cdb9Db4CfdaDc4C776Dd4Cc95De4C111Df4"
			+ "C000D35CdddD45Cec3D65CffcD75C875D85Cfe7D95C542La5b5Cda7Dc5C653Dd5C111Df5"
			+ "C000D36CdddD46Cec3D56CffcD66CffbD76C773D86Cfd4D96Ccb7Da6C000Db6C642Dc6CeeeDd6C111Df6"
			+ "C000D37Cb92D47CffcD57CffbD67Cfe6D77C541D87Cff9D97Ceb6Da7C321Db7C555Dc7C111Df7"
			+ "C999D28C000D38C665D48CeeaD58Cfe6D68Ca93D78C110D88C974D98Ce94Da8CaaaDb8CcccDc8CaaaDe8C000Df8"
			+ "Cc92D29CfecD39CffbD49Cfe6D59Cfd4D69Cff9D79Ceb6D89Ce94D99"
			+ "Ca62D0aCc92D1aCedbD2aCdb6D3aCfe6D4aCfd4D5aCff9D6aCeb6D7aCe94D8a"
			+ "C972D0bCfedL1b2bCb83D3bCca3D4bCec7D5bCda5D6bCc83D7b"
			+ "C972D0cCfedD1cCfb6D2cCfa4D3cCc61D4cCb61D5cCb73D6c"
			+ "C641D0dCda6D1dCfdaD2dCfdbD3dCc95D4dCb73D5d"
			+ "C641L0e1eCa72D2eCb73D3eCc94D4e";

	/** Fall back default radius when the settings does not give a default radius to use. */
	private static final double FALL_BACK_RADIUS = 5;


	@SuppressWarnings("rawtypes")
	private static MVSpotEditTool instance;
	private HashMap<ImagePlus, MultiViewDisplayer<T>> displayers = new HashMap<ImagePlus, MultiViewDisplayer<T>>();
	/** The radius of the previously edited spot. */
	private Double previousRadius = null;
	/** The spot currently moved. */
	private Spot quickEditedSpot;


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
				System.out.println("[SpotEditTool] Instantiating: "+instance);
		}
		if (DEBUG)
			System.out.println("[SpotEditTool] Returning instance: "+instance);
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
			System.out.println("[SpotEditTool] Registering "+imp+" and "+displayer);
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
			System.out.println("[SpotEditTool] @mouseClicked");
			System.out.println("[SpotEditTool] Got "+imp+ " as ImagePlus");
			System.out.println("[SpotEditTool] Matching displayer: "+displayer);

			for (MouseListener ml : imp.getCanvas().getMouseListeners()) {
				System.out.println("[SpotEditTool] mouse listener: "+ml);
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
			} else {
				model.addSpotToSelection(target);
			}
		} else {
			model.clearSpotSelection();
			model.addSpotToSelection(target);
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
		if (quickEditedSpot == null)
			return;

		final ImagePlus imp = getImagePlus(e);
		final MultiViewDisplayer<T> displayer = displayers.get(imp);
		if (null == displayer)
			return;

		final float ix = (float) (displayer.getCanvas(imp).offScreenXD(e.getX()) + 0.5f);  // relative to pixel center
		final float iy =  (float) (displayer.getCanvas(imp).offScreenYD(e.getY()) + 0.5f);
		final float iz =  imp.getSlice() - 1;
		final float[] pixelCoords = new float[] { ix, iy, iz };

		final float[] physicalCoords;
		try {
			physicalCoords = displayer.getTransform(imp).applyInverse(pixelCoords);
		} catch (NoninvertibleModelException e1) {
			System.err.println("Unable to compute back spot physical coordinates.\n" +
					"Pixel coordinates were "+pixelCoords+". Transform was "+displayer.getTransform(imp));
			e1.printStackTrace();
			return;
		}
		quickEditedSpot.setPosition(physicalCoords);

		imp.updateAndDraw();

	}

	/*
	 * KEYLISTENER
	 */

	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void keyPressed(KeyEvent e) { 

		if (DEBUG) 
			System.out.println("[SpotEditTool] keyPressed: "+e.getKeyChar());

		final ImagePlus imp = getImagePlus(e);
		if (imp == null)
			return;
		final MultiViewDisplayer<T> displayer = displayers.get(imp);
		if (null == displayer)
			return;

		TrackMateModel<T> model = displayer.getModel();

		int keycode = e.getKeyCode(); 

		switch (keycode) {

		// Delete currently edited spot
		case KeyEvent.VK_DELETE: {

			ArrayList<Spot> spotSelection = new ArrayList<Spot>(model.getSpotSelection());
			ArrayList<DefaultWeightedEdge> edgeSelection = new ArrayList<DefaultWeightedEdge>(model.getEdgeSelection());
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

			imp.updateAndDraw();
			e.consume();
			break;
		}

		// Quick add spot at mouse
		case KeyEvent.VK_A: {

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

			model.beginUpdate();
			try {
				model.addSpotTo(newSpot, frame);
			} finally {
				model.endUpdate();
			}

			imp.updateAndDraw();
			e.consume();

			break;
		}

		// Quick delete spot under mouse
		case KeyEvent.VK_D: {

			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen(mouseLocation, displayer.getCanvas(imp));
			int frame = imp.getFrame() - 1;
			Spot clickLocation = displayer.getCLickLocation(mouseLocation, imp);
			Spot target = model.getFilteredSpots().getSpotAt(clickLocation, frame);
			if (null == target) {
				e.consume(); // Consume it anyway, so that we are not bothered by IJ
				return; 
			}

			model.beginUpdate();
			try {
				model.removeSpotFrom(target, frame);
			} finally {
				model.endUpdate();
			}

			imp.updateAndDraw();

			e.consume();
			break;
		}

		// Quick move spot under the mouse
		case KeyEvent.VK_SPACE: {

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
			e.consume();
			break;

		}

		// Quick change spot radius
		case KeyEvent.VK_Q:
		case KeyEvent.VK_E: {

			Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen(mouseLocation, displayer.getCanvas(imp));
			int frame = imp.getFrame() - 1;
			Spot clickLocation = displayer.getCLickLocation(mouseLocation, imp);
			Spot target = model.getFilteredSpots().getSpotAt(clickLocation, frame);
			if (null == target) {
				return; // un-consumed event
			}

			int factor;
			if (e.getKeyCode() == KeyEvent.VK_Q) {
				factor = -1;
			} else {
				factor = 1;
			}

			double radius = target.getFeature(Spot.RADIUS);
			// We need to get some idea of the current pixel scale, so that our increment is not
			// out of range for all possible calibration.
			final float[] transformMatrix = displayer.getTransform(imp).getMatrix(null);
			float roughScale = Util.computeAverage(new float[] { 
					transformMatrix[0],  transformMatrix[1], transformMatrix[2], 
					transformMatrix[4],  transformMatrix[5], transformMatrix[6], 
					transformMatrix[8],  transformMatrix[9], transformMatrix[10] } );
			if (e.isShiftDown()) 
				radius += factor / roughScale * COARSE_STEP;
			else 
				radius += factor / roughScale * FINE_STEP;

			if (radius <= 0)
				return;

			target.putFeature(Spot.RADIUS, radius);
			model.beginUpdate();
			try {
				model.updateFeatures(target);
			} finally {
				model.endUpdate();
			}

			imp.updateAndDraw();
			e.consume();
			break;
		}

		// Copy spots from previous frame
		case KeyEvent.VK_V: {
			if (e.isShiftDown()) {

				int currentFrame = imp.getFrame() - 1;
				if (currentFrame > 0) {

					List<Spot> previousFrameSpots = model.getFilteredSpots().get(currentFrame-1);
					if (previousFrameSpots.isEmpty()) {
						e.consume();
						break;
					}
					ArrayList<Spot> copiedSpots = new ArrayList<Spot>(previousFrameSpots.size());
					HashSet<String> featuresKey = new HashSet<String>(previousFrameSpots.get(0).getFeatures().keySet());
					featuresKey.remove(Spot.POSITION_T); // Deal with time separately
					featuresKey.remove(Spot.FRAME); // Deal with time separately
					double dt = model.getSettings().dt;
					if (dt == 0)
						dt = 1;

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


				e.consume();
			}
			break;
		}

		case KeyEvent.VK_W: {
			e.consume(); // consume it: we do not want IJ to close the window
			break;
		}

		}

	}

	@Override
	public void keyReleased(KeyEvent e) { 
		if (DEBUG) 
			System.out.println("[SpotEditTool] keyReleased: "+e.getKeyChar());

		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE: {
			if (null == quickEditedSpot)
				return;
			final ImagePlus imp = getImagePlus(e);
			if (imp == null)
				return;
			final MultiViewDisplayer<T> displayer = displayers.get(imp);
			if (null == displayer)
				return;
			TrackMateModel<T> model = displayer.getModel();
			model.beginUpdate();
			try {
				model.updateFeatures(quickEditedSpot);
			} finally {
				model.endUpdate();
			}
			quickEditedSpot = null;
			break;
		}
		}

	}	


	/*
	 * PRIVATE METHODS
	 */

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

}
