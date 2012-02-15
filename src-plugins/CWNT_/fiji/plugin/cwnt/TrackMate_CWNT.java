package fiji.plugin.cwnt;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.Concatenator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeMap;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.labeling.Labeling;
import mpicbg.imglib.type.numeric.RealType;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.cwnt.segmentation.CWSettings;
import fiji.plugin.cwnt.segmentation.CrownWearingSegmenter;
import fiji.plugin.cwnt.segmentation.LabelToGlasbey;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.gui.ActionChooserPanel;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.gui.InitFilterPanel;
import fiji.plugin.trackmate.gui.LoadDescriptor;
import fiji.plugin.trackmate.gui.SaveDescriptor;
import fiji.plugin.trackmate.gui.SegmentationDescriptor;
import fiji.plugin.trackmate.gui.SegmenterChoiceDescriptor;
import fiji.plugin.trackmate.gui.SpotFilterDescriptor;
import fiji.plugin.trackmate.gui.StartDialogPanel;
import fiji.plugin.trackmate.gui.TrackFilterDescriptor;
import fiji.plugin.trackmate.gui.TrackerChoiceDescriptor;
import fiji.plugin.trackmate.gui.TrackingDescriptor;
import fiji.plugin.trackmate.gui.WizardController;
import fiji.plugin.trackmate.gui.WizardPanelDescriptor;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.hyperstack.SpotOverlay;
import fiji.plugin.trackmate.visualization.hyperstack.TrackOverlay;

/**
 * An entry for CWNT that uses {@link TrackMate_} as a GUI.
 * @author Jean-Yves Tinevez 2011-2012
 *
 */
public class TrackMate_CWNT extends TrackMate_ {

	/**
	 * The ImagePlus that will display labels resulting from segmentation.
	 */
	protected ImagePlus labelImp;

	protected TreeMap<Integer, Labeling<Integer>> labels = new TreeMap<Integer, Labeling<Integer>>();


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected List<SpotSegmenter<? extends RealType<?>>> createSegmenterList() {
		List<SpotSegmenter<? extends RealType<?>>> list = new ArrayList<SpotSegmenter<? extends RealType<?>>>(1);
		list.add(new CrownWearingSegmenter());
		return list;
	}

	@Override
	protected List<SpotFeatureAnalyzer> createSpotFeatureAnalyzerList() {
		List<SpotFeatureAnalyzer> analyzers = new ArrayList<SpotFeatureAnalyzer>(0);
		return analyzers;
	}

	@Override
	protected List<TrackMateModelView> createTrackMateModelViewList() {
		List<TrackMateModelView> views = new ArrayList<TrackMateModelView>(1);
		views.add(createLocalSliceDisplayer());
		return views;
	}

	@Override
	protected List<SpotTracker> createSpotTrackerList() {
		List<SpotTracker> trackers = new ArrayList<SpotTracker>(1);
		trackers.add(new NearestNeighborTracker());
		return trackers;
	}

	@Override
	protected void launchGUI() {
		WizardController controller = new WizardController(this) {

			@Override
			protected List<WizardPanelDescriptor> createWizardPanelDescriptorList() {
				List<WizardPanelDescriptor> descriptors = new ArrayList<WizardPanelDescriptor>(14);
				descriptors.add(new StartDialogPanel());
				descriptors.add(new SegmenterChoiceDescriptor());
				descriptors.add(new CWNTSegmentationDescriptor());
				descriptors.add(new CWMNTSpotFilterDescriptor());
				descriptors.add(new TrackerChoiceDescriptor());
				descriptors.add(new TrackingDescriptor());
				descriptors.add(new TrackFilterDescriptor());
				descriptors.add(new DisplayerPanel());
				descriptors.add(new ActionChooserPanel(this.plugin));
				descriptors.add(new InitFilterPanel()); // We put it, even if we skip it, so that we avoid NPE when loading

				descriptors.add(new LoadDescriptor());
				descriptors.add(new SaveDescriptor());
				return descriptors;
			}

		};

		// Show version number in the frame title bar
		controller.getWizard().setTitle("CWNT - β4");
	}


	/**
	 * We override the single segmentation method so as to be able to get
	 * the label image that is computed if desired.
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected List<Spot> execSingleFrameSegmentation(Image<? extends RealType<?>> img, Settings settings, int frameIndex) {
		final float[] calibration = settings.getCalibration();
		SpotSegmenter segmenter = settings.segmenter.createNewSegmenter();
		segmenter.setTarget(img, calibration, settings.segmenterSettings);

		if (segmenter.checkInput() && segmenter.process()) {

			// Extra step: get back label image. if required
			if (segmenter instanceof CrownWearingSegmenter) {

				CWSettings cws = (CWSettings) settings.segmenterSettings;
				if (cws.doDisplayLabels) {
					Labeling labelsThisFrame = ((CrownWearingSegmenter) segmenter).getLabeling();
					labels.put(frameIndex, labelsThisFrame);
				}

			}
			List<Spot> spotsThisFrame = segmenter.getResult();
			return translateAndPruneSpots(spotsThisFrame, settings);

		} else {
			model.getLogger().error(segmenter.getErrorMessage()+'\n');
			return null;
		}
	}


	/*
	 * INNER CLASSES
	 */




