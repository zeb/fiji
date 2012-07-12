package mpicbg.spim.fusion;

import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import mpicbg.spim.registration.ViewDataBeads;

public class GaussContentFactory implements IsolatedPixelWeightenerFactory<GaussContent>
{
	ImgFactory< FloatType > gaussContentContainer;
	
	public GaussContentFactory( final ImgFactory< FloatType > gaussContentContainer ) { this.gaussContentContainer = gaussContentContainer; }
	
	@Override
	public GaussContent createInstance( final ViewDataBeads view ) 
	{ 
		return new GaussContent( view, gaussContentContainer ); 
	}
	
	public String getDescriptiveName() { return "Gauss approximated Entropy"; }

	public void printProperties()
	{
		System.out.print("GaussContentFactory(): Owns Factory for Image<FloatType>");
	}
	
	public String getErrorMessage() { return ""; }

	@Override
	public void setParameters(String configuration) { }
	
}
