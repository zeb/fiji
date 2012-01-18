package fiji.plugin.cwnt;

import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.type.numeric.RealType;

import fiji.plugin.cwnt.segmentation.CrownWearingSegmenter;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.gui.ActionChooserPanel;
import fiji.plugin.trackmate.gui.DisplayerChoiceDescriptor;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.gui.InitFilterPanel;
import fiji.plugin.trackmate.gui.LaunchDisplayerDescriptor;
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
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * An entry for CWNT that uses {@link TrackMate_} as a GUI.
 * @author Jean-Yves Tinevez 2011-2012
 *
 */
public class TrackMate_CWNT implements PlugIn {

	@Override
	public void run(String arg0) {

		TrackMate_ plugin = new TrackMate_() {

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
				WizardController controller = new WizardController(this) {

					@Override
					protected List<WizardPanelDescriptor> createWizardPanelDescriptorList() {
						List<WizardPanelDescriptor> descriptors = new ArrayList<WizardPanelDescriptor>(14);
						descriptors.add(new StartDialogPanel());
						descriptors.add(new SegmenterChoiceDescriptor());
						descriptors.add(new CWNTSegmentationDescriptor());
						descriptors.add(new CWMNTDisplayerChoiceDescriptor());
						descriptors.add(new LaunchDisplayerDescriptor());
						descriptors.add(new SpotFilterDescriptor());
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
				controller.getWizard().setTitle("CWNT - β3");
			}
		};

		plugin.run(arg0);

	}

	private class  CWMNTDisplayerChoiceDescriptor extends DisplayerChoiceDescriptor {
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
			return CWMNTDisplayerChoiceDescriptor.DESCRIPTOR;
		}
	}


}
