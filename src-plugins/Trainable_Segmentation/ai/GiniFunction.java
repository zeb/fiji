package ai;

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
 * 			Albert Cardona (acardona@ini.phys.ethz.ch)
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import weka.core.Instance;
import weka.core.Instances;

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
	 */	 
	public void init(final Instance[] ins, final int insSize, final int numAttributes, final int numClasses, final int classIndex)
	{
		if (0 == insSize)
		{
			this.index = 0;
			this.threshold = 0;
			this.allSame = true;
			return;
		}

		//final int len = data.numAttributes();
		//final int numElements = indices.size();
		//final int numClasses = data.numClasses();
		//final int classIndex = data.classIndex();
		
		// Create and shuffle indices of features to use
		//ArrayList<Integer> allIndices = new ArrayList<Integer>();
		//for(int i=0; i<len; i++)
		//	if(i != classIndex)
		//		allIndices.add(i);

		// Create and shuffle indices of features to use
		final int len = numAttributes;
		final int[] featureIndices = new int[len-1];
		for (int i=0, j=0; i<len; i++, j++) {
			if (classIndex == i) continue;
			featureIndices[j] = i;
		}
		final int[] fIndices = shuffled(featureIndices);

		final int numElements = insSize;

		double minimumGini = Double.MAX_VALUE;

		//System.out.println("numElements: " + numElements + ", ins.length: " + ins.length + ", numFeatures: " + numOfFeatures);
		//System.out.println("numAttributes: " + numAttributes + ", numClasses: " + numClasses + ", classIndex: " + classIndex);

		for(int i=0; i < numOfFeatures; i++)
		{
			final int featureToUse = fIndices[ i ];

			// Get the smallest Gini coefficient

			// Create list with pairs attribute-class
			//final ArrayList<AttributeClassPair> list = new ArrayList<AttributeClassPair>();
			final AttributeClassPair[] list = new AttributeClassPair[numElements];
			for(int j=0; j<numElements; j++)
			{
				//final Instance ins = ins[j];
				//list.add( new AttributeClassPair( ins.value( featureToUse ), (int) ins.value( classIndex ) ));
				//System.out.println("j: " + j + "ins[j]: " + ins[j]);
				list[j] = new AttributeClassPair( ins[j].value( featureToUse ), (int) ins[j].value( classIndex ) );
			}

			// Sort pairs in increasing order
			//Collections.sort(list, comp);  // calls Arrays.sort by copying the array, then replacing every element. No need for that!
			// From 22 to 15 seconds/tree
			//Arrays.sort( list, comp );
			// Down to 10.3 seconds/tree
			sort( list );

			final double[] probLeft  = new double[numClasses];
			final double[] probRight = new double[numClasses];
			// initial probabilities (all samples on the right)
			for(int n = 0; n < list.length; n++)
				probRight[list[n].classValue] ++;
			
			// Try all splitting points, from position 0 to the end
			for(int splitPoint=0; splitPoint < numElements; splitPoint++)
			{								
				// Calculate Gini coefficient
				double giniLeft = 0;
				double giniRight = 0;
				final int rightNumElements = numElements - splitPoint;

				for(int nClass = 0; nClass < numClasses; nClass++)
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
									/ (double) numElements;

				// Save values of minimum Gini coefficient
				if( gini < minimumGini )
				{
					minimumGini = gini;
					this.index = featureToUse;
					this.threshold = list[splitPoint].attributeValue;
				}

				// update probabilities for next iteration
				probLeft[list[splitPoint].classValue] ++;
				probRight[list[splitPoint].classValue] --;
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
	public boolean evaluate(final Instance instance) 
	{
		if(allSame)
			return true;
		else
			return instance.value(this.index) < this.threshold;
	}

	@Override
	public SplitFunction newInstance() 
	{
		return new GiniFunction(this.numOfFeatures, this.random);
	}

	private static final void sort(final AttributeClassPair[] acps)
	{
		// No need to sort:
		if (acps.length < 2) return;

		// Insertion sort limit:
		// 0 is 10.5
		// 7 is 10.8
		// 5 is 10.5
		// 4 is 10.3
		if (acps.length < 4) { // 7 == Arrays.INSERTIONSORT_THRESHOLD
			for (int i=0; i<acps.length-1; i++) {
				for (int j=i; j>0 && acps[j-1].attributeValue > acps[j].attributeValue; j--) {
					final AttributeClassPair tmp = acps[j];
					acps[j] = acps[j-1];
					acps[j-1] = acps[j];
				}
			}
		}

		quicksort(acps, 0, acps.length-1);
	}

	/** Adapted from Preibisch code. */
	private static final void quicksort(final AttributeClassPair[] acps, int left, int right)
	{
		// quicksort:
		int i = left,
		    j = right;
		final double x = acps[(left + right) / 2].attributeValue;

		do {
			while (acps[i].attributeValue < x) i++;
			while (x < acps[j].attributeValue) j--;
			if (i <= j) {
				final AttributeClassPair temp = acps[i];
				acps[i] = acps[j];
				acps[j] = temp;
				i++;
				j--;
			}
		} while (i <= j);
		if (left < j) quicksort(acps, left, j);
		if (i < right) quicksort(acps, i, right);
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
