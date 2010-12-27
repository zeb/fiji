package ac;

import java.io.Serializable;
import weka.core.Instance;

public abstract class SplitFunction implements Serializable
{
	private static final long serialVersionUID = 1L;
	int index;
	double threshold;
	boolean allSame;
	public abstract void init(SortedInstances si, BitVector bits, int instanceIndicesSize);
	public abstract boolean evaluate(SortedInstances si, int ith);
	public abstract boolean evaluate(Instance instance);
	public abstract SplitFunction newInstance();
}
