package fiji.plugin.multiviewtracker.util;

import ij.IJ;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import mpicbg.models.AffineModel3D;

public class TransformUtils {

	
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
