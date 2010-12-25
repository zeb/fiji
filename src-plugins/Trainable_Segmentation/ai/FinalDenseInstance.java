package ai;

import weka.core.DenseInstance;
import weka.core.Instance;

/** A DenseInstance with inlineable methods. The methods lack error checking and thus never fork. */
public final class FinalDenseInstance extends DenseInstance {

	public FinalDenseInstance(final double weight, final double[] values) {
		super(weight, values);
	}

	/** Deep copy */
	public FinalDenseInstance(final Instance instance) {
		super(instance.weight(), instance.toDoubleArray());
	}

	/** Enable direct access to the value array, no error checking, no forks. */
	@Override
	public final double value(final int i) {
		return m_AttValues[i];
	}

	/** Shallow copy, accesses same data and same dataset instance. */
	@Override
	public final Instance copy() {
		final FinalDenseInstance fdi = new FinalDenseInstance(this.m_Weight, this.m_AttValues);
		fdi.m_Dataset = this.m_Dataset;
		return fdi;
	}
}
