package fiji.plugin.multiviewtracker;

import ij.ImagePlus;
import ij.gui.StackWindow;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas;

public class MultiViewDisplayer <T extends RealType<T> & NativeType<T>> extends AbstractTrackMateModelView<T>  {

	private static final boolean DEBUG = true;
	public static final String NAME = "MultiView Displayer";
	public static final String INFO_TEXT = "<html>" +
			"<ul>" +
			"	<li><b>A</b> creates a new spot under the mouse" +
			"	<li><b>D</b> deletes the spot under the mouse" +
			"	<li><b>Q</b> and <b>E</b> decreases and increases the radius of the spot " +
			"under the mouse (shift to go faster)" +
			"	<li><b>Space</b> + mouse drag moves the spot under the mouse" +
			"</ul>" +
			"</html>";

	private List<ImagePlus> imps;
	Map<ImagePlus, StackWindow> windows = new HashMap<ImagePlus, StackWindow>();
	Map<ImagePlus, OverlayedImageCanvas> canvases = new HashMap<ImagePlus, OverlayedImageCanvas>();
	Map<ImagePlus, SpotOverlay<T>> spotOverlays = new HashMap<ImagePlus, SpotOverlay<T>>();
	Map<ImagePlus, TrackOverlay<T>> trackOverlays = new HashMap<ImagePlus, TrackOverlay<T>>();
	Map<ImagePlus, double[]> calibrations = new HashMap<ImagePlus, double[]>();

	private MVSpotEditTool<T> editTool;

	/*
	 * CONSTRUCTORS
	 */

	public MultiViewDisplayer(List<ImagePlus> imps, TrackMateModel<T> model) {
		this.imps = imps;
		this.model = model;
		render();
	}

	/*
	 * DEFAULT METHODS
	 */

