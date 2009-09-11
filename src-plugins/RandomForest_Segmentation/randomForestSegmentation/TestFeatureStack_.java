package randomForestSegmentation;
/* Thest plugin to see if the feature stack contains anything useful.
 * 
 * License: GPL
 *
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
 * Author: Verena Kaynig (verena.kaynig@inf.ethz.ch)
 */

import ij.IJ;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.gui.Roi;
import ij.ImageStack;
import ij.ImagePlus;
import ij.WindowManager;

public class TestFeatureStack_  implements PlugIn{
	
	public void run(String args){
		ImagePlus trainingImage = IJ.openImage("testImages/i00000-1.tif");
		FeatureStack test = new FeatureStack(trainingImage);
/*		for (float i=2.0f; i<test.getWidth()/5.0f; i*=2){
			test.addGaussianBlur(i);
			test.addGradient(i);
			test.addHessian(i);
			for (float j=2.0f; j<i; j*=2){
				test.addDoG(i, j);
			}
		}
		*/
		test.addMembraneFeatures(11, 1);
		test.addMembraneFeatures(21, 1);
		test.addMembraneFeatures(21, 3);
		test.addMembraneFeatures(31, 1);
		test.addMembraneFeatures(31, 3);
		test.show();
	}

}
