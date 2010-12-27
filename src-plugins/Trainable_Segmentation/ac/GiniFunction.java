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
	 * @param count The number of indices in instanceIndices that are not -1.
	 */	 
	public void init(final SortedInstances si, final int[] instanceIndices, final int count)
	{
		if (0 == count)
		{
			this.index = 0;
			this.threshold = 0;
			this.allSame = true;
			return;
		}

		// Shuffle indices of features to use
		//final int[] fIndices = si.shuffledFeatureIndices();

		double minimumGini = Double.MAX_VALUE;

		//System.out.println("numElements: " + numElements + ", ins.length: " + ins.length + ", numFeatures: " + numOfFeatures);
		//System.out.println("numAttributes: " + numAttributes + ", numClasses: " + numClasses + ", classIndex: " + classIndex);

		// Class values are accessed directly by index (not sorted)
		final double[] classValues = si.values[si.classIndex];  // these are the class values for all instances, not just for those referred to in instanceIndices.

		// initial probabilities (all samples on the right)
		final double[] initialProbRight = new double[si.numClasses];
		for (int i=0; i<instanceIndices.length; i++) {
			if (-1 == instanceIndices[i]) continue;
			initialProbRight[ (int) classValues[i]] ++; // classValues are not sorted, so access by i and not by instanceIndices[i] (would be the same)
		}

		for(int i=0; i < numOfFeatures; i++)
		{
			//final int featureToUse = fIndices[ i ];
			final int featureToUse = si.nextRandomFeatureIndex();
			final double[] values = si.values[featureToUse]; // already sorted. These are the sorted values of all instances for featureToUse feature or attribute, not just those referred to in the instanceIndices
			final int[] indices = si.indices[featureToUse];

			// Get the smallest Gini coefficient

			// --- list[j] = new AttributeClassPair( ins[j].value( featureToUse ), (int) ins[j].value( classIndex ) );

			// initial probabilities (all samples on the right)
			final double[] probLeft  = new double[initialProbRight.length]; // == numClasses as length
			final double[] probRight = initialProbRight.clone();

			// Try all splitting points, from position 0 to the end
			// Iterates sorted values.
			//for(int splitPoint=0; splitPoint < instanceIndicesSize; splitPoint++)
			for(int j=0, splitPoint=0; j < indices.length; j++)
			{
				// Retrieve the Instance index 'k' from the indices array (which was sorted along with the values array)
				final int k = indices[j];
				if (-1 == instanceIndices[k]) continue;

				// Calculate Gini coefficient
				double giniLeft = 0;
				double giniRight = 0;
				final int rightNumElements = count - splitPoint;

				for(int nClass = 0; nClass < probLeft.length; nClass++) // == numClasses as length
				{	
					// left set
					double prob = probLeft[nClass];
					// Divide by the number of elements to get probabilities
					if (splitPoint != 0)
						prob /= splitPoint;
					giniLeft += prob * prob;

					// right set
					prob = probRight[nClass];
					// Divide by the number of elements to get probabilities
					if (rightNumElements != 0)
						prob /= rightNumElements;
					giniRight += prob * prob;
				}

				// Total Gini value
				final double gini = ( (1.0 - giniLeft) * splitPoint 
									+ (1.0 - giniRight) * rightNumElements ) 
									/ (double) count;

				// Save values of minimum Gini coefficient
				if( gini < minimumGini )
				{
					minimumGini = gini;
					this.index = featureToUse;
					//this.threshold = list[splitPoint].attributeValue;
					this.threshold = values[j];
				}

				// update probabilities for next iteration
				probLeft[(int) classValues[k]] ++;   // use instanceIndices to find the original index of the Instance at j (via k = indices[j]), and then use it to retrieve its class value.
				probRight[(int) classValues[k]] --;

				// next
				splitPoint++;
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
		// Reverse look-up: given Instance at ith position, what is its value for attribute index 'this.index' ?
		return si.getInstanceAt(ith).value(this.index) < this.threshold;
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
}