	private class  CWMNTSpotFilterDescriptor extends SpotFilterDescriptor {
		@Override
		public String getPreviousDescriptorID() {
			// So as to skip the initfilter step in the GUI
			return CWNTSegmentationDescriptor.DESCRIPTOR;
		}

	}

	private class  CWNTSegmentationDescriptor extends SegmentationDescriptor {

		@Override
		public String getNextDescriptorID() {
			// So as to skip the initfilter step in the GUI
			return CWMNTSpotFilterDescriptor.DESCRIPTOR;
		}

		/**
		 * Override to display all labels as an imp
		 */
		@Override
		public void aboutToHidePanel() {

			SegmenterSettings s = model.getSettings().segmenterSettings;
			if ( (s instanceof CWSettings)) {
				CWSettings cws = (CWSettings) s;
				if (cws.doDisplayLabels) {

					// Do that in another thread
					new Thread("Crown-Wearing segmenter label renderer thread") {

						public void run() {

							model.getLogger().log("Rendering labels started...\n");

							// Get all segmented frames
							int nFrames = labels.size();
							ImagePlus[] coloredFrames = new ImagePlus[nFrames];

							// Check if we have a label for each frame
							int index = 0;
							for (Labeling<Integer> labelThisFrame : labels.values()) {

								ImagePlus imp = null;

								if (null == labelThisFrame) {
									// No labels there. This frame was skipped. We prepare a blank imp to feed the final imp.
									imp = createBlankImagePlus();

								} else {
									// Get the label, make it an 8-bit colored imp
									LabelToGlasbey colorer = new LabelToGlasbey(labelThisFrame);
									if (colorer.checkInput() && colorer.process()) {
										imp = colorer.getImp();
									} else {
										// Blank image
										imp = createBlankImagePlus();
									}
								}
								coloredFrames[index] = imp;
								index++;
							}

							Concatenator cat = new Concatenator();
							labelImp = cat.concatenate(coloredFrames, false);
							int nSlices = model.getSettings().zend - model.getSettings().zstart + 1;
							labelImp.setCalibration(model.getSettings().imp.getCalibration());
							labelImp.setTitle(model.getSettings().imp.getShortTitle()+"_segmented");
							labelImp.setDimensions(1, nSlices, nFrames);
							labelImp.setOpenAsHyperStack(true);
							labelImp.show();

							model.getLogger().log("Rendering labels done.\n");
						};

					}.start();
				}
			}

			launchDisplayerAndComputeFeatures();
		}

		private ImagePlus createBlankImagePlus() {
			Settings settings = model.getSettings();
			int slices = settings.zend - settings.zstart;
			int height = settings.yend - settings.ystart;
			int width = settings.xend - settings.xstart;
			return NewImage.createByteImage("", width, height, slices, NewImage.FILL_BLACK);
		}

