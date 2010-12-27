package ac;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.Attribute;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Class values are not sorted. To retrieve class values, use:
 *  
 *  SortedInstances si = ...
 *  si.values[si.classIndex][any_instance_index];
 * 
 * */
public class SortedInstances
{
	/** An array of [numAttributes][num instances] where each value is the value of an Instance for that given attribute. */
	final public double[][] values;
	/** An array of [numAttributes][num instances] where each i is the index of an Instance in the dataset. */
	final public int[][] indices;
	/** The weights of each instance */
	final public double[] weights;

	/** The indices of the features, aka attributes that are not the class.
	 *  This array's length is numAttributes -1. */
	final public int[] featureIndices;

	final public int classIndex, numAttributes, numClasses, size;

	final public boolean isNumeric;

	public SortedInstances(final Instances dataset) throws Exception {
		// Store properties
		this.classIndex = dataset.classIndex();
		this.numAttributes = dataset.numAttributes();
		this.numClasses = dataset.numClasses();
		this.size = dataset.size();
		this.isNumeric = dataset.classAttribute().isNumeric();

		// Create a template array of feature indices
		this.featureIndices = new int[numAttributes -1];
		for (int i=0, j=0; i<this.featureIndices.length; i++, j++) {
			if (i == classIndex) continue;
			this.featureIndices[j] = i;
		}

		// Put the data into one double[] for each attribute
		this.values = new double[numAttributes][size];
		this.weights = new double[size];
		for (int i=0; i < size; i++) {
			final Instance ins = dataset.get(i);
			for (int k=0; k < numAttributes; k++) {
				values[k][i] = ins.value(k);
			}
			weights[i] = ins.weight();
		}

		// Create indices relating each Instance to the values
		this.indices = new int[numAttributes][];
		final int[] range = new int[size];
		for (int i=0; i < size; i++) range[i] = i;
		this.indices[0] = range;
		for (int i=1; i < numAttributes; i++) this.indices[i] = range.clone();
	}

	public void sort() throws Exception {
		// Sort the instance values for a given attribute, remembering which was the original instance index
		// except for the class values--these then can be accessed directly.
		final Thread[] thread = new Thread[Runtime.getRuntime().availableProcessors()];
		final AtomicInteger ai = new AtomicInteger(0);
		for (int t=0; t<thread.length; t++) {
			thread[t] = new Thread() {
				{ setPriority(Thread.NORM_PRIORITY); }
				public void run() {
					for (int i = ai.getAndIncrement(); i < numAttributes; i++) {
						if (classIndex == i) continue; // class values are not sorted, so that they are retrieved directly by index.
						quicksort(values[i], indices[i], 0, values[0].length -1);
					}
				}
			};
			thread[t].start();
		}
		// Wait until all threads are done
		for (int t=0; t<thread.length; t++) {
			thread[t].join();
		}
	}

	// TODO should not use this, too much indirection
	// Instead, grab the values and indices arrays for the attribute of interest, and call with that
	public final double getValue(final int instanceIndex, final int attributeIndex) {
		return values[attributeIndex][indices[attributeIndex][instanceIndex]];
	}

	public final int[] createIndexRange() {
		final int[] range = new int[size];
		for (int i=0; i<range.length; i++) range[i] = i;
		return range;
	}

	/** Adapted from Preibisch code. */
	private static final void quicksort(final double[] values, final int[] indices, int left, int right)
	{
		// quicksort:
		int i = left,
		    j = right;
		final double x = values[(left + right) / 2];

		do {
			while (values[i] < x) i++;
			while (x < values[j]) j--;
			if (i <= j) {
				// Swap
				final double tmpD = values[i];
				values[i] = values[j];
				values[j] = tmpD;
				// At i, store the j, so that the value can be found by looking at the stored index
				final int old_i = indices[i];
				final int old_j = indices[j];
				indices[old_i] = j;
				indices[old_j] = i;
				
				i++;
				j--;
			}
		} while (i <= j);
		if (left < j) quicksort(values, indices, left, j);
		if (i < right) quicksort(values, indices, i, right);
	}
}
