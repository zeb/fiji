package ai;

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.Instance;

public abstract class SplitFunction implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int index;
	double threshold;
	boolean allSame;
	public abstract void init(Instance[] ins, int numAttributes, int numClasses, int classIndex);
	public abstract boolean evaluate(final Instance instance);
	public abstract SplitFunction newInstance();
}
