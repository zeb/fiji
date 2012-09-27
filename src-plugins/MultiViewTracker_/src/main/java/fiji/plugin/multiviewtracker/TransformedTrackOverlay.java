package fiji.plugin.multiviewtracker;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

/**
 * The overlay class in charge of drawing the tracks on the hyperstack window.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class TransformedTrackOverlay <T extends RealType<T> & NativeType<T>> implements Overlay {

	private final static boolean DEBUG = false;
	protected final ImagePlus imp;
	protected Map<Integer, Color> edgeColors;
	protected Map<String, Object> displaySettings;
	protected final TrackMateModel<T> model;
	protected final AffineTransform3D transform;

	/*
	 * CONSTRUCTOR
	 */

	public TransformedTrackOverlay(final TrackMateModel<T> model, final ImagePlus imp, final AffineTransform3D transform, final Map<String, Object> displaySettings) {
		this.model = model;
		this.imp = imp;
		this.transform = transform;
		this.displaySettings = displaySettings;
		computeTrackColors();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Provide default coloring.
	 */
	public void computeTrackColors() {
		int ntracks = model.getNFilteredTracks();
		if (ntracks == 0)
			return;

		if (DEBUG)
			System.out.println("[TransformedTrackOverlay] Recomputing track colors. Found " + ntracks + " visible tracks.");

		InterpolatePaintScale colorMap = (InterpolatePaintScale) displaySettings.get(TrackMateModelView.KEY_COLORMAP);
		Color defaultColor = (Color) displaySettings.get(TrackMateModelView.KEY_COLOR);
		edgeColors = new HashMap<Integer, Color>(ntracks);

		final String feature = (String) displaySettings.get(TrackMateModelView.KEY_TRACK_COLOR_FEATURE);
		if (feature != null) {

			// Get min & max
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			for (double val : model.getFeatureModel().getTrackFeatureValues().get(feature)) {
				if (val > max) max = val;
				if (val < min) min = val;
			}

			for(int i : model.getVisibleTrackIndices()) {
				Double val = model.getFeatureModel().getTrackFeature(i, feature);
				if (null == val) {
					edgeColors.put(i, defaultColor); // if feature is not calculated
				} else {
					edgeColors.put(i, colorMap.getPaint((double) (val-min) / (max-min)));
				}
			}

		} else {
			int index = 0;
			for(int i : model.getVisibleTrackIndices()) {
				edgeColors.put(i, colorMap.getPaint((double) index / (ntracks-1)));
				index ++;
			}
		}
	}

	@Override
	public final void paint(final Graphics g, final int xcorner, final int ycorner, final double magnification) {
		boolean tracksVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_TRACKS_VISIBLE);
		if (!tracksVisible  || model.getNFilteredTracks() == 0)
			return;

		final Set<DefaultWeightedEdge> highlight = model.getEdgeSelection();

		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();	
		final double dt = model.getSettings().dt;
		final float mag = (float) magnification;
		Spot source, target;

		// Deal with highlighted edges first: brute and thick display
		g2d.setStroke(new BasicStroke(4.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR);
		for (DefaultWeightedEdge edge : highlight) {
			source = model.getEdgeSource(edge);
			target = model.getEdgeTarget(edge);
			drawEdge(g2d, source, target, xcorner, ycorner, mag);
		}

		// The rest
		final int currentFrame = imp.getFrame() - 1;
		final int trackDisplayMode = (Integer) displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_MODE);
		final int trackDisplayDepth = (Integer) displaySettings.get(TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH);
		final List<Set<DefaultWeightedEdge>> trackEdges = model.getTrackEdges(); 
		final Set<Integer> filteredTrackIndices = model.getVisibleTrackIndices();

		g2d.setStroke(new BasicStroke(2.0f,  BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if (trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL || trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK) 
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

		// Determine bounds for limited view modes
		int minT = 0;
		int maxT = 0;
		switch (trackDisplayMode) {
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK:
			minT = currentFrame - trackDisplayDepth;
			maxT = currentFrame + trackDisplayDepth;
			break;
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK:
			minT = currentFrame;
			maxT = currentFrame + trackDisplayDepth;
			break;
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK:
			minT = currentFrame - trackDisplayDepth;
			maxT = currentFrame;
			break;
		}

		double sourceFrame;
		float transparency;
		switch (trackDisplayMode) {

		case TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE: {
			for (int i : filteredTrackIndices) {
				g2d.setColor(edgeColors.get(i));
				final Set<DefaultWeightedEdge> track = trackEdges.get(i);

				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getEdgeSource(edge);
					target = model.getEdgeTarget(edge);
					drawEdge(g2d, source, target, xcorner, ycorner, mag);
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK: 
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK: 
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK: {

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			for (int i : filteredTrackIndices) {
				g2d.setColor(edgeColors.get(i));
				final Set<DefaultWeightedEdge> track= trackEdges.get(i);

				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getEdgeSource(edge);
					sourceFrame = source.getFeature(Spot.POSITION_T) / dt;
					if (sourceFrame < minT || sourceFrame >= maxT)
						continue;

					target = model.getEdgeTarget(edge);
					drawEdge(g2d, source, target, xcorner, ycorner, mag);
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD: {

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			for (int i : filteredTrackIndices) {
				g2d.setColor(edgeColors.get(i));
				final Set<DefaultWeightedEdge> track= trackEdges.get(i);

				for (DefaultWeightedEdge edge : track) {
					if (highlight.contains(edge))
						continue;

					source = model.getEdgeSource(edge);
					sourceFrame = source.getFeature(Spot.POSITION_T) / dt;
					if (sourceFrame < minT || sourceFrame >= maxT)
						continue;

					transparency = (float) (1 - Math.abs(sourceFrame-currentFrame) / trackDisplayDepth);
					target = model.getEdgeTarget(edge);
					drawEdge(g2d, source, target, xcorner, ycorner, mag, transparency);
				}
			}
			break;

		}


		}

		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);


	}

	/* 
	 * PROTECTED METHODS
	 */

	protected void drawEdge(final Graphics2D g2d, final Spot source, final Spot target,
			final int xcorner, final int ycorner, final double magnification, final float transparency) {

		// Find x & y in physical coordinates
		final double[] physicalPositionSource = new double[3];
		source.localize(physicalPositionSource);

		final double[] physicalPositionTarget = new double[3];
		target.localize(physicalPositionTarget);

		// In pixel units
		final double[] pixelPositionSource = new double[3];
		transform.apply(physicalPositionSource, pixelPositionSource);
		final double[] pixelPositionTarget = new double[3];
		transform.apply(physicalPositionTarget, pixelPositionTarget);

		// Scale to image zoom
		final double x0s = (pixelPositionSource[0] - xcorner) * magnification ;
		final double y0s = (pixelPositionSource[1] - ycorner) * magnification ;
		final double x1s = (pixelPositionTarget[0] - xcorner) * magnification ;
		final double y1s = (pixelPositionTarget[1] - ycorner) * magnification ;

		// Round
		final int x0 = (int) Math.round(x0s);
		final int y0 = (int) Math.round(y0s);
		final int x1 = (int) Math.round(x1s);
		final int y1 = (int) Math.round(y1s);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
		g2d.drawLine(x0, y0, x1, y1);

	}

	protected void drawEdge(final Graphics2D g2d, final Spot source, final Spot target,
			final int xcorner, final int ycorner, final float magnification) {

		// Find x & y in physical coordinates
		final double[] physicalPositionSource = new double[3];
		source.localize(physicalPositionSource);

		final double[] physicalPositionTarget = new double[3];
		target.localize(physicalPositionTarget);

		// In pixel units
		final double[] pixelPositionSource = new double[3];
		transform.apply(physicalPositionSource, pixelPositionSource);
		final double[] pixelPositionTarget = new double[3];
		transform.apply(physicalPositionTarget, pixelPositionTarget);

		// Scale to image zoom
		final double x0s = (pixelPositionSource[0] - xcorner) * magnification ;
		final double y0s = (pixelPositionSource[1] - ycorner) * magnification ;
		final double x1s = (pixelPositionTarget[0] - xcorner) * magnification ;
		final double y1s = (pixelPositionTarget[1] - ycorner) * magnification ;

		// Round
		final int x0 = (int) Math.round(x0s);
		final int y0 = (int) Math.round(y0s);
		final int x1 = (int) Math.round(x1s);
		final int y1 = (int) Math.round(y1s);

		g2d.drawLine(x0, y0, x1, y1);
	}

	/**
	 * Ignored.
	 */
	@Override
	public void setComposite(Composite composite) {	}

}