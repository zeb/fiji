package ai;

import weka.core.DenseInstance;

/** A DenseInstance with inlineable methods. The methods lack error checking and thus never fork. */
public final class FinalDenseInstance extends DenseInstance {

	public FinalDenseInstance(final double weight, final double[] values) {
		super(weight, values);
	}

	/** Enable direct access to the value array, no error checking, no forks. */
	@Override
	public final double value(final int i) {
		return m_AttValues[i];
	}
}
