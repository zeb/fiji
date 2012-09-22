

import fiji.plugin.multiviewtracker.MultiViewDisplayer;
import fiji.plugin.multiviewtracker.MultiViewTrackerConfigPanel;
import fiji.plugin.multiviewtracker.util.TransformUtils;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class MultiViewExample {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {

		// Set UI toolkit
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		// Launch ImageJ
		ImageJ.main(args);

		// Open several imp
		List<ImagePlus> imps = new ArrayList<ImagePlus>();

		ImagePlus imp1, imp2, imp3;

		if (IJ.isWindows()) {
			imp1 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans.tif");
			imp2 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans_XZ.tif");
			imp3 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans_YZ.tif");
		} else {
			imp1 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-XY.tif");
			imp2 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-XZ.tif");
			imp3 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-YZ.tif");
		}

//		imp1 = IJ.openImage("../Celegans-XY.tif");
//		imp2 = IJ.openImage("../Celegans-XZ.tif");
//		imp3 = IJ.openImage("../Celegans-YZ.tif");

		
		imp1.show();
		imps.add(imp1);

		imp2.show();
		imps.add(imp2);

		imp3.show();
		imps.add(imp3);

		// Transforms
		Map<ImagePlus, AffineTransform3D> transforms = new HashMap<ImagePlus, AffineTransform3D>();

		AffineTransform3D identity = TransformUtils.getTransformFromCalibration(imp1);
		transforms.put(imp1, identity);

		AffineTransform3D projXZ = TransformUtils.makeXZProjection(imp2);
		transforms.put(imp2, projXZ);

		AffineTransform3D projYZ = TransformUtils.makeYZProjection(imp3);
		transforms.put(imp3, projYZ);

		// Instantiate model
		Settings<T> settings = new Settings<T>(imp1);
		TrackMate_<T> plugin = new TrackMate_<T>(settings);
		TrackMateModel<T> model = plugin.getModel();

		// Initialize viewer
		MultiViewDisplayer<T> viewer = new MultiViewDisplayer<T>(imps, transforms, model);
		viewer.render();
		
		// Control panel
		MultiViewTrackerConfigPanel<T> mvFrame = new MultiViewTrackerConfigPanel<T>(model, viewer);
		mvFrame.setVisible(true);

	}

}
