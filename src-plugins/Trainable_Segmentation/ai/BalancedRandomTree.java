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

import ij.IJ;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import weka.core.Instance;
import weka.core.Instances;

/**
 * This class implements a random tree based on the split
 * function specified by the template in Splitter
 * 
 */
public class BalancedRandomTree implements Serializable
{
	/** Generated serial version UID */
	private static final long serialVersionUID = 41518309467L;
	/** root node */
	private final BaseNode rootNode;

	/**
	 * Build random tree for a balanced random forest  
	 * 
	 * @param ins The instances to use.
	 * @param splitter split function generator
	 */
	public BalancedRandomTree(final Instance[] ins, final int numAttributes, final int numClasses, final int classIndex, final Splitter splitter, final ExecutorService exec) throws Exception
	{
		this.rootNode = createNode( ins, numAttributes, numClasses, classIndex, splitter, exec );
	}

	/**
	 * Build the random tree based on the data specified 
	 * in the constructor 
	 */
	private final BaseNode createNode(final Instance[] ins, final int numAttributes, final int numClasses, final int classIndex, final Splitter splitter, final ExecutorService exec) throws Exception
	{
			
		final long start = System.currentTimeMillis();
		try {
			return createTree(ins, numAttributes, numClasses, classIndex, 0, splitter, exec);
		} finally {
			final long end = System.currentTimeMillis();
			IJ.log("Creating tree took: " + (end-start) + "ms");
		}
	}

	/**
	 * Evaluate sample
	 * 
	 * @param instance sample to evaluate
	 * @return array of class probabilities
	 */
	public double[] evaluate(Instance instance)
	{
		if (null == rootNode)
			return null;
		return rootNode.eval(instance);
	}


	/**
	 * Basic node of the tree
	 *
	 */
	static abstract class BaseNode implements Serializable
	{

		/** serial version ID */
		private static final long serialVersionUID = 46734234231L;
		/**
		 * Evaluate an instance
		 * @param instance input sample
		 * @return class probabilities
		 */
		public abstract double[] eval( Instance instance );
		/**
		 * Get the node depth
		 * 
		 * @return tree depth at that node
		 */
		public int getDepth()
		{
			return 0;
		}
	} // end class BaseNode

	/**
	 * Leaf node in the tree 
	 *
	 */
	static class LeafNode extends BaseNode implements Serializable
	{
		/** serial version ID */
		private static final long serialVersionUID = 2019873470157L;
		/** class probabilites */
		double[] probability;

		@Override
		public double[] eval(Instance instance)   // TODO isn't this an error? instance is ignored
		{		
			return probability;
		}
		/**
		 * Create a leaf node
		 * 
		 * @param probability class probabilities
		 */
		public LeafNode(double[] probability)
		{
			this.probability = probability;
		}

		/**
		 * Create leaf node based on the current split data
		 *  
		 * @param data pointer to original data
		 * @param indices indices at this node
		 */
		public LeafNode(
				final Instance[] ins,
				final int numClasses)
		{
			this.probability = new double[ numClasses ];
			for(int i=0; i<ins.length; i++)
			{
				this.probability[ (int) ins[i].classValue() ] ++;
			}
			// Divide by the number of elements
			for(int i=0; i<probability.length; i++)
				this.probability[i] /= (double) ins.length;
		}

	} //end class LeafNode
	
	/**
	 * Interior node of the tree
	 *
	 */
	static class InteriorNode extends BaseNode implements Serializable
	{
		/** serial version ID */
		private static final long serialVersionUID = 9972970234021L;
		/** left son */
		BaseNode left;
		/** right son */
		BaseNode right;
		/** node depth */
		final int depth;
		/** split function that divides the samples into left and right sons */
		final SplitFunction splitFn;

		/**
		 * Constructs an interior node of the random tree
		 * 
		 * @param depth tree depth at this node
		 * @param splitFn split function
		 */
		private InteriorNode(int depth, SplitFunction splitFn) 
		{
			this.depth = depth;
			this.splitFn = splitFn;
		}

		/**
		 * Construct interior node of the tree
		 * 
		 * @param data pointer to the original set of samples
		 * @param indices indices of the samples at this node
		 * @param depth current tree depth
		 */
		/*		public InteriorNode(
				final Instances data,
				final ArrayList<Integer> indices,
				final int depth,
				final Splitter splitFnProducer)
		{
			this.splitFn = splitFnProducer.getSplitFunction(data, indices);


			this.depth = depth;

			// left and right new arrays
			final ArrayList<Integer> leftArray = new ArrayList<Integer>();
			final ArrayList<Integer> rightArray = new ArrayList<Integer>();

			// split data
			int totalLeft = 0;
			int totalRight = 0;
			for(final Integer it : indices)
			{
				if( splitFn.evaluate( data.get(it.intValue()) ) )
				{
					leftArray.add(it);
					totalLeft ++;					
				}
				else
				{
					rightArray.add(it);
					totalRight ++;
				}
			}
			//System.out.println("total left = " + totalLeft + ", total rigth = " + totalRight + ", depth = " + depth);					
			//indices.clear();
			if( totalLeft == 0 )
			{
				left = new LeafNode(data, rightArray);
			}
			else if ( totalRight == 0 )
			{
				left = new LeafNode(data, leftArray);
			}
			else
			{
				left = new InteriorNode(data, leftArray, depth+1, splitFnProducer);
				right = new InteriorNode(data, rightArray, depth+1, splitFnProducer);
			}				
		}*/


