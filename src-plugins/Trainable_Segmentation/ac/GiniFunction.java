package ac;

/**
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
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), 
 *          Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import weka.core.Instance;

/**
 * This class implements a split function based on the Gini coefficient
 *
 */
public class GiniFunction extends SplitFunction 
{
	
	/** Serial version ID */
	private static final long serialVersionUID = 9707184791345L;
	/** index of the splitting attribute */
	int index;
	/** threshold value of the splitting point */
	double threshold;
	/** flag to identify when all samples belong to the same class */
	boolean allSame;
	/** number of random features to use */
	final int numOfFeatures;
	/** random number generator */
	final Random random;

	/**
	 * Constructs a Gini function (initialize it)
	 * 
	 * @param numOfFeatures number of features to use
	 * @param random random number generator
	 */
	public GiniFunction(final int numOfFeatures, final Random random)
	{
		this.numOfFeatures = numOfFeatures;
		this.random = random;
	}

	/**
	 * Create split function based on Gini coefficient
	 *
	 * @param si All the instances, reachable by index.
	 * @param instanceIndices The indices of the list of Instance to use
	 */	 
	public void init(final SortedInstances si, final int[] instanceIndices, final int instanceIndicesSize)
	{
		if (0 == instanceIndicesSize)
		{
			this.index = 0;
			this.threshold = 0;
			this.allSame = true;
			return;
		}

		// Shuffle indices of features to use
		final int[] fIndices = shuffled(si.featureIndices); // makes a copy

		double minimumGini = Double.MAX_VALUE;

		//System.out.println("numElements: " + numElements + ", ins.length: " + ins.length + ", numFeatures: " + numOfFeatures);
		//System.out.println("numAttributes: " + numAttributes + ", numClasses: " + numClasses + ", classIndex: " + classIndex);

		// Class values are accessed directly by index
		final double[] classValues = si.values[si.classIndex];

		// initial probabilities (all samples on the right)
		final double[] initialProbRight = new double[si.numClasses];
		for (int k=0; k<instanceIndicesSize; k++) {
			initialProbRight[ (int) classValues[instanceIndices[k]]] ++;
		}

		for(int i=0; i < numOfFeatures; i++)
		{
			final int featureToUse = fIndices[ i ];
			final double[] values = si.values[featureToUse];
			final int[] indices = si.indices[featureToUse];

			// Get the smallest Gini coefficient

			// --- list[j] = new AttributeClassPair( ins[j].value( featureToUse ), (int) ins[j].value( classIndex ) );

			// initial probabilities (all samples on the right)
			final double[] probLeft  = new double[initialProbRight.length]; // == numClasses as length
			final double[] probRight = initialProbRight.clone();

			// Try all splitting points, from position 0 to the end
			for(int splitPoint=0; splitPoint < instanceIndicesSize; splitPoint++)
			{								
				// Calculate Gini coefficient
				double giniLeft = 0;
				double giniRight = 0;
				final int rightNumElements = instanceIndicesSize - splitPoint;

				for(int nClass = 0; nClass < probLeft.length; nClass++) // == numClasses as length
				{	
					// left set
					double prob = probLeft[nClass];
					// Divide by the number of elements to get probabilities
					if(splitPoint != 0)
						prob /= (double) splitPoint;
					giniLeft += prob * prob;

					// right set
					prob = probRight[nClass];
					// Divide by the number of elements to get probabilities
					if(rightNumElements != 0)
						prob /= (double) rightNumElements;
					giniRight += prob * prob;
				}

				// Total Gini value
				final double gini = ( (1.0 - giniLeft) * splitPoint 
									+ (1.0 - giniRight) * rightNumElements ) 
									/ (double) instanceIndicesSize;

				// Save values of minimum Gini coefficient
				if( gini < minimumGini )
				{
					minimumGini = gini;
					this.index = featureToUse;
					//this.threshold = list[splitPoint].attributeValue;
					this.threshold = values[indices[instanceIndices[splitPoint]]];
				}

				// update probabilities for next iteration
				probLeft[(int) classValues[instanceIndices[splitPoint]]] ++;
				probRight[(int) classValues[instanceIndices[splitPoint]]] --;
			}
		}
	}

	/**
	 * Evaluate a single instance based on the current 
	 * state of the split function
	 * 
	 * @param instance sample to evaluate
	 * @return false if the instance is on the right of the splitting point, true if it's on the left 
	 */
	public boolean evaluate(final SortedInstances si, final int ith)
	{
		if (allSame) return true;
		return si.values[this.index][ith] < this.threshold;
	}

	public boolean evaluate(Instance instance)
	{
		if (allSame) return true;
		return instance.value(this.index) < this.threshold;
	}

	@Override
	public SplitFunction newInstance() 
	{
		return new GiniFunction(this.numOfFeatures, this.random);
	}

	/** Return a new array, shuffled. */
	private final int[] shuffled(final int[] a) {
		final int[] b = a.clone();
		for (int i=a.length; i>1; i--) {
			final int k = random.nextInt(i);
			final int tmp = b[i-1];
			b[i-1] = b[k];
			b[k] = tmp;
		}
		return b;
	}
}
