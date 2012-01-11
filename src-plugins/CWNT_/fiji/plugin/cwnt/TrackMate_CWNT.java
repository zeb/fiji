package fiji.plugin.cwnt;

import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.cwnt.segmentation.CrownWearingSegmenter;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.gui.WizardController;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * An entry for CWNT that uses {@link TrackMate_} as a GUI.
 * @author Jean-Yves Tinevez 2011
 *
 */
public class TrackMate_CWNT implements PlugIn {

	@Override
	public void run(String arg0) {

		TrackMate_ plugin = new TrackMate_() {

			@SuppressWarnings("rawtypes")
			@Override
			protected List<SpotSegmenter> createSegmenterList() {
				List<SpotSegmenter> list = new ArrayList<SpotSegmenter>(1);
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
				views.add(CWNT_.createLocalSliceDisplayer());
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
				WizardController controller = new WizardController(this);
				controller.getWizard().setTitle("CWNT - β2");
			}


		};

		plugin.run(arg0);

	}

}
