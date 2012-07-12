package mpicbg.spim.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import mpicbg.models.AbstractAffineModel3D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.TranslationModel3D;
import mpicbg.spim.registration.ViewStructure;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import spimopener.SPIMExperiment;

public class SPIMConfiguration
{
	// general
	public String timepointPattern;
	public int timepoints[];
	public String anglePattern;
	public int angles[];
	public int illuminations[];

	//public String angleString;
	public String inputFilePattern;//spim_TL{i}_Angle\d*\.lsm
	public int[] channels, channelsRegister, channelsFuse;
	public int[][] channelsMirror;
	public String channelPattern;
	public String channelsToRegister;
	public String channelsToFuse;
	public String mirrorChannels = "";
	public int[] registrationAssignmentForFusion = null;

	// [timepoint][channel][angle][illumination]
	public File file[][][][];
	public String inputdirectory;
	public String outputdirectory;// = "";
	public String registrationFiledirectory;// = "";
	public String debugLevel;
	public int debugLevelInt = ViewStructure.DEBUG_MAIN;
	public boolean showImageJWindow = false;
	public boolean multiThreadedOpening = false;
	public boolean collectRegistrationStatistics = false;
	public String transformationModel = "Affine";
	// time lapse
	public boolean timeLapseRegistration = false;
	public int referenceTimePoint = 1;
	public SPIMExperiment spimExperiment = null;

	// image factories
	public ImgFactory< FloatType > imageFactory = new ArrayImgFactory<FloatType>();
	public ImgFactory< FloatType > recursiveGaussFactory = new ArrayImgFactory<FloatType>();
	public ImgFactory< FloatType > imageFactoryFusion = new ArrayImgFactory<FloatType>();
	public ImgFactory< FloatType > outputImageFactory = new ArrayImgFactory<FloatType>();
	public ImgFactory< FloatType > entropyFactory = new ArrayImgFactory<FloatType>();
	public ImgFactory< FloatType > scaleSpaceFactory = new ArrayImgFactory<FloatType>();

	// for cached image arrays
	public String tempDir = null;

	// for the interpolation
	public OutOfBoundsFactory<FloatType, RandomAccessibleInterval< FloatType > > strategyFactoryOutput = 
			new OutOfBoundsConstantValueFactory<FloatType, RandomAccessibleInterval<FloatType>>( new FloatType() );
	public InterpolatorFactory<FloatType, RandomAccessible< FloatType > > interpolatorFactorOutput = 
			new NLinearInterpolatorFactory<FloatType>();

	// outofbounds strategy factories
	public OutOfBoundsFactory<FloatType, RandomAccessibleInterval< FloatType > > strategyFactoryGauss = 
			new OutOfBoundsMirrorFactory<FloatType, RandomAccessibleInterval<FloatType>>( OutOfBoundsMirrorFactory.Boundary.SINGLE );

	// segmentation
	public boolean writeOutputImage = true;
	public boolean showOutputImage = false;
	public boolean useScaleSpace = true;

	// which fusion weightening
    public boolean useGauss = false;
    public boolean useLinearBlening = true;

    public boolean paralellFusion = true;
    public boolean sequentialFusion = false;
    public int numParalellViews = 1;
    public boolean multipleImageFusion = false;
    public boolean isDeconvolution = false;

    public boolean registerOnly = false;
    public boolean displayRegistration = false;
    public boolean readSegmentation = false;
    public boolean writeSegmentation = true;
    public boolean readRegistration = false;
    public boolean writeRegistration = true;

	public boolean overrideImageZStretching = false;
	public double zStretching = 1;
	public int background = 0;

	// threshold segmentation
	public float threshold = 0.9f;
	public float fixedThreshold = 0.02f;
	public boolean useFixedThreshold = false;
	public double circularityFactor = 0.5;
	public int minBlackBorder = 1;
	public int minSize = 10;
	public int maxSize = 15 * 15 * 15;
    public boolean useCenterOfMass = false;

	// ScaleSpace Segmentation
	public float[] minPeakValue = new float[]{ 0.01f };
	public float[] minInitialPeakValue = null; // minPeakValue/10
	public float identityRadius = 3f;
	public float maximaTolerance = 0.01f;
	public float imageSigma = 0.5f;
	public float[] initialSigma = new float[]{ 1.8f };
	public int stepsPerOctave = 4;
	public int steps = 3;
	public boolean detectSmallestStructures = false;
	public int scaleSpaceNumberOfThreads = 0;

