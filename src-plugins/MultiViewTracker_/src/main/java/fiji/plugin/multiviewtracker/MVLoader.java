package fiji.plugin.multiviewtracker;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import loci.formats.FormatException;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

public class MVLoader <T extends RealType<T> & NativeType<T>> implements PlugIn {

	public MVLoader() {}

	@Override
	public void run(String arg) {
		
		File folder = null;
		if (null != arg ) {
			folder = new File(arg);
		}
		
		Logger logger = Logger.IJ_LOGGER;
		File file = MVLauncher.askForFile(folder, null, logger);
		if (null == file) {
			return;
		}
		
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();
		
		TmXmlReader<T> reader = new TmXmlReader<T>(file, plugin, logger);
		reader.parse();
		
		// Build model form xml file
		TrackMateModel<T> model = plugin.getModel();
		
		// Settings
		Settings<T> settings = reader.getSettings();
		reader.getDetectorSettings(settings);
		model.setSettings(settings);

		// Spots
		SpotCollection allSpots = reader.getAllSpots();
		SpotCollection filteredSpots = reader.getFilteredSpots();
		model.setSpots(allSpots, false);
		model.setFilteredSpots(filteredSpots, false);
		
		// Tracks
		SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = reader.readTrackGraph();
		// Ensures lonely spots get added to the graph too
		for (Spot spot : model.getFilteredSpots()) {
			graph.addVertex(spot);
		}
		
		if (null != graph) {
			model.setGraph(graph);
		}
		
		// Logger
		model.setLogger(logger);
		
		// Load image dataset
		File imageFile = new File(settings.imageFolder, settings.imageFileName);
		Map<ImagePlus, List<AffineTransform3D>> impMap;

		try {
			
			// Load all the data
			impMap = MVLauncher.openSPIM(imageFile, true, Logger.IJ_LOGGER);
			
			// Configure model & setup settings
			settings.imp = impMap.keySet().iterator().next();
			settings.imageFileName = imageFile.getName();
			settings.imageFolder = imageFile.getParent();

			// Strip feature model
			model.getFeatureModel().setSpotFeatureFactory(null);
			
//			// Initialize viewer
//			logger.log("Instantiating viewer.\n");
//			MultiViewDisplayer<T> viewer = new MultiViewDisplayer<T>(impMap.keySet(), impMap, model);
//			logger.log("Rendering viewer.\n");
//			viewer.render();
//			logger.log("Done.\n");
//			
//			// Show controller
//			MultiViewTrackerConfigPanel<T> mtvc = new MultiViewTrackerConfigPanel<T>(model, viewer);
//			mtvc.setVisible(true);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}

	}

	/*
	 * MAIN
	 */
	
	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {
		ImageJ.main(args);
		String rootFolder = "E:/Users/JeanYves/Documents/Projects/PTomancak/Data";
		new MVLoader<T>().run(rootFolder);
	}
}
