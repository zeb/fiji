package mpicbg.spim.postprocessing.deconvolution;

import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

public class LucyRichardsonFFT 
{
	final Img<FloatType> image, kernel, weight;
	final FFTConvolution<FloatType> fftConvolution;
	
	Img<FloatType> viewContribution = null;
	
	public LucyRichardsonFFT( final Img<FloatType> image, final Img<FloatType> weight, final Img<FloatType> kernel, final int cpusPerView )
	{
		this.image = image;
		this.kernel = kernel;
		this.weight = weight;
		
		fftConvolution = new FFTConvolution<FloatType>( image, kernel );	
	}

	public Img<FloatType> getImage() { return image; }
	public Img<FloatType> getWeight() { return weight; }
	public Img<FloatType> getKernel() { return kernel; }
	public Img<FloatType> getViewContribution() { return viewContribution; }
	
	public FFTConvolution<FloatType> getFFTConvolution() { return fftConvolution; }
	
	public void setViewContribution( final Img<FloatType> viewContribution )
	{
		if ( this.viewContribution != null )
			this.viewContribution = null;
		
		this.viewContribution = viewContribution;
	}
}