	final Spot getCLickLocation(final Point point, ImagePlus imp) {
		OverlayedImageCanvas canvas = canvases.get(imp);
		double[] calibration = calibrations.get(imp);
		if (null == canvas) {
			System.err.println("ImagePlus "+imp+" is unknown to this displaer "+this);
			return null;
		}
		final double ix = canvas.offScreenXD(point.x) + 0.5f;
		final double iy =  canvas.offScreenYD(point.y) + 0.5f;
		final double x = ix * calibration[0];
		final double y = iy * calibration[1];
		final double z = (imp.getSlice()-1) * calibration[2];
		return new SpotImp(new double[] {x, y, z});
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected static <T extends RealType<T> & NativeType<T>> SpotOverlay<T> createSpotOverlay(final ImagePlus imp, final TrackMateModel<T> model, final Map<String, Object> displaySettings) {
		return new SpotOverlay<T>(model, imp, displaySettings);
	}

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected static <T extends RealType<T> & NativeType<T>> TrackOverlay<T> createTrackOverlay(final ImagePlus imp, final TrackMateModel<T> model, final Map<String, Object> displaySettings) {
		return new TrackOverlay<T>(model, imp, displaySettings);
	}

	@Override
	public void setModel(TrackMateModel<T> model) {
		// TODO
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void modelChanged(TrackMateModelChangeEvent event) {
		if (DEBUG)
			System.out.println("[MultiViewDisplayer] Received model changed event ID: "+event.getEventID()+" from "+event.getSource());
		boolean redoOverlay = false;

		switch (event.getEventID()) {

		case TrackMateModelChangeEvent.MODEL_MODIFIED:
			// Rebuild track overlay only if edges were added or removed, or if at least one spot was removed. 
			final List<DefaultWeightedEdge> edges = event.getEdges();
			if (edges != null && edges.size() > 0) {
				for (TrackOverlay<T> trackOverlay : trackOverlays.values()) {
					trackOverlay.computeTrackColors();
				}
				redoOverlay = true;				
			} else {
				final List<Spot> spots = event.getSpots();
				if ( spots != null && spots.size() > 0) {
					redoOverlay = true;
					for (Spot spot : event.getSpots()) {
						if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_REMOVED) {
							for (TrackOverlay<T> trackOverlay : trackOverlays.values()) {
								trackOverlay.computeTrackColors();
							}
							break;
						}
					}
				}
			}
			break;

		case TrackMateModelChangeEvent.SPOTS_COMPUTED:
			for (SpotOverlay<T> spotOverlay : spotOverlays.values()) {
				spotOverlay.computeSpotColors();
			}
			redoOverlay = true;
			break;

		case TrackMateModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
		case TrackMateModelChangeEvent.TRACKS_COMPUTED:
			for (TrackOverlay<T> trackOverlay : trackOverlays.values()) {
				trackOverlay.computeTrackColors();
			}
			redoOverlay = true;
			break;
		}
		
		if (DEBUG)
			System.out.println("[MultiViewDisplayer] Do I need to refresh all views? "+redoOverlay);
		
		if (redoOverlay)
			refresh();
	}

	@Override
	public void highlightEdges(Collection<DefaultWeightedEdge> edges) {
		for(TrackOverlay<T> trackOverlay : trackOverlays.values()) {
			trackOverlay.setHighlight(edges);
		}
	}

	@Override
	public void highlightSpots(Collection<Spot> spots) {
		for (SpotOverlay<T> spotOverlay : spotOverlays.values()) {
			spotOverlay.setSpotSelection(spots);
		}
		for(ImagePlus imp : imps) {
			imp.updateAndDraw();
		}
	}

	@Override
	public void centerViewOn(Spot spot) {
		int frame = - 1;
		for(int i : model.getFilteredSpots().keySet()) {
			List<Spot> spotThisFrame = model.getFilteredSpots().get(i);
			if (spotThisFrame.contains(spot)) {
				frame = i;
				break;
			}
		}
		if (frame == -1)
			return;
		for(ImagePlus imp : imps) {
			double[] calibration = calibrations.get(imp);
			long z = Math.round(spot.getFeature(Spot.POSITION_Z) / calibration[2] ) + 1;
			imp.setPosition(1, (int) z, frame+1);
		}
	}

	@Override
	public void render() {
		clear();

		model.addTrackMateModelChangeListener(this);
		
		for (ImagePlus imp : imps) {
			imp.setOpenAsHyperStack(true);
			
			double[] calibration = TMUtils.getSpatialCalibration(imp);
			calibrations.put(imp, calibration);
			
			SpotOverlay<T> spotOverlay = createSpotOverlay(imp, model, displaySettings);
			spotOverlays.put(imp, spotOverlay);
			
			TrackOverlay<T> trackOverlay = createTrackOverlay(imp, model, displaySettings);
			trackOverlays.put(imp, trackOverlay);
			
			OverlayedImageCanvas canvas = new OverlayedImageCanvas(imp);
			canvases.put(imp, canvas);
			canvas.addOverlay(spotOverlay);
			canvas.addOverlay(trackOverlay);

			StackWindow window = new StackWindow(imp, canvas);
			window.setVisible(true);
			windows.put(imp,  window);
			
			imp.updateAndDraw();
		}
		registerEditTool();
		
	}


	@Override
	public void refresh() {
		for (ImagePlus imp : imps) {
			if (DEBUG) {
				System.out.println("[MultiViewDisplayer] Refreshing display of "+imp);
			}
			imp.updateAndDraw();
//			windows.get(imp).repaint();
		}
	}

	@Override
	public void clear() {
		for (OverlayedImageCanvas canvas : canvases.values()) {
			if (canvas != null)
				canvas.clearOverlay();
		}
	}	

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}


	/*
	 * PRIVATE METHODS
	 */

	private void registerEditTool() {
		editTool = MVSpotEditTool.getInstance();
		if (!MVSpotEditTool.isLaunched())
			editTool.run("");
		else {
			for (ImagePlus imp : imps) {
				editTool.imageOpened(imp);
			}
		}
		for (ImagePlus imp : imps) {
			editTool.register(imp, this);
		}
	}

	@Override
	public void setDisplaySettings(String key, Object value) {
		super.setDisplaySettings(key, value);
		// If we modified the feature coloring, then we recompute NOW the colors.
		if (key == TrackMateModelView.KEY_SPOT_COLOR_FEATURE) {
			for (SpotOverlay<T> spotOverlay : spotOverlays.values()) {
				spotOverlay.computeSpotColors();
			}
		}
		if (key == TrackMateModelView.KEY_TRACK_COLOR_FEATURE) {
			for (TrackOverlay<T> trackOverlay : trackOverlays.values()) {
				trackOverlay.computeTrackColors();
			}
		}
	}

	public SpotOverlay<T> getSpotOverlay(final ImagePlus imp) {
		return spotOverlays.get(imp);
	}

	public OverlayedImageCanvas getCanvas(final ImagePlus imp) {
		return canvases.get(imp);
	}

	public double[] getCalibration(final ImagePlus imp) {
		return calibrations.get(imp);
	}

	public Settings<T> getSttings() {
		return model.getSettings();
	}
}
