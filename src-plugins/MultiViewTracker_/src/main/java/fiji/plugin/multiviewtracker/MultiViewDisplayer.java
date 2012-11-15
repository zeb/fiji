package fiji.plugin.multiviewtracker;

import ij.ImagePlus;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateSelectionChangeEvent;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas;

public class MultiViewDisplayer <T extends RealType<T> & NativeType<T>> extends AbstractTrackMateModelView<T> implements AdjustmentListener  {

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

	private Collection<ImagePlus> imps;
	Map<ImagePlus, StackWindow> windows = new HashMap<ImagePlus, StackWindow>();
	Map<ImagePlus, OverlayedImageCanvas> canvases = new HashMap<ImagePlus, OverlayedImageCanvas>();
	Map<ImagePlus, TransformedSpotOverlay<T>> spotOverlays = new HashMap<ImagePlus, TransformedSpotOverlay<T>>();
	Map<ImagePlus, TransformedTrackOverlay<T>> trackOverlays = new HashMap<ImagePlus, TransformedTrackOverlay<T>>();

	private MVSpotEditTool<T> editTool;
	private Map<ImagePlus, AffineTransform3D> transforms;
	/** The updater instance in charge of setting the views Z & T when {@link #centerViewOn(Spot)} is called. */
	private DisplayUpdater updater = new DisplayUpdater();
	/** The spot to center on when {@link #centerViewOn(Spot)} is called asynchronously. */
	private Spot spotToCenterOn;

	/*
	 * CONSTRUCTORS
	 */

	public MultiViewDisplayer(final Collection<ImagePlus> imps, final Map<ImagePlus, AffineTransform3D> transforms, final TrackMateModel<T> model) {
		this.imps = imps;
		this.transforms = transforms;
		this.model = model;
	}

	public MultiViewDisplayer(final Map<ImagePlus, AffineTransform3D> transforms, final TrackMateModel<T> model) {
		this(transforms.keySet(), transforms, model);
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
		final double ix = canvas.offScreenXD(point.x) + 0.5;
		final double iy = canvas.offScreenYD(point.y) + 0.5;
		final double iz = imp.getSlice()-1;
		double[] physicalCoords = new double[3];
		double[] pixelCoords = new double[] { ix, iy, iz };
		transforms.get(imp).applyInverse(physicalCoords, pixelCoords);

		if (DEBUG) {
			System.out.println("[MultiViewDisplayer] Got a mouse click on "+Util.printCoordinates(pixelCoords)+". Converted it to physical coords: "+Util.printCoordinates(physicalCoords));
		}

		return new SpotImp(physicalCoords);
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected static <T extends RealType<T> & NativeType<T>> TransformedSpotOverlay<T> createSpotOverlay(final ImagePlus imp, final AffineTransform3D transform, final TrackMateModel<T> model, final Map<String, Object> displaySettings) {
		return new TransformedSpotOverlay<T>(model, imp, transform, displaySettings);
	}

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for the spots. 
	 * @return
	 */
	protected static <T extends RealType<T> & NativeType<T>> TransformedTrackOverlay<T> createTrackOverlay(final ImagePlus imp, final AffineTransform3D transform, final TrackMateModel<T> model, final Map<String, Object> displaySettings) {
		return new TransformedTrackOverlay<T>(model, imp, transform, displaySettings);
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
	public void centerViewOn(final Spot spot) {
		spotToCenterOn = spot;
		updater.doUpdate();
	}
	
	private final void refreshCenterViewOnSpot() {
		for (ImagePlus imp : imps) {
			double[] physicalCoords = new double[3];
			spotToCenterOn.localize(physicalCoords);
			double[] pixelCoords =  new double[3];
			transforms.get(imp).apply(physicalCoords, pixelCoords);
			long z = Math.round(pixelCoords[2]) + 1;
			int frame = spotToCenterOn.getFeature(Spot.FRAME).intValue();
			imp.setPosition(1, (int) z, frame +1);
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

			TransformedTrackOverlay<T> trackOverlay = createTrackOverlay(imp, transforms.get(imp), model, displaySettings);
			trackOverlays.put(imp, trackOverlay);

			OverlayedImageCanvas canvas = new OverlayedImageCanvas(imp);
			canvases.put(imp, canvas);
			canvas.addOverlay(spotOverlay);
			canvas.addOverlay(trackOverlay);

			StackWindow window = new StackWindow(imp, canvas);
			window.setVisible(true);
			windows.put(imp,  window);

			// Add a listener for time slider
			Component[] cs = window.getComponents();
			if (cs.length > 2) {
				((ScrollbarWithLabel) cs[2]).addAdjustmentListener (this); // We assume the 2nd is for time
			}

		}

		model.addTrackMateSelectionChangeListener(this);
		registerEditTool();

	}

	@Override
	public void selectionChanged(TrackMateSelectionChangeEvent event) {
		super.selectionChanged(event);
		refresh();
	}

	@Override
	public void refresh() {
		for (ImagePlus imp : imps) {
			if (DEBUG) {
				System.out.println("[MultiViewDisplayer] Refreshing display of "+imp);
			}
			imp.updateAndDraw();
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

	public  AffineTransform3D getTransform(final ImagePlus imp) {
		return transforms.get(imp);
	}

	public Settings<T> getSttings() {
		return model.getSettings();
	}

	/**
	 * Notified when any of the imps change slider.
	 */
	@Override
	public void adjustmentValueChanged(AdjustmentEvent event) {
		setViewsT(event.getValue());
	}

	public void setViewsT(final int frame) {
		for (ImagePlus imp : imps) {
			imp.setT(frame);
		}
	}
	
	public void setViewsZ(int targetZ) {
		for (ImagePlus imp : imps) {
			imp.setZ(targetZ);
		}
	}


	/*
	 * PRIVATE CLASSES
	 */


	/**
	 * This is a helper class modified after a class by Albert Cardona
	 */
	private class DisplayUpdater extends Thread {
		long request = 0;

		// Constructor autostarts thread
		DisplayUpdater() {
			super("TrackMate displayer thread");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call displayer update from this thread
					if (r > 0)
						refreshCenterViewOnSpot(); 
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}


}