	// PointDescriptor properties
	public double differenceThreshold = 50;
	public double ratioOfDistance = 10;
	public int neighbors = 3;
    public boolean useAssociatedBeads = false;
    public boolean useRANSAC = true;

    // RANSAC
    public float max_epsilon = 5;
    public float min_inlier_ratio = 0.1f;
    public int numIterations = 1000;

	// output image
	public int scale = 1;
	public int cropOffsetX = 0;
	public int cropOffsetY = 0;
	public int cropOffsetZ = 0;
	public int cropSizeX = 0;
	public int cropSizeY = 0;
	public int cropSizeZ = 0;
	public int numberOfThreads = 0;

	// defines the sigma of the volumes injected
    public float sigma = 0.25f;

    // where the injected Gaussian Distributions are cut off
    public int cutOffRadiusGauss = 2;

    // the number of histogram bins for computing the entropy
    public int histogramBins = 256;

    // the window Sizes for computing the local entropy
    public int windowSizeX = 19;
    public int windowSizeY = 19;

    // linear blending
    public float alpha = 1.5f;

    // gauss fusion
    public float fusionSigma1 = 20;//42;
    public float fusionSigma2 = 40;//88;

    public AbstractAffineModel3D getModel()
    {
		if ( transformationModel.equals( "Translation" ) )
			return new TranslationModel3D();
		else if ( transformationModel.equals( "Rigid" ) )
			return new RigidModel3D();
		else
			return new AffineModel3D();
    }

    public int getIndexForTimePoint( final int timepoint )
    {
    	for ( int i = 0; i < timepoints.length; i++ )
    	{
    		if ( timepoints[ i ] == timepoint )
    			return i;
    	}

    	return -1;
    }

