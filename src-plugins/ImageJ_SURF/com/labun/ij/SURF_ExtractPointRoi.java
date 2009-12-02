package com.labun.ij;
import mpicbg.models.*;

import ij.plugin.*;
import ij.gui.*;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.labun.surf.IntegralImage;
import com.labun.surf.InterestPoint;
import com.labun.surf.Matcher;
import com.labun.surf.Params;

/**
 * Extract landmark correspondences in two images as PointRoi.
 * 
 * The plugin uses ImageJ SURF, implemented by Eugen Labun <labun@gmx.net>
 * 
 * @author Eugen Labun <labun@gmx.net> and Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class SURF_ExtractPointRoi implements PlugIn
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	private ImagePlus imp1;
	private ImagePlus imp2;
	
	final private List< InterestPoint > fs1 = new ArrayList< InterestPoint >();
	final private List< InterestPoint > fs2 = new ArrayList< InterestPoint >();
	
	static private class Param
	{	
		public Params surf = new Params();
		
		public boolean doReverseComparisonToo = true;
		
		/**
		 * Closest/next closest neighbour distance ratio
		 */
		public float rod = 0.92f;
		
		/**
		 * Maximal allowed alignment error in px
		 */
		public float maxEpsilon = 25.0f;
		
		/**
		 * Inlier/candidates ratio
		 */
		public float minInlierRatio = 0.05f;
		
		/**
		 * Implemeted transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
		public int modelIndex = 1;
	}
	
	final static private Param p = new Param();
	
	
	
	public SURF_ExtractPointRoi()
	{
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	public void run( String args )
	{
		// cleanup
		fs1.clear();
		fs2.clear();
		
		if ( IJ.versionLessThan( "1.40" ) ) return;
		
		int[] ids = WindowManager.getIDList();
		if ( ids == null || ids.length < 2 )
		{
			IJ.showMessage( "You should have at least two images open." );
			return;
		}
		
		String[] titles = new String[ ids.length ];
		for ( int i = 0; i < ids.length; ++i )
		{
			titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
		}
		
		final GenericDialog gd = new GenericDialog( "Extract SIFT Landmark Correspondences" );
		
		gd.addMessage( "Image Selection:" );
		final String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice( "source_image", titles, current );
		gd.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
		
		gd.addMessage( "SURF Parameters:" );
		
		Params.addSurfParamsToDialog( gd );
		
		gd.addCheckbox( "Perform reverse comparison too (gives more accurate results)", p.doReverseComparisonToo );
		
		gd.addMessage( "Geometric Consensus Filter:" );
		gd.addNumericField( "maximal_alignment_error :", p.maxEpsilon, 2, 6, "px" );
		gd.addNumericField( "inlier_ratio :", p.minInlierRatio, 2 );
		gd.addChoice( "expected_transformation :", Param.modelStrings, Param.modelStrings[ p.modelIndex ] );
		
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		imp1 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		imp2 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		
		p.surf = Params.getSurfParamsFromDialog( gd );
		
		p.doReverseComparisonToo = gd.getNextBoolean();
		
//		p.rod = ( float )gd.getNextNumber();
		
		p.maxEpsilon = ( float )gd.getNextNumber();
		p.minInlierRatio = ( float )gd.getNextNumber();
		p.modelIndex = gd.getNextChoiceIndex();

		run( imp1, imp2, p );
	}

	public void run( final ImagePlus imp1, final ImagePlus imp2, final Param p )
	{
		Params p1 = new Params( p.surf );
		Params p2 = new Params( p.surf );
		long begin, end;
		
		p1.getStatistics().startTime = new Date();
		p1.getStatistics().imageTitle = imp1.getTitle();
		begin = System.currentTimeMillis();
		IJ.log( "Processing SURF ..." );
		final IntegralImage intImg1 = new IntegralImage( imp1.getProcessor(), true );
		end = System.currentTimeMillis();
		p1.getStatistics().timeIntegralImage = end - begin;
		fs1.addAll( IJFacade.detectAndDescribeInterestPoints( intImg1, p1 ) );
		IJ.log( " took " + ( System.currentTimeMillis() - begin ) + "ms." );
		IJ.log( fs1.size() + " features extracted." );
		
		p2.getStatistics().startTime = new Date();
		p2.getStatistics().imageTitle = imp2.getTitle();
		begin = System.currentTimeMillis();
		IJ.log( "Processing SURF ..." );
		final IntegralImage intImg2 = new IntegralImage( imp2.getProcessor(), true );
		end = System.currentTimeMillis();
		p2.getStatistics().timeIntegralImage = end - begin;
		fs2.addAll( IJFacade.detectAndDescribeInterestPoints( intImg2, p2 ) );
		IJ.log( " took " + ( System.currentTimeMillis() - begin ) + "ms." );
		IJ.log( fs2.size() + " features extracted." );
		
		begin = System.currentTimeMillis();
		IJ.log( "Identifying correspondence candidates using brute force ..." );
		Map< InterestPoint, InterestPoint > matchedPoints = Matcher.findMathes( fs1, fs2 );
		if ( p.doReverseComparisonToo )
		{
			Map< InterestPoint, InterestPoint > matchedPointsReverse = Matcher.findMathes( fs2, fs1 );
			matchedPoints = Compare_Images.intersection( matchedPoints, matchedPointsReverse );
		}
		IJ.log( " took " + ( System.currentTimeMillis() - begin ) + "ms." );	
		IJ.log( matchedPoints.size() + " potentially corresponding features identified." );
		end = System.currentTimeMillis();
		
		if ( matchedPoints.size() == 0 )
		{
			IJ.showMessage( "Extract SURF Correspondences", "No matches found." );
			return;
		}
		
		List< PointMatch > candidates = new ArrayList< PointMatch >();
		for ( final Map.Entry< InterestPoint, InterestPoint > e : matchedPoints.entrySet() )
			candidates.add(
					new PointMatch(
							new Point( new float[]{ e.getKey().x, e.getKey().y } ),
							new Point( new float[]{ e.getValue().x, e.getValue().y } ) ) );
		
		begin = System.currentTimeMillis();
		IJ.log( "Filtering correspondence candidates by geometric consensus ..." );
		List< PointMatch > inliers = new ArrayList< PointMatch >();
		
		Model< ? > model;
		switch ( p.modelIndex )
		{
		case 0:
			model = new TranslationModel2D();
			break;
		case 1:
			model = new RigidModel2D();
			break;
		case 2:
			model = new SimilarityModel2D();
			break;
		case 3:
			model = new AffineModel2D();
			break;
		default:
			return;
		}
		
		boolean modelFound;
		try
		{
			modelFound = model.filterRansac(
					candidates,
					inliers,
					1000,
					p.maxEpsilon,
					p.minInlierRatio );
		}
		catch ( NotEnoughDataPointsException e )
		{
			modelFound = false;
		}
			
		IJ.log( " took " + ( System.currentTimeMillis() - begin ) + "ms." );	
		
		if ( modelFound )
		{
			int x1[] = new int[ inliers.size() ];
			int y1[] = new int[ inliers.size() ];
			int x2[] = new int[ inliers.size() ];
			int y2[] = new int[ inliers.size() ];
			
			int i = 0;
			
			for ( PointMatch m : inliers )
			{
				float[] m_p1 = m.getP1().getL(); 
				float[] m_p2 = m.getP2().getL();
				
				x1[ i ] = Math.round( m_p1[ 0 ] );
				y1[ i ] = Math.round( m_p1[ 1 ] );
				x2[ i ] = Math.round( m_p2[ 0 ] );
				y2[ i ] = Math.round( m_p2[ 1 ] );
				
				++i;
			}
		
			PointRoi pr1 = new PointRoi( x1, y1, inliers.size() );
			PointRoi pr2 = new PointRoi( x2, y2, inliers.size() );
			
			imp1.setRoi( pr1 );
			imp2.setRoi( pr2 );
			
			IJ.log( inliers.size() + " corresponding features with an average displacement of " + decimalFormat.format( model.getCost() ) + "px identified." );
			IJ.log( "Estimated transformation model: " + model );
		}
		else
		{
			IJ.log( "No correspondences found." );
		}
	}
}
