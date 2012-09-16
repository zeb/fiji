
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import mpicbg.models.AffineModel3D;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.multiviewtracker.util.TransformUtils;

public class MultiView_Test {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ImgLibException, IOException {

		// Load transformation 1 
		File file_1 = new File("/Users/tinevez/Projects/PTomancak/Data/spim_TL80_Angle150.lsm.registration");
		AffineModel3D transform_1 = TransformUtils.getModelFromFile(file_1);
		AffineModel3D zscaling_1 = TransformUtils.getZScalingFromFile(file_1);
		transform_1.concatenate(zscaling_1);

		// Load transformation 2 
		File file_2 = new File("/Users/tinevez/Projects/PTomancak/Data/spim_TL80_Angle190.lsm.registration");
		AffineModel3D transform_2 = TransformUtils.getModelFromFile(file_2);
		AffineModel3D zscaling_2 = TransformUtils.getZScalingFromFile(file_2);
		transform_2.concatenate(zscaling_2);
		
		// Load stack 1
		File img_file_1 = new File("/Users/tinevez/Projects/PTomancak/Data/spim_TL80_Angle150.lsm");
		ImgPlus<T> img_1 = ImgOpener.open(img_file_1.getAbsolutePath());
		ImagePlus imp1 = ImageJFunctions.wrapUnsignedShort(img_1, "1");
		imp1.show();

		// Load stack 2
		File img_file_2 = new File("/Users/tinevez/Projects/PTomancak/Data/spim_TL80_Angle190.lsm");
		ImgPlus<T> img_2 = ImgOpener.open(img_file_2.getAbsolutePath());
		ImagePlus imp2 = ImageJFunctions.wrapUnsignedShort(img_2, "2");
		imp2.show();
	}

}
