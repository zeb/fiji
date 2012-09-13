package fiji.plugin.multiviewtracker;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class MultiViewExample {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {

		// Launch ImageJ
		ImageJ.main(args);
		
		// Open several imp
		File file;
		if (IJ.isWindows()) {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.tif");
		} else {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.tif");
		}
		
		ImagePlus imp1 = IJ.openImage(file.getAbsolutePath());
		imp1.setTitle("1");
		ImagePlus imp2 = IJ.openImage(file.getAbsolutePath());
		imp2.setTitle("2");
		ImagePlus imp3 = IJ.openImage(file.getAbsolutePath());
		imp3.setTitle("3");
		imp1.show();
		imp2.show();
		imp3.show();
		List<ImagePlus> imps = new ArrayList<ImagePlus>();
		imps.add(imp1);
		imps.add(imp2);
		imps.add(imp3);
		
		
		// Instantiate model
		Settings<T> settings = new Settings<T>(imp1);
		TrackMate_<T> plugin = new TrackMate_<T>(settings);
		TrackMateModel<T> model = plugin.getModel();
		
		// Initialize viewer
		MultiViewDisplayer<T> viewer = new MultiViewDisplayer<T>(imps, model);
		viewer.render();
		
		
		
	}

}
