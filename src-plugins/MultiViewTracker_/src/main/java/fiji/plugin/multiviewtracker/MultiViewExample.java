package fiji.plugin.multiviewtracker;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AffineModel3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class MultiViewExample {

	public static <T extends RealType<T> & NativeType<T>, M extends AbstractAffineModel3D< AffineModel3D >> void main(String[] args) {

		// Launch ImageJ
		ImageJ.main(args);
		
		// Open several imp
		ImagePlus imp1 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans.tif");
		ImagePlus imp2 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans_XZ.tif");
		ImagePlus imp3 = IJ.openImage("E:/Users/JeanYves/Desktop/Data/Celegans_YZ.tif");
		imp1.show();
		imp2.show();
		imp3.show();
		List<ImagePlus> imps = new ArrayList<ImagePlus>();
		imps.add(imp1);
		imps.add(imp2);
		imps.add(imp3);
		
		// Transforms
		AffineModel3D identity = new AffineModel3D();
		double[] calib1 = TMUtils.getSpatialCalibration(imp1);
		identity.set(
				(float) (1/calib1[0]), 	0, 			0, 			0,
				0, 			(float) (1/calib1[1]), 	0, 			0, 
				0, 			0, 			(float) (1/calib1[2]), 0);
		

		double[] calib2 = TMUtils.getSpatialCalibration(imp2);
		AffineModel3D projXZ = new AffineModel3D();
		projXZ.set(0, 0, (float) ( 1 / calib2[2]), 0,
				0, (float) ( 1 / calib2[1] ), 0, 0, 
				(float) ( - 1 / calib2[0]), 0, 0,  imp2.getNSlices() );
		
		AffineModel3D projYZ = new AffineModel3D();
		double[] calib3 = TMUtils.getSpatialCalibration(imp3);
		projYZ.set( (float) ( 1 / calib3[0] ), 0, 0, 0, 
				0, 0, (float) ( - 1 / calib3[2] ), imp3.getHeight(), 
				0, (float) ( 1 / calib3[1] ), 0, 0);
		
		Map<ImagePlus, AffineModel3D> transforms = new HashMap<ImagePlus, AffineModel3D>();
		transforms.put(imp1, identity);
		transforms.put(imp2, projXZ);
		transforms.put(imp3, projYZ);
		
		// Instantiate model
		Settings<T> settings = new Settings<T>(imp1);
		TrackMate_<T> plugin = new TrackMate_<T>(settings);
		TrackMateModel<T> model = plugin.getModel();
		
		// Initialize viewer
		MultiViewDisplayer<T> viewer = new MultiViewDisplayer<T>(imps, transforms, model);
		viewer.render();
		
		
		
	}

}
