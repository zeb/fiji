package fiji.plugin.multiviewtracker;

import ij.ImagePlus;
import ij.gui.StackWindow;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.models.AffineModel3D;
import mpicbg.models.NoninvertibleModelException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas;

public class MultiViewDisplayer <T extends RealType<T> & NativeType<T>> extends AbstractTrackMateModelView<T>  {

	private static final boolean DEBUG = false;
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
	Map<ImagePlus, TransformedSpotOverlay<T>> spotOverlays = new HashMap<ImagePlus, TransformedSpotOverlay<T>>();
	Map<ImagePlus, TransformedTrackOverlay<T>> trackOverlays = new HashMap<ImagePlus, TransformedTrackOverlay<T>>();

	private MVSpotEditTool<T> editTool;
	private Map<ImagePlus, AffineModel3D> transforms;

	/*
	 * CONSTRUCTORS
	 */

	public MultiViewDisplayer(List<ImagePlus> imps, Map<ImagePlus, AffineModel3D> transforms, TrackMateModel<T> model) {
		this.imps = imps;
		this.transforms = transforms;
		this.model = model;
		render();
	}

	/*
	 * DEFAULT METHODS
	 */

	final Spot getCLickLocation(final Point point, ImagePlus imp) {
		OverlayedImageCanvas canvas = canvases.get(imp);
		if (null == canvas) {
			System.err.println("ImagePlus "+imp+" is unknown to this displayer "+this);
			return null;
		}
		final float ix = (float) (canvas.offScreenXD(point.x) + 0.5);
		final float iy = (float) (canvas.offScreenYD(point.y) + 0.5);
		final float iz = imp.getSlice()-1;
		float[] physicalCoords = new float[] { 1, 1, 1 };
		float[] pixelCoords = new float[] { ix, iy, iz };
		try {
			physicalCoords = transforms.get(imp).applyInverse(pixelCoords);
		} catch (NoninvertibleModelException e) {
			System.err.println("Failed to convert click location to physical coordinates.\n" +
					"Model was: "+transforms.get(imp)+" and coords were "+pixelCoords);
			e.printStackTrace();
		}
		return new SpotImp(new double[] { physicalCoords[0], physicalCoords[1], physicalCoords[2] });
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected static <T extends RealType<T> & NativeType<T>> TransformedSpotOverlay<T> createSpotOverlay(final ImagePlus imp, final AffineModel3D transform, final TrackMateModel<T> model, final Map<String, Object> displaySettings) {
		return new TransformedSpotOverlay<T>(model, imp, transform, displaySettings);
	}

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected static <T extends RealType<T> & NativeType<T>> TransformedTrackOverlay<T> createTrackOverlay(final ImagePlus imp, final TrackMateModel<T> model, final Map<String, Object> displaySettings) {
		return new TransformedTrackOverlay<T>(model, imp, displaySettings);
	}

	@Override
	public void setModel(TrackMateModel<T> model) {
		// TODO
	}

	/*
	 * PUBLIC METHODS
	 */
	
//	@Override
//	public void displayChanged(DisplayChangeEvent e) {
//		if (vwins == null) return;
//
//		Object source = e.getSource();
//		int type = e.getType();
//		int value = e.getValue();
//
//		ImagePlus imp;
//		ImageWindow iw;
//
//		// Current imagewindow
//		ImageWindow iwc = WindowManager.getCurrentImage().getWindow();
//
//		// pass on only if event comes from current window
//		if (!iwc.equals(source)) return;
//	}

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
				for (TransformedTrackOverlay<T> trackOverlay : trackOverlays.values()) {
					trackOverlay.computeTrackColors();
				}
				redoOverlay = true;				
			} else {
				final List<Spot> spots = event.getSpots();
				if ( spots != null && spots.size() > 0) {
					redoOverlay = true;
					for (Spot spot : event.getSpots()) {
						if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_REMOVED) {
							for (TransformedTrackOverlay<T> trackOverlay : trackOverlays.values()) {
								trackOverlay.computeTrackColors();
							}
							break;
						}
					}
				}
			}
			break;

		case TrackMateModelChangeEvent.SPOTS_COMPUTED:
			for (TransformedSpotOverlay<T> spotOverlay : spotOverlays.values()) {
				spotOverlay.computeSpotColors();
			}
			redoOverlay = true;
			break;

		case TrackMateModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
		case TrackMateModelChangeEvent.TRACKS_COMPUTED:
			for (TransformedTrackOverlay<T> trackOverlay : trackOverlays.values()) {
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
		for(TransformedTrackOverlay<T> trackOverlay : trackOverlays.values()) {
			trackOverlay.setHighlight(edges);
		}
	}

	@Override
	public void highlightSpots(Collection<Spot> spots) {
		for (TransformedSpotOverlay<T> spotOverlay : spotOverlays.values()) {
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
			
			float[] physicalCoords = new float[3];
			spot.localize(physicalCoords);
			float[] pixelCoords = new float[] {1, 1, 1};
			try {
				pixelCoords = transforms.get(imp).applyInverse(physicalCoords);
			} catch (NoninvertibleModelException e) {
				System.err.println("Failed to convert view location to physical coordinates.\n" +
						"Model was: "+transforms.get(imp)+" and coords were "+pixelCoords);
				e.printStackTrace();
			}
			
			int z = Math.round(pixelCoords[2]) + 1;
			imp.setPosition(1, z, frame+1);
		}
	}

	@Override
	public void render() {
		clear();

		model.addTrackMateModelChangeListener(this);
		
		for (ImagePlus imp : imps) {
			imp.setOpenAsHyperStack(true);
			
			TransformedSpotOverlay<T> spotOverlay = createSpotOverlay(imp, transforms.get(imp), model, displaySettings);
			spotOverlays.put(imp, spotOverlay);
			
			TransformedTrackOverlay<T> trackOverlay = createTrackOverlay(imp, model, displaySettings);
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
			for (TransformedSpotOverlay<T> spotOverlay : spotOverlays.values()) {
				spotOverlay.computeSpotColors();
			}
		}
		if (key == TrackMateModelView.KEY_TRACK_COLOR_FEATURE) {
			for (TransformedTrackOverlay<T> trackOverlay : trackOverlays.values()) {
				trackOverlay.computeTrackColors();
			}
		}
	}

	public TransformedSpotOverlay<T> getSpotOverlay(final ImagePlus imp) {
		return spotOverlays.get(imp);
	}

	public OverlayedImageCanvas getCanvas(final ImagePlus imp) {
		return canvases.get(imp);
	}

	public  AffineModel3D getTransform(final ImagePlus imp) {
		return transforms.get(imp);
	}

	public Settings<T> getSttings() {
		return model.getSettings();
	}
}
