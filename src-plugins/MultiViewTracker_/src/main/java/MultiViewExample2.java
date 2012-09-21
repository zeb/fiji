import fiji.plugin.multiviewtracker.MultiViewDisplayer;
import fiji.plugin.multiviewtracker.util.TransformUtils;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import loci.formats.FormatException;
import loci.plugins.LociImporter;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.Importer;
import loci.plugins.in.ImporterOptions;
import net.imglib2.exception.ImgLibException;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class MultiViewExample2 {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ImgLibException, IOException, FormatException {

		ImageJ.main(args);


		// Load transformation 1 
		File file_1 = new File("/Users/tinevez/Projects/PTomancak/Data/registration/spim_TL80_Angle190.lsm.registration");
		AffineTransform3D transform_1 = TransformUtils.getTransformFromFile(file_1);
		AffineTransform3D zscaling_1 = TransformUtils.getZScalingFromFile(file_1);
		transform_1.concatenate(zscaling_1);

		// Load transformation 2 
		File file_2 = new File("/Users/tinevez/Projects/PTomancak/Data/registration/spim_TL80_Angle230.lsm.registration");
		AffineTransform3D transform_2 = TransformUtils.getTransformFromFile(file_2);
		AffineTransform3D zscaling_2 = TransformUtils.getZScalingFromFile(file_2);
		transform_2.concatenate(zscaling_2);

		String dirPath = "/Users/tinevez/Projects/PTomancak/Data/"; // dc.getDirectory();
		String path1 = dirPath + "spim_TL80_Angle190.lsm";
		String path2 = dirPath + "spim_TL80_Angle230.lsm"; // /Users/tinevez/Projects/PTomancak/Data/spim_TL80_Angle150.lsm

		ImagePlus imp1 = useImporter(path1);
		ImagePlus imp2 = useImporter(path2);

		Map<ImagePlus, AffineTransform3D> transforms = new HashMap<ImagePlus, AffineTransform3D>();
		transforms.put(imp1, transform_1);
		transforms.put(imp2, transform_2);

		List<ImagePlus> imps2 = new ArrayList<ImagePlus>();
		imps2.add(imp1);
		imps2.add(imp2);

		// Instantiate model
		Settings<T> settings = new Settings<T>(imp1);
		TrackMate_<T> tm = new TrackMate_<T>(settings);
		TrackMateModel<T> model = tm.getModel();

		// Initialize viewer
		MultiViewDisplayer<T> viewer = new MultiViewDisplayer<T>(imps2, transforms, model);
		viewer.render();
		
	}

	
	public static ImagePlus useImporter(String path) throws IOException, FormatException {
		LociImporter plugin = new LociImporter();
		Importer importer = new Importer(plugin);
		
		
		ImporterOptions  options = importer.parseOptions("");
		
		
		options.setId(path);
		options.setGroupFiles(false);
		options.setVirtual(true);
		options.setSplitChannels(true);
		options.setQuiet(true);
//
//		Location file = new Location(path);
//		FilePattern fp = new FilePattern(file);
//		System.out.println(fp.getPattern());
//		options.setId(path); // fp.getPattern());
//		System.out.println(options.getId());
//
//		options.setSpecifyRanges(true);
//		
//		options.setCBegin(0, 15);
//		options.setCEnd(0, 19);
//		options.setCStep(0, 4);
//
//		options.setTBegin(0, 0);
//		options.setTEnd(0, 4);
//		options.setTStep(0, 1);
//		
		
		ImportProcess process = new ImportProcess(options);
		ImagePlusReader reader = new ImagePlusReader(process);
		DisplayHandler displayHandler = new DisplayHandler(process);

		process.execute();
		
		
		ImagePlus[] imps = importer.readPixels(reader, options, displayHandler);
		importer.finish(process);
		return imps[0];

	}
}
