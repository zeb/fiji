package ai;

/**
 * Inner class to order attributes while preserving class indices 
 *
 */
public final class AttributeClassPair
{
	/** real value of the corresponding attribute */
	double attributeValue;
	/** index of the class associated to this pair */
	int classValue;
	/**
	 * Create pair attribute-class
	 * 
	 * @param attributeValue real attribute value
	 * @param classIndex index of the class associated to this sample
	 */
	AttributeClassPair(final double attributeValue, final int classIndex)
	{
		this.attributeValue = attributeValue;
		this.classValue = classIndex;
	}

	final void set(final double attributeValue, final int classIndex)
	{
		this.attributeValue = attributeValue;
		this.classValue = classIndex;
	}
}
