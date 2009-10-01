package stitching;

public class CorrelationResult
{
	public double R;
	public float[] translation;
	
	public String toString()
	{
		final StringBuffer s = new StringBuffer( "R: " );
		s.append( R ).append( ", t: ( " ).append( translation[ 0 ] );
		for ( int i = 1; i < translation.length; ++i )
			s.append( ", " ).append( translation[ i ] );
		return s.append( " )" ).toString();
		
	}
}