		/**
		 * Evaluate sample at this node
		 */
		public double[] eval(Instance instance) 
		{
			if( null != right)
			{
				if(this.splitFn.evaluate( instance ) )
				{
					return left.eval(instance);
				}
				else
					return right.eval(instance);
			}
			else // leaves are always left nodes 
				return left.eval(instance);				
		}


		/**
		 * Get node depth
		 */
		public int getDepth()
		{
			return this.depth;
		}
	}


	static private abstract class Task implements Callable<Integer>
	{
		private final Instance[] currentInstances;
		private final int numAttributes, numClasses, classIndex, depth;
		private final Splitter splitFnProducer;
		private final ExecutorService exec;
		private final Vector<Future<Integer>> futures;
		private final AtomicInteger counter;
		private final Object sentinel;

		private Task(final Instance[] ins, final int numAttributes, final int numClasses,
				final int classIndex, final int depth, final Splitter splitFnProducer,
				final ExecutorService exec, final Vector<Future<Integer>> futures,
				final AtomicInteger counter, final Object sentinel) {
			this.currentInstances = ins;
			this.numAttributes = numAttributes;
			this.numClasses = numClasses;
			this.classIndex = classIndex;
			this.depth = depth;
			this.splitFnProducer = splitFnProducer;
			this.exec = exec;
			this.futures = futures;
			this.counter = counter;
			counter.incrementAndGet();
			this.sentinel = sentinel;
		}

		public final Integer call() {

			final InteriorNode currentNode = new InteriorNode(depth, splitFnProducer.getSplitFunction(currentInstances, numAttributes, numClasses, classIndex));

			// new arrays for the left and right sons
			final ArrayList<Instance> leftArray = new ArrayList<Instance>();
			final ArrayList<Instance> rightArray = new ArrayList<Instance>();

			// split data
			for(int i=0; i < currentInstances.length; i++)
			{
				if( currentNode.splitFn.evaluate( currentInstances[i] ) )
				{
					leftArray.add(currentInstances[i]);
				}
				else
				{
					rightArray.add(currentInstances[i]);
				}
			}
			//System.out.println("total left = " + leftArray.size() + ", total right = " + rightArray.size() + ", depth = " + currentNode.depth);					

			if( leftArray.isEmpty() )
			{
				currentNode.left = new LeafNode(rightArray.toArray(new Instance[rightArray.size()]), numClasses);
				//System.out.println("Created leaf with feature " + currentNode.splitFn.index);
			}
			else if ( rightArray.isEmpty() )
			{
				currentNode.left = new LeafNode(leftArray.toArray(new Instance[leftArray.size()]), numClasses);
				//System.out.println("Created leaf with feature " + currentNode.splitFn.index);
			}
			else
			{
				futures.add(exec.submit(new Task(leftArray.toArray(new Instance[leftArray.size()]), numAttributes, numClasses, classIndex, depth+1, splitFnProducer, exec, futures, counter, sentinel) {
					@Override
					final void assign(final InteriorNode node) {
						currentNode.left = node;
					}
				}));

				futures.add(exec.submit(new Task(rightArray.toArray(new Instance[rightArray.size()]), numAttributes, numClasses, classIndex, depth+1, splitFnProducer, exec, futures, counter, sentinel) {
					@Override
					final void assign(final InteriorNode node) {
						currentNode.right = node;
					}
				}));
			}

			assign(currentNode);

			// Update maximum depth (for the record)
			/*
			if(currentNode.depth > maxDepth)
				maxDepth = currentNode.depth;
			*/

			if (0 == this.counter.decrementAndGet()) {
				synchronized (this.sentinel) {
					this.sentinel.notifyAll();
				}
			}

			return this.depth;
		}
		abstract void assign(InteriorNode node);
	}

	/**
	 * Create random tree (non-recursively)
	 * 
	 * @param data original data
	 * @param indices indices of the samples to use
	 * @param depth starting depth
	 * @param splitFnProducer split function producer
	 * @return root node 
	 */
	private InteriorNode createTree(
			Instance[] ins,
			final int numAttributes,
			final int numClasses,
			final int classIndex,
			final int depth,
			final Splitter splitFnProducer,
			final ExecutorService exec) throws Exception
	{
		final InteriorNode[] root = new InteriorNode[1];

		final Vector<Future<Integer>> futures = new Vector<Future<Integer>>();

		final Object sentinel = new Object();
		final AtomicInteger counter = new AtomicInteger(0);

		futures.add(exec.submit(new Task(ins, numAttributes, numClasses, classIndex, depth, splitFnProducer, exec, futures, counter, sentinel) {
			@Override
			final void assign(final InteriorNode node) {
				root[0] = node;
			}
		}));

		// Wait until all branches have been split
		synchronized (counter) {
			if (counter.get() > 0) {
				synchronized (sentinel) {
					sentinel.wait();
				}
			}
		}

		int maxDepth = 0;
		for (final Future<Integer> fu : futures) {
			maxDepth = Math.max(maxDepth, fu.get());
		}

		System.out.println("maxDepth: " + maxDepth);

		return root[0];
	}
}
