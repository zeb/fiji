package ac;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.Attribute;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Class values are not sorted. To retrieve class values, use:
 *  
 *  SortedInstances si = ...
 *  si.values[si.classIndex][any_instance_index];
 * 
 * */
public class SortedInstances
{
	final private Instances dataset;

	/** An array of [numAttributes][num instances] where each value is the value of an Instance for that given attribute. */
	final public double[][] values;
	/** An array of [numAttributes][num instances] where the ith value is the index of an Instance in the dataset;
	 *  this array is thus associated with the values array. */
	final public int[][] indices;
	/** An array of [numAttributes][num instances] where the ith value is the index in the values array for the ith Instance.
	 * This array enables finding the index of the attribute in the corresponding sorted double[] array. */
	final public int[][] reverseIndices;

	/** The weights of each instance */
	final public double[] weights;

	/** The indices of the features, aka attributes that are not the class.
	 *  This array's length is numAttributes -1. */
	final public int[] featureIndices;

	final public int classIndex, numAttributes, numClasses, size;

	final public boolean isNumeric;

	final private Random random = new Random();

	public SortedInstances(final Instances dataset) throws Exception {
		// Store properties
		this.dataset = dataset;
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
		this.indices = new int[numAttributes][];  // TODO make classIndex entry null
		this.reverseIndices = new int[numAttributes][]; // TODO idem
		final int[] range = new int[size];
		for (int i=0; i < size; i++) range[i] = i;
		this.indices[0] = range;
		for (int i=1; i < numAttributes; i++) {
			this.indices[i] = range.clone();
			this.reverseIndices[i] = range.clone();
		}
	}

	public final Instance getInstanceAt(final int ith) {
		return dataset.get(ith);
	}

	public void sort() throws Exception {
		// Sort the instance values for a given attribute, remembering which was the original instance index
		// except for the class values--these then can be accessed directly.

		if (this.size < 2) return;
		final int last = this.size - 1;

		final Thread[] thread = new Thread[Runtime.getRuntime().availableProcessors()];
		final AtomicInteger ai = new AtomicInteger(0);
		for (int t=0; t<thread.length; t++) {
			thread[t] = new Thread() {
				{ setPriority(Thread.NORM_PRIORITY); }
				public void run() {
					try {
						for (int i = ai.getAndIncrement(); i < numAttributes; i = ai.getAndIncrement()) {
							if (classIndex == i) continue; // class values are not sorted, so that they are retrieved directly by index.
							quicksort(values[i], indices[i], 0, last);
							// Given the indices, create the inverse relationship
							final int[] indicesI = indices[i];
							final int[] reverseIndicesI = reverseIndices[i];
							for (int k=0; k<indicesI.length; k++) {
								reverseIndicesI[indicesI[k]] = k;
							}
						}
					} catch (Throwable t) {
						t.printStackTrace();
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
				final int tmpI = indices[i];
				indices[i] = indices[j];
				indices[j] = tmpI;
				
				i++;
				j--;
			}
		} while (i <= j);
		if (left < j) quicksort(values, indices, left, j);
		if (i < right) quicksort(values, indices, i, right);
	}

	/** Return a new array, shuffled. */
	public final int[] shuffledFeatureIndices() {
		final int[] b = featureIndices.clone();
		for (int i=b.length; i>1; i--) {
			final int k = random.nextInt(i);
			final int tmp = b[i-1];
			b[i-1] = b[k];
			b[k] = tmp;
		}
		return b;
	}

	public final int nextRandomFeatureIndex() {
		return featureIndices[random.nextInt(featureIndices.length)];
	}

	/** Test index-preserving quicksort. */
	static public final void main(String[] args) {
		double[] v = new double[20];
		int[] indices = new int[20];
		for (int i=0; i<v.length; i++) {
			v[i] = i;
			indices[i] = i;
		}
		StringBuilder sbo = new StringBuilder();
		for (int i=0; i<v.length; i++) {
			sbo.append(s3(v[i])).append(", ");
		}
		System.out.println("original: " + sbo.toString());
		// suffle b, a clone of v
		final double[] b = v.clone();
		final java.util.Random random = new java.util.Random();
		for (int i=v.length; i>1; i--) {
			final int k = random.nextInt(i);
			final double tmp = b[i-1];
			b[i-1] = b[k];
			b[k] = tmp;
		}
		// print shuffled
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<b.length; i++) {
			sb.append(s3(b[i])).append(", ");
		}
		System.out.println("shuffled: " + sb.toString());
		// quicksort b, with indices
		quicksort(b, indices, 0, b.length-1);
		// print sorted and indices
		StringBuilder sbs = new StringBuilder();
		StringBuilder sbi = new StringBuilder();
		for (int i=0; i<b.length; i++) {
			sbs.append(s3(b[i])).append(", ");
			sbi.append(s3(indices[i])).append(", ");
		}
		System.out.println("  sorted: " + sbs.toString());
		System.out.println(" indices: " + sbi.toString());
	}
	static private final String s3(final int val) {
		String s = Integer.toString(val);
		while (s.length() < 4) s = " " + s;
		return s;
	}
	static private final String s3(final double val) {
		return s3((int)val);
	}


	// Reverse quicksort
	private static final void quicksort2(final double[] values, final int[] indices, int left, int right) {
		if (left < right) {
			final int q = partition(values, indices, left, right);
			quicksort2(values, indices, left, q-1);
			quicksort2(values, indices, q+1, right);
		}
	}
	private static final int partition(final double[] values, final int[] indices, final int p, final int r) {
		final double x = values[r];
		int j = p - 1;
		for (int i=p; i<r; i++) {
			if (x <= values[i]) {
				j++;
				final double tmpD = values[j];
				values[j] = values[i];
				values[i] = tmpD;
				final int tmpI = indices[j];
				indices[j] = indices[i];
				indices[i] = tmpI;
			}
		}
		values[r] = values[j + 1];
		values[j + 1] = x;
		return j+1;
	}
}
