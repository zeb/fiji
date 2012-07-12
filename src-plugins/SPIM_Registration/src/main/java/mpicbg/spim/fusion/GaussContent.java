package mpicbg.spim.fusion;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.fft.FourierConvolution;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import ij.IJ;
import ij.ImagePlus;

import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;

public class GaussContent extends IsolatedPixelWeightener<GaussContent> 
{
	Img<FloatType> gaussContent;
	
	protected GaussContent( final ViewDataBeads view, final ImgFactory< FloatType > entropyContainer ) 
	{
		super( view );
		
		try
		{			
			final int numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() / view.getNumViews() );
			
			final SPIMConfiguration conf = view.getViewStructure().getSPIMConfiguration();
			
			// get the kernels
			
			final double[] k1 = new double[ view.getNumDimensions() ];
			final double[] k2 = new double[ view.getNumDimensions() ];
			
			for ( int d = 0; d < view.getNumDimensions() - 1; ++d )
			{
				k1[ d ] = conf.fusionSigma1;
				k2[ d ] = conf.fusionSigma2;
			}
			
			k1[ view.getNumDimensions() - 1 ] = conf.fusionSigma1 / view.getZStretching();
			k2[ view.getNumDimensions() - 1 ] = conf.fusionSigma2 / view.getZStretching();		
			
			final Img<FloatType> kernel1 = FourierConvolution.createGaussianKernel( new ArrayImgFactory<FloatType>(), k1 );
			final Img<FloatType> kernel2 = FourierConvolution.createGaussianKernel( new ArrayImgFactory<FloatType>(), k2 );
	
			
			final Img<FloatType> conv1 = view.getImage().getImg().copy();
			final FFTConvolution<FloatType> fftConv1 = new FFTConvolution<FloatType>( conv1, kernel1 );
			fftConv1.run();
					
			// compute ( I - I*sigma1 )^2
			final Cursor<FloatType> cursorImg = view.getImage().cursor();
			final Cursor<FloatType> cursorConv = conv1.cursor();
			
			while ( cursorImg.hasNext() )
			{
				cursorImg.fwd();
				cursorConv.fwd();
				
				final float diff = cursorImg.get().get() - cursorConv.get().get();
				
				cursorConv.get().set( diff*diff );
			}
	
			// compute ( ( I - I*sigma1 )^2 ) * sigma2
			final FFTConvolution<FloatType> fftConv2 = new FFTConvolution<FloatType>( conv1, kernel1 );
			fftConv2.run();
			
			gaussContent = conv1;
			
			ViewDataBeads.normalizeImage( gaussContent, "Gausian content based of " + view.getName() );
		}
		catch ( OutOfMemoryError e )
		{
			IJ.log( "OutOfMemory: Cannot compute Gauss approximated Entropy for " + view.getName() + ": " + e );
			e.printStackTrace();
			gaussContent = null;
		}
	}

	@Override
	public RandomAccess<FloatType> randomAccess()
	{
        // the iterator we need to get values from the weightening image
		return gaussContent.randomAccess();
	}
	
	@Override
	public RandomAccess<FloatType> randomAccess( OutOfBoundsFactory<FloatType, Img<FloatType>> factory )
	{
        // the iterator we need to get values from the weightening image
		return Views.extend( gaussContent, factory).randomAccess();
	}
	
	@Override
	public void close()
	{
		gaussContent = null;
	}

	@Override
	public Img<FloatType> getResultImage() {
		return gaussContent;
	}
}
