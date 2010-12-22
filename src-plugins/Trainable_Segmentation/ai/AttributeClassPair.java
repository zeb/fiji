package ai;

/**
 * Inner class to order attributes while preserving class indices 
 *
 */
public final class AttributeClassPair
{
	/** real value of the corresponding attribute */
	final double attributeValue;
	/** index of the class associated to this pair */
	final int classValue;
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
}
