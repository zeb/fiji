package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.TrackMateModel;

import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.TrackerSettingsPanel;

public abstract class TrackerType implements InfoTextable {
	/**
	 * Create a new {@link TrackerSettings} object suited to the {@link SpotTracker} referenced by this enum.
	 */
	public abstract TrackerSettings createSettings();

	/**
	 * Return a new {@link SpotTracker} as selected in this settings object, initialized for the given model.
	 */
	public abstract SpotTracker getSpotTracker(TrackMateModel model);

	/** 
	 * Return a {@link TrackerSettingsPanel} that is able to configure the {@link SpotTracker}
	 * selected in the settings object.
	 */
	public abstract TrackerSettingsPanel createPanel(TrackerSettings trackerSettings);

	public String name() {
		return getClass().getName();
	}

	public static TrackerType SIMPLE_LAP_TRACKER = new TrackerType() {
		@Override
		public TrackerSettings createSettings() {
			TrackerSettings ts = new TrackerSettings();
			ts.allowMerging = false;
			ts.allowSplitting = false;
			return ts;
		}

		@Override
		public SpotTracker getSpotTracker(TrackMateModel model) {
			return new LAPTracker(model.getFilteredSpots(), model.getSettings().trackerSettings);
		}

		@Override
		public TrackerSettingsPanel createPanel(TrackerSettings trackerSettings) {
			return new SimpleLAPTrackerSettingsPanel(trackerSettings);
		}

		@Override
		public String toString() {
			return "Simple LAP tracker";
		}

		@Override
		public String getInfoText() {
			return "<html>" +
				"This tracker is identical to the LAP tracker present in this plugin, except that it <br>" +
				"proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" +
				"a distance and time condition. Track splitting and merging are not allowed, resulting <br>" +
				"in having non-branching tracks." +
				" </html>";
		}
	};

	public static TrackerType LAP_TRACKER = new TrackerType() {
		@Override
		public TrackerSettings createSettings() {
			return new TrackerSettings();
		}

		@Override
		public SpotTracker getSpotTracker(TrackMateModel model) {
			return new LAPTracker(model.getFilteredSpots(), model.getSettings().trackerSettings);
		}

		@Override
		public TrackerSettingsPanel createPanel(TrackerSettings trackerSettings) {
			return new LAPTrackerSettingsPanel(trackerSettings);
		}

		@Override
		public String toString() {
			return "LAP tracker";
		}

		@Override
		public String getInfoText() {
			return "<html>" +
				"This tracker is based on the Linear Assignment Problem mathematical framework. <br>" +
				"Its implementation is derived from the following paper: <br>" +
				"<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>" +
				"Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>" +
				" </html>";
		}
	};

	public static TrackerType SIMPLE_FAST_LAPT = new TrackerType() {
		@Override
		public TrackerSettings createSettings() {
			TrackerSettings ts = new TrackerSettings();
			ts.allowMerging = false;
			ts.allowSplitting = false;
			return ts;
		}

		@Override
		public SpotTracker getSpotTracker(TrackMateModel model) {
			return new FastLAPTracker(model.getFilteredSpots(), model.getSettings().trackerSettings);
		}

		@Override
		public TrackerSettingsPanel createPanel(TrackerSettings trackerSettings) {
			return new SimpleLAPTrackerSettingsPanel(trackerSettings);
		}

		@Override
		public String toString() {
			return "Simple Fast LAP tracker";
		}

		@Override
		public String getInfoText() {
			return "<html>" +
				"This tracker is identical to the " + TrackerType.LAP_TRACKER.toString() + ", expect that it <br>" +
				"uses Johannes Schindelin implementation of the Hungarian solver, that solves an assignment <br>" +
				"problem in O(n^3) instead of O(n^4)." +
				" </html>";
		}
	};

	public static TrackerType FAST_LAPT = new TrackerType() {
		@Override
		public TrackerSettings createSettings() {
			return new TrackerSettings();
		}

		@Override
		public SpotTracker getSpotTracker(TrackMateModel model) {
			return new FastLAPTracker(model.getFilteredSpots(), model.getSettings().trackerSettings);
		}

		@Override
		public TrackerSettingsPanel createPanel(TrackerSettings trackerSettings) {
			return new LAPTrackerSettingsPanel(trackerSettings);
		}

		@Override
		public String toString() {
			return "Fast LAP tracker";
		}

		@Override
		public String getInfoText() {
			return "<html>" +
				"This tracker is identical to the " + TrackerType.SIMPLE_LAP_TRACKER.toString() + ", expect that it <br>" +
				"uses Johannes Schindelin implementation of the Hungarian solver, that solves an assignment <br>" +
				"problem in O(n^3) instead of O(n^4)." +
				" </html>";
		}
	};

	public final static TrackerType[] types = {
		SIMPLE_LAP_TRACKER,
		LAP_TRACKER,
		SIMPLE_FAST_LAPT,
		FAST_LAPT
	};

	public static int ordinal(TrackerType type) {
		for (int i = 0; i < types.length; i++)
			if (type == types[i])
				return i;
		return -1;
	}

	public static TrackerType valueOf(String name) {
		for (int i = 0; i < types.length; i++)
			if (name.equals(types[i].toString()))
				return types[i];
		return null;
	}
}