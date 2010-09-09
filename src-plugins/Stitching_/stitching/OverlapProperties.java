/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * An execption is the FFT implementation of Dave Hale which we use as a library,
 * wich is released under the terms of the Common Public License - v1.0, which is 
 * available at http://www.eclipse.org/legal/cpl-v10.html  
 *
 * @author Stephan Preibisch
 */
package stitching;

public class OverlapProperties
{
	public ImageInformation i1, i2;
	public double R;
	public Point2D translation2D;
	public Point3D translation3D;
	public boolean validOverlap = true;
	public boolean correlationFallBack = false;
	public Point2D deltaToExpected;

	public void checkDeltaToExpected(double maxDeltaToExpectedCorrelation, double thresholdR)
	{
		Point2D expected2D = null;
		Point3D expected3D = null;
		if (translation2D != null)
		{
			expected2D = new Point2D(CommonFunctions.round(i2.offset[0] - i1.offset[0]), CommonFunctions.round(i2.offset[1] - i1.offset[1]));
			deltaToExpected = new Point2D(translation2D.x - expected2D.x, translation2D.y - expected2D.y);
		}
		else
		{
			expected3D = new Point3D(CommonFunctions.round(i2.offset[0] - i1.offset[0]), CommonFunctions.round(i2.offset[1] - i1.offset[1]), 0);
			deltaToExpected = new Point2D(translation3D.x - expected3D.x, translation3D.y - expected3D.y);
		}
		
		if (maxDeltaToExpectedCorrelation != -1 && Math.sqrt(Math.pow(deltaToExpected.x, 2) + Math.pow(deltaToExpected.y, 2)) > maxDeltaToExpectedCorrelation)
		{
			R =  thresholdR;
			correlationFallBack = true;
			if (translation2D != null)
				translation2D = expected2D;
			else
				translation3D = expected3D;
		}
	}
}
