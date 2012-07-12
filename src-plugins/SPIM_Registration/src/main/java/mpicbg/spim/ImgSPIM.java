package mpicbg.spim;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

public class ImgSPIM 
{
	protected final static AtomicInteger ai = new AtomicInteger();
	
	protected final Img< FloatType > img;
	protected String name;
	protected float[] calibration;
	
	public ImgSPIM( final Img< FloatType > img, final String name, final float[] calibration )
	{
		this.img = img;
		this.name = name;
		this.calibration = calibration.clone();
	}

	public ImgSPIM( final Img< FloatType > img, final String name )
	{
		this.img = img;
		this.name = name;
		this.calibration = new float[ img.numDimensions() ];
		
		for ( int d = 0; d < img.numDimensions(); ++d )
			calibration[ d ] = 1;
	}

	public ImgSPIM( final Img< FloatType > img )
	{
		this( img, "img " + ai.incrementAndGet() );
	}
	
	public Img< FloatType > getImg() { return img; }
	public float[] getCalibration() { return calibration.clone(); }
	public String getName() { return name; }
	
	public void setName( String name ) { this.name = name; }
	public void setCalibration( float[] cal ) { this.calibration = cal.clone(); }
}
