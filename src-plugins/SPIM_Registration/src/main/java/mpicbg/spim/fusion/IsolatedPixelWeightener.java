package mpicbg.spim.fusion;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.numeric.real.FloatType;
import mpicbg.spim.io.SPIMConfiguration;
import mpicbg.spim.registration.ViewDataBeads;

public abstract class IsolatedPixelWeightener<I>
{
	final ViewDataBeads view;
	final SPIMConfiguration conf;
	int debugLevel;
	
	protected IsolatedPixelWeightener( ViewDataBeads view )
	{
		this.view = view;
		this.conf = view.getViewStructure().getSPIMConfiguration();
		this.debugLevel = view.getViewStructure().getDebugLevel();
	}	
	
	public abstract Img<FloatType> getResultImage();
	public abstract RandomAccess<FloatType> randomAccess();
	public abstract RandomAccess<FloatType> randomAccess( OutOfBoundsFactory<FloatType, Img<FloatType>> factory);	
	public abstract void close();
}