    public static ArrayList<Integer> parseIntegerString(final String integers) throws ConfigurationParserException
    {
    	if ( integers.trim().length() == 0 )
    		return new ArrayList<Integer>();

    	ArrayList<Integer> tmp = null;

		try
		{
	    	tmp = new ArrayList<Integer>();
	    	final String[] entries = integers.split(",");
	    	for (String s: entries)
	    	{
	    		s = s.trim();

	    		if (s.contains("-"))
	    		{
	    			int start = 0, end = 0, step;
	    			start = Integer.parseInt(s.substring(0, s.indexOf("-")));

	    			if (s.indexOf(":") < 0)
	    			{
	    				end = Integer.parseInt(s.substring(s.indexOf("-") + 1, s.length()));
	    				step = 1;
	    			}
	    			else
	    			{
	    				end = Integer.parseInt(s.substring(s.indexOf("-") + 1, s.indexOf(":")));
	    				step = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.length()));
	    			}

	    			if (end > start)
	    				for (int i = start; i <= end; i += step)
	    					tmp.add(i);
	    			else
	    				for (int i = start; i >= end; i -= step)
	    					tmp.add(i);
	    		}
	    		else
	    		{
	    			tmp.add(Integer.parseInt(s));
	    		}
	    	}
		}
		catch (final Exception e)
		{
			throw new ConfigurationParserException("Cannot parse pattern '" + integers + "'");
		}

		return tmp;
    }

    public void parseAngles() throws ConfigurationParserException
    {
    	final ArrayList<Integer> tmp = parseIntegerString(anglePattern);
    	angles = new int[tmp.size()];

    	for (int i = 0; i < tmp.size(); i++)
    		angles[i] = tmp.get(i);

    	// if there are no angles given take all
    	if ( angles.length == 0 )
    	{
    		final String replaceTL = getReplaceStringTimePoints( inputFilePattern );
    		final int numDigitsTL = replaceTL.length() - 2;
    		final String replaceAngle = getReplaceStringAngle( inputFilePattern );
    		String filePattern = inputFilePattern;

    		filePattern = filePattern.replace( replaceTL, getLeadingZeros( numDigitsTL, timepoints[ 0 ] ) );
    		filePattern = filePattern.replace( replaceAngle, "*" );

    		final String filePatternStart = filePattern.substring( 0, filePattern.indexOf( '*' ));
    		final String filePatternEnd = filePattern.substring( filePattern.indexOf( '*' ) + 1, filePattern.length() );

     		final String[] listing = getDirListing( inputdirectory, filePatternStart, filePatternEnd );
    		angles = new int[ listing.length ];

    		for ( int i = 0; i < listing.length; ++i )
    		{
    			String entry = listing[ i ];
    			entry = entry.substring( filePatternStart.length(), entry.length() - filePatternEnd.length() );

    			angles[ i ] = Integer.parseInt( entry );
    		}
    	}
    }

    public void parseChannels() throws ConfigurationParserException
    {
    	if ( channelPattern != null && channelPattern.trim().length() > 0 )
    	{
	    	final ArrayList<Integer> tmp = parseIntegerString( channelPattern );
	    	channels = new int[ tmp.size() ];

	    	for (int i = 0; i < tmp.size(); i++)
	    		channels[i] = tmp.get(i);
    	}
    	else
    	{
    		// there is always channel 0
    		channels = new int[ 1 ];

    		// ...except when it is Huisken format, then we just take the first channel (which might not be 0)
    		if ( isHuiskenFormat() )
    		{
    			channels[0] = spimExperiment.channelStart;
    		}
    	}

    	if ( channelsToRegister != null && channelsToRegister.trim().length() > 0 )
    	{
	    	final ArrayList<Integer> tmp = parseIntegerString( channelsToRegister );
	    	channelsRegister = new int[ tmp.size() ];

	    	for (int i = 0; i < tmp.size(); i++)
	    		channelsRegister[i] = tmp.get(i);
    	}
    	else
    	{
    		// there is always channel 0
    		channelsRegister = new int[ 1 ];

    		// ...except when it is Huisken format, then we just take the first channel (which might not be 0)
    		if ( isHuiskenFormat() )
    		{
    			channelsRegister[0] = spimExperiment.channelStart;
    		}
    	}

    	if ( channelsToFuse != null && channelsToFuse.trim().length() > 0 )
    	{
	    	final ArrayList<Integer> tmp = parseIntegerString( channelsToFuse );
	    	channelsFuse = new int[ tmp.size() ];

	    	for (int i = 0; i < tmp.size(); i++)
	    		channelsFuse[i] = tmp.get(i);
    	}
    	else
    	{
    		// there is always channel 0
    		channelsFuse = new int[ 1 ];

    		// ...except when it is Huisken format, then we just take the first channel (which might not be 0)
    		if ( isHuiskenFormat() )
    		{
    			channelsFuse[0] = spimExperiment.channelStart;
    		}
    	}

    	// test validity (channels for registration and fusion have to be a subclass of the channel pattern)
    	for ( final int cR : channelsRegister )
    	{
    		boolean contains = false;

    		for ( final int c : channels )
    			if ( c == cR )
    				contains = true;

    		if ( !contains )
    		{
			throw new ConfigurationParserException( "Channel " + cR + " that should be used for registration is not part of the channels " +
    					Util.printCoordinates( channels ) );
    		}
    	}

    	for ( final int cF : channelsFuse )
    	{
    		boolean contains = false;

    		for ( final int c : channels )
    			if ( c == cF )
    				contains = true;

    		if ( !contains )
    		{
			throw new ConfigurationParserException( "Channel " + cF + " that should be used for fusion is not part of the channels " +
    					Util.printCoordinates( channels ) );
    		}
    	}

    	// all channels in channels should be used for something
    	for ( final int c : channels )
    	{
    		boolean contains = false;

    		for ( final int cR : channelsRegister )
    			if ( c == cR )
    				contains = true;

    		for ( final int cF : channelsFuse )
    			if ( c == cF )
    				contains = true;

    		if ( !contains )
    		{
			throw new ConfigurationParserException( "Channel " + c + " is not used for anything (not registration, not fusion); stopping. " );
    		}
    	}

	if ( useScaleSpace )
	{
		final int numChannelsRegister = channelsRegister.length;

		if ( numChannelsRegister != initialSigma.length || numChannelsRegister != minPeakValue.length )
			throw new ConfigurationParserException( "The number of channels with beads does not match the number of DoG parameters." );

		// auto-adjust minInitialPeakValue
		if ( minInitialPeakValue == null || minInitialPeakValue.length != numChannelsRegister )
		{
			minInitialPeakValue = new float[ numChannelsRegister ];
			for ( int i = 0; i < numChannelsRegister; ++i )
				minInitialPeakValue[ i ] = minPeakValue[ i ] / 10;
		}
	}
    	// do we want to mirror some channels in advance??
    	if ( mirrorChannels.trim().length() > 0 )
    	{
    		final String[] mirror = mirrorChannels.trim().split( "," );
    		channelsMirror = new int[ mirror.length ][ 2 ];
    		int i = 0;

    		for ( String entry : mirror )
    		{
    			entry = entry.trim();

    			try
    			{
    				final int channel = Integer.parseInt( entry.substring( 0, entry.length() - 1 ) );
    				final String direction = entry.substring( entry.length()-1, entry.length() ).toLowerCase();

    				if ( direction.equalsIgnoreCase( "h" ) )
    				{
    					channelsMirror[ i ][ 0 ] = channel;
    					channelsMirror[ i ][ 1 ] = 0;
    				}
    				else if ( direction.equalsIgnoreCase( "v" ) )
    				{
    					channelsMirror[ i ][ 0 ] = channel;
    					channelsMirror[ i ][ 1 ] = 1;
    				}
    				else
    				{
    					throw new ConfigurationParserException( "Cannot parse channel mirroring information: " + entry + ": " + direction + " is unknown." );
    				}

    				i++;
    			}
    			catch ( final Exception e )
    			{
    				throw new ConfigurationParserException( "Cannot parse channel mirroring information: " + mirrorChannels.trim() + ": " + e );
    			}
    		}
    	}
    }

	public void parseIlluminations() throws ConfigurationParserException
    {
		if ( hasAlternatingIllumination() )
			illuminations = new int[] {0, 1};
		else
			illuminations = new int[] {0};
    }

	protected String[] getDirListing( final String directory, final String filePatternStart, final String filePatternEnd )
	{
		final File dir = new File( directory );

	    // It is also possible to filter the list of returned files.
	    // This example does not return any files that start with `.'.
	    final FilenameFilter filter = new FilenameFilter()
	    {
	        @Override
			public boolean accept(final File dir, final String name)
	        {
	            return name.startsWith( filePatternStart) && name.endsWith( filePatternEnd );
	        }
	    };

	    return dir.list(filter);
	}

    public void parseTimePoints() throws ConfigurationParserException
    {
    	final ArrayList<Integer> tmp = parseIntegerString(timepointPattern);

    	timepoints = new int[tmp.size()];

    	for (int i = 0; i < tmp.size(); i++)
    		timepoints[i] = tmp.get(i);
    }

    /*
    public String getUniqueNameForTimePoint(int timePoint)
    {
		// find how to parse
		String replaceTL = null, replaceAngle = null;
		int numDigitsTL = 0;
		int numDigitsAngle = 0;

		int i1 = inputFilePattern.indexOf("{t");
		int i2 = inputFilePattern.indexOf("t}");
		if (i1 > 0 && i2 > 0)
		{
			replaceTL = "{";

			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replaceTL += "t";

			replaceTL += "}";
		}

		i1 = inputFilePattern.indexOf("{a");
		i2 = inputFilePattern.indexOf("a}");
		if (i1 > 0 && i2 > 0)
		{
			replaceAngle = "{";

			numDigitsAngle = i2 - i1;
			for (int i = 0; i < numDigitsAngle; i++)
				replaceAngle += "a";

			replaceAngle += "}";
		}

		String fileName = inputFilePattern;

		if (replaceTL != null)
			fileName = fileName.replace(replaceTL, getLeadingZeros(numDigitsTL, timePoint));

		fileName = fileName.replace(replaceAngle, "");

		// cut the extension
		fileName = fileName.substring(0, fileName.lastIndexOf("."));

		// remove the angle string
		fileName = fileName.replace(angleString, "");

		return fileName;
    }
    */

    public static String getReplaceStringChannels( final String inputFilePattern )
    {
    	String replacePattern = null;
    	int numDigitsTL = 0;

		final int i1 = inputFilePattern.indexOf("{c");
		final int i2 = inputFilePattern.indexOf("c}");
		if (i1 >= 0 && i2 > 0)
		{
			replacePattern = "{";

			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replacePattern += "c";

			replacePattern += "}";
		}

		return replacePattern;
    }

    public static String getReplaceStringTimePoints( final String inputFilePattern )
    {
    	String replacePattern = null;
    	int numDigitsTL = 0;

		final int i1 = inputFilePattern.indexOf("{t");
		final int i2 = inputFilePattern.indexOf("t}");
		if (i1 >= 0 && i2 > 0)
		{
			replacePattern = "{";

			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replacePattern += "t";

			replacePattern += "}";
		}

		return replacePattern;
    }

    public static String getReplaceStringAngle( final String inputFilePattern )
    {
    	String replacePattern = null;
    	int numDigitsTL = 0;

		final int i1 = inputFilePattern.indexOf("{a");
		final int i2 = inputFilePattern.indexOf("a}");
		if (i1 >= 0 && i2 > 0)
		{
			replacePattern = "{";

			numDigitsTL = i2 - i1;
			for (int i = 0; i < numDigitsTL; i++)
				replacePattern += "a";

			replacePattern += "}";
		}

		return replacePattern;
    }

	public boolean isHuiskenFormat()
	{
		return spimExperiment != null;
	}

	public boolean hasAlternatingIllumination()
	{
		return isHuiskenFormat() && ( spimExperiment.d < ( spimExperiment.planeEnd + 1 - spimExperiment.planeStart ) );
	}

	public SPIMExperiment getSpimExperiment()
	{
		return spimExperiment;
	}

	public double getZStretchingHuisken()
	{
		return spimExperiment.pd / spimExperiment.pw;
	}

	public void getFilenamesHuisken() throws ConfigurationParserException
	{
		parseTimePoints();
		parseAngles();
		parseChannels();
		parseIlluminations();

		// generate some dummy filenames that will be used for bead/registration
		// files
		file = new File[ timepoints.length ][ channels.length ][ angles.length ][ illuminations.length ];

		// int sample = spimExperiment.sampleStart;
		// int region = spimExperiment.regionStart;
		// int plane = spimExperiment.planeStart;
		// int frame = spimExperiment.frameStart;
		// final String pathFormatString =
		// "s%03d/t%05d/r%03d/a%03d/c%03d/z%04d/%010d.dat";
		final String pathFormatString = "reg-t%05d-a%03d-c%03d-i%01d";

		for ( int tp = 0; tp < timepoints.length; ++tp )
			for ( int channel = 0; channel < channels.length; ++channel )
				for ( int angle = 0; angle < angles.length; ++angle )
					for ( int illumination = 0; illumination < illuminations.length; ++illumination )
					{
						file[ tp ][ channel ][ angle ][ illumination ] = new File( inputdirectory, String.format( pathFormatString, timepoints[ tp ], angles[ angle ], channels[ channel ], illuminations[ illumination ] ) );
					}
	}

    public void getFileNames() throws ConfigurationParserException
    {
		// find how to parse
		String replaceTL = getReplaceStringTimePoints( inputFilePattern );
		String replaceAngle = getReplaceStringAngle( inputFilePattern );
		String replaceChannel = getReplaceStringChannels( inputFilePattern );

		if ( replaceTL == null )
			replaceTL = "\\";

		if ( replaceAngle == null )
			replaceAngle = "\\";

		if ( replaceChannel == null )
			replaceChannel = "\\";

		final int numDigitsTL = Math.max( 0, replaceTL.length() - 2 );
		final int numDigitsAngle = Math.max( 0, replaceAngle.length() - 2 );
		final int numDigitsChannel = Math.max( 0, replaceChannel.length() - 2 );

		parseTimePoints();
		parseAngles();
		parseChannels();
		parseIlluminations();

		if ( replaceAngle.equals( "\\" ) )
			throw new ConfigurationParserException("You gave no pattern to substitute the angles in the file name");

		if ( angles.length < 2 )
			IOFunctions.println( "Warning: You gave less than two angles to process: " + anglePattern );

		//throw new ConfigurationParserException("You gave less than two angles to process: " + anglePattern);

		if (timepoints.length > 1 && replaceTL.equals( "\\" ) )
			throw new ConfigurationParserException("You gave more than one timepoint but no pattern to replace");

		file = new File[ timepoints.length ][ channels.length ][ angles.length ][ illuminations.length ];

		for ( int tp = 0; tp < timepoints.length; ++tp )
			for ( int channel = 0; channel < channels.length; ++channel )
				for ( int angle = 0; angle < angles.length; ++angle )
					for ( int illumination = 0; illumination < illuminations.length; ++illumination )
					{
						String fileName = inputFilePattern;
						if ( replaceTL != null )
							fileName = fileName.replace( replaceTL, getLeadingZeros( numDigitsTL, timepoints[ tp ] ) );

						fileName = fileName.replace( replaceAngle, getLeadingZeros( numDigitsAngle, angles[ angle ] ) );

						fileName = fileName.replace( replaceChannel, getLeadingZeros( numDigitsChannel, channels[ channel ] ) );

						file[ tp ][ channel ][ angle ][ illumination ] = new File( inputdirectory, fileName );
					}
	}

	public int getTimePointIndex( final int timepoint )
	{
		for ( int i = 0; i < timepoints.length; ++i )
			if ( timepoints[ i ] == timepoint )
				return i;
		return -1;
	}

	private static String getLeadingZeros(final int zeros, final int number)
	{
		String output = "" + number;

		while (output.length() < zeros)
			output = "0" + output;

		return output;
	}
}