		private void launchDisplayerAndComputeFeatures() {
			// Launch renderer
			logger .log("Rendering results...\n",Logger.BLUE_COLOR);
			wizard.setNextButtonEnabled(false);
			final TrackMateModelView displayer = createLocalSliceDisplayer();
			wizard.setDisplayer(displayer);

			if (plugin.getModel().getSpots().getNSpots() > 0) {
				logger.log("Calculating features...\n",Logger.BLUE_COLOR);
				// Calculate features
				long start = System.currentTimeMillis();
				plugin.computeSpotFeatures();		
				long end  = System.currentTimeMillis();
				logger.log(String.format("Calculating features done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
			}

			try {
				displayer.setModel(plugin.getModel());
				displayer.render();
			} finally {
				// Re-enable the GUI
				logger.log("Rendering done.\n", Logger.BLUE_COLOR);
				wizard.setNextButtonEnabled(true);
			}
		}
	}


	/**
	 * Return a displayer where the tracks and the spots are only displayed in the current or nearing Z
	 * slices, to accommodate the large data spread in Z that is typically met by CWNT.
	 * @return
	 */
	private  HyperStackDisplayer createLocalSliceDisplayer() {

		return new HyperStackDisplayer() {

			@Override
			protected SpotOverlay createSpotOverlay() {

				return new SpotOverlay(model, model.getSettings().imp, displaySettings) {

					public void drawSpot(final Graphics2D g2d, final Spot spot, final float zslice, 
							final int xcorner, final int ycorner, final float magnification) {

						final float x = spot.getFeature(Spot.POSITION_X);
						final float y = spot.getFeature(Spot.POSITION_Y);
						final float z = spot.getFeature(Spot.POSITION_Z);
						final float dz2 = (z - zslice) * (z - zslice);
						float radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
						final float radius = spot.getFeature(Spot.RADIUS)*radiusRatio;
						if (dz2 >= radius * radius)
							return;

						// In pixel units
						final float xp = x / calibration[0];
						final float yp = y / calibration[1];
						// Scale to image zoom
						final float xs = (xp - xcorner) * magnification ;
						final float ys = (yp - ycorner) * magnification ;

						// Spots are painted as purple circled of white
						final float apparentRadius =  3 *radiusRatio; 
						g2d.fillOval(Math.round(xs - apparentRadius), Math.round(ys - apparentRadius), 
								Math.round(2 * apparentRadius), Math.round(2 * apparentRadius));		
						g2d.setColor(Color.CYAN.darker()); // Carefully set to reflect Aliette Leroy's opinion
						g2d.drawOval(Math.round(xs - apparentRadius), Math.round(ys - apparentRadius), 
								Math.round(2 * apparentRadius), Math.round(2 * apparentRadius));		

						boolean spotNameVisible = (Boolean) displaySettings.get(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES);
						if (spotNameVisible ) {
							String str = spot.toString();
							int xindent = fm.stringWidth(str) / 2;
							int yindent = fm.getAscent() / 2;
							g2d.drawString(spot.toString(), xs-xindent, ys+yindent);
						}
					}

				};
			}



			@Override
			protected TrackOverlay createTrackOverlay() {

				return new TrackOverlay(model, model.getSettings().imp, displaySettings) {

					@Override
					protected void drawEdge(Graphics2D g2d, Spot source, Spot target, int xcorner, int ycorner,	float magnification) {
						// Find x & y in physical coordinates
						final float x0i = source.getFeature(Spot.POSITION_X);
						final float y0i = source.getFeature(Spot.POSITION_Y);
						final float z0i = source.getFeature(Spot.POSITION_Z);
						final float x1i = target.getFeature(Spot.POSITION_X);
						final float y1i = target.getFeature(Spot.POSITION_Y);
						final float z1i = target.getFeature(Spot.POSITION_Z);
						// In pixel units
						final float x0p = x0i / calibration[0];
						final float y0p = y0i / calibration[1];
						final float x1p = x1i / calibration[0];
						final float y1p = y1i / calibration[1];

						// Check if we are nearing their plane, if not, do not draw
						final float zslice = (imp.getSlice()-1) * calibration[2];
						float radiusRatio = (Float) displaySettings.get(TrackMateModelView.KEY_SPOT_RADIUS_RATIO);
						final float tRadius = target.getFeature(Spot.RADIUS) * radiusRatio;
						final float dz1 = (z1i - zslice) * (z1i - zslice);
						final float sRadius = source.getFeature(Spot.RADIUS) * radiusRatio;
						final float dz0 = (z0i - zslice) * (z0i - zslice);

						if (dz1 >= tRadius * tRadius && dz0 >= sRadius * sRadius)
							return;

						// Scale to image zoom
						final float x0s = (x0p - xcorner) * magnification ;
						final float y0s = (y0p - ycorner) * magnification ;
						final float x1s = (x1p - xcorner) * magnification ;
						final float y1s = (y1p - ycorner) * magnification ;
						// Round
						final int x0 = Math.round(x0s);
						final int y0 = Math.round(y0s);
						final int x1 = Math.round(x1s);
						final int y1 = Math.round(y1s);

						g2d.drawLine(x0, y0, x1, y1);
					}

					/**
					 * Default color is all magenta
					 */
					public void computeTrackColors() {
						int ntracks = model.getNFilteredTracks();
						if (ntracks == 0)
							return;
						InterpolatePaintScale colorMap = (InterpolatePaintScale) displaySettings.get(TrackMateModelView.KEY_COLORMAP);
						Color defaultColor = (Color) displaySettings.get(TrackMateModelView.KEY_COLOR);
						edgeColors = new HashMap<Integer, Color>(ntracks);

						final String feature = (String) displaySettings.get(TrackMateModelView.KEY_TRACK_COLOR_FEATURE);
						if (feature != null) {

							// Get min & max
							double min = Float.POSITIVE_INFINITY;
							double max = Float.NEGATIVE_INFINITY;
							for (double val : model.getFeatureModel().getTrackFeatureValues().get(feature)) {
								if (val > max) max = val;
								if (val < min) min = val;
							}

							for(int i : model.getVisibleTrackIndices()) {
								Float val = model.getFeatureModel().getTrackFeature(i, feature);
								if (null == val) {
									edgeColors.put(i, defaultColor); // if feature is not calculated
								} else {
									edgeColors.put(i, colorMap.getPaint((float) (val-min) / (max-min)));
								}
							}

						} else {
							for(int i : model.getVisibleTrackIndices()) {
								edgeColors.put(i, defaultColor);
							}
						}
					}

				};

			}

		};

	}

}
