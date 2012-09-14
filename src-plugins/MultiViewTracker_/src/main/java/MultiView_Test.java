package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.util.TextFileAccess;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import net.imglib2.img.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import loci.formats.FormatException;
import mpicbg.models.AffineModel3D;

public class MultiView_Test {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ImgIOException, FormatException, IOException {

		// Load transformation 1 
		File file_1 = new File("/Users/tinevez/Projects/PTomancak/Data/spim_TL80_Angle150.lsm.registration");
		AffineModel3D transform_1 = getModelFromFile(file_1);
		AffineModel3D zscaling_1 = getZScalingFromFile(file_1);
		transform_1.concatenate(zscaling_1);

		// Load transformation 2 
		File file_2 = new File("/Users/tinevez/Projects/PTomancak/Data/spim_TL80_Angle190.lsm.registration");
		AffineModel3D transform_2 = getModelFromFile(file_2);
		AffineModel3D zscaling_2 = getZScalingFromFile(file_2);
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
		
		// Viewer for 1
		HyperStackDisplayer<T> viewer1 = new HyperStackDisplayer<T>();
		
		

		
	}


	public static AffineModel3D getZScalingFromFile( final File file )	{
		final AffineModel3D model = new AffineModel3D();

		try  {
			final BufferedReader in = TextFileAccess.openFileRead( file );
			float z = 1;

			// the default if nothing is written
			String savedModel = "AffineModel3D";

			while ( in.ready() ) {
				String entry = in.readLine().trim();

				if (entry.startsWith("z-scaling:")) {
					z = Float.parseFloat(entry.substring(11, entry.length()));
				}
			}

			in.close();

			if ( !savedModel.equals("AffineModel3D") )
				System.out.println( "Warning: Loading a '" + savedModel + "' as AffineModel3D!" );

			model.set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, z, 0);

		}  catch (IOException e) {
			IJ.log( "Cannot find file: " + file.getAbsolutePath() + ": " + e );
			e.printStackTrace();
			return null;
		}

		return model;
	}	

	public static AffineModel3D getModelFromFile( final File file )	{
		final AffineModel3D model = new AffineModel3D();

		try 
		{
			final BufferedReader in = TextFileAccess.openFileRead( file );

			// get 12 entry float array
			final float m[] = new float[ 12 ];

			// the default if nothing is written
			String savedModel = "AffineModel3D";

			while ( in.ready() )
			{
				String entry = in.readLine().trim();

				if (entry.startsWith("m00:"))
					m[ 0 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m01:"))
					m[ 1 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m02:"))
					m[ 2 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m03:"))
					m[ 3 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m10:"))
					m[ 4 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m11:"))
					m[ 5 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m12:"))
					m[ 6 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m13:"))
					m[ 7 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m20:"))
					m[ 8 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m21:"))
					m[ 9 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m22:"))
					m[ 10 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("m23:"))
					m[ 11 ] = Float.parseFloat(entry.substring(5, entry.length()));
				else if (entry.startsWith("model:"))
					savedModel = entry.substring(7, entry.length()).trim();
			}

			in.close();

			if ( !savedModel.equals("AffineModel3D") )
				System.out.println( "Warning: Loading a '" + savedModel + "' as AffineModel3D!" );

			model.set( m[ 0 ], m[ 1 ], m[ 2 ], m[ 3 ], m[ 4 ], m[ 5 ], m[ 6 ], m[ 7 ], m[ 8 ], m[ 9 ], m[ 10 ], m[ 11 ] );

		} 
		catch (IOException e) 
		{
			IJ.log( "Cannot find file: " + file.getAbsolutePath() + ": " + e );
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return model;
	}	
}
