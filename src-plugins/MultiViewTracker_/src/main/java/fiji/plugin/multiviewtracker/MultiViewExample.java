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
		List<ImagePlus> imps = new ArrayList<ImagePlus>();

		ImagePlus imp1 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-XY.tif");
		imp1.show();
		imps.add(imp1);
		
		ImagePlus imp2 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-XZ.tif");
		imp2.show();
		imps.add(imp2);
		
		ImagePlus imp3 = IJ.openImage("/Users/tinevez/Desktop/Data/Celegans-YZ.tif");
		imp3.show();
		imps.add(imp3);
		
		// Transforms
		Map<ImagePlus, AffineModel3D> transforms = new HashMap<ImagePlus, AffineModel3D>();

		AffineModel3D identity = new AffineModel3D();
		double[] calib1 = TMUtils.getSpatialCalibration(imp1);
		identity.set(
				(float) (1/calib1[0]), 	0, 			0, 			0,
				0, 			(float) (1/calib1[1]), 	0, 			0, 
				0, 			0, 			(float) (1/calib1[2]), 0);
		transforms.put(imp1, identity);
		

		double[] calib2 = TMUtils.getSpatialCalibration(imp2);
		AffineModel3D projXZ = new AffineModel3D();
		projXZ.set(
				(float) ( 1 / calib2[0] ), 	0, 		0, 			0,
				0, 			0, 		(float) ( - 1 / calib2[1] ), 	imp2.getHeight(), 
				0, 			(float) ( 1 / calib2[2]), 		0,  		0 );
		transforms.put(imp2, projXZ);
		
		AffineModel3D projYZ = new AffineModel3D();
		double[] calib3 = TMUtils.getSpatialCalibration(imp3);
		projYZ.set( 
				0, 0, (float) ( 1 / calib3[0] ), 0, 
				0, (float) (1/calib3[1]), 0, 0, 
				(float) ( - 1 / calib3[2] ), 0, 0, (float) ( imp3.getNSlices() ));
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
