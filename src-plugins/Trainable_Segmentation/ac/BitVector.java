package ac;

public final class BitVector {

	private final int size;
	private final long[] bits;

	public BitVector(final int size) {
		this.size = size;
		this.bits = QuickBitVector.makeBitVector(size, 1);
	}
	public BitVector(final long[] bits, final int size) {
		this.bits = bits;
		this.size = size;
	}

	public final int size() {
		return size;
	}
	public final long[] getBitsCopy() {
		return bits.clone();
	}
	public BitVector copy() {
		return new BitVector(bits.clone(), size);
	}
	public final boolean get(final int ith) {
		return QuickBitVector.get(this.bits, ith);
	}
	public final void put(final int ith, final boolean value) {
		QuickBitVector.put(this.bits, ith, value);
	}
	public final void setTrue(final int ith) {
		QuickBitVector.set(this.bits, ith);
	}
	public final void setFalse(final int ith) {
		QuickBitVector.clear(this.bits, ith);
	}
	public final void setAllTrue() {
		for (int i=0; i<bits.length; i++) {
			bits[i] = 0xffffffffffffffffL;
		}
	}

	// test
	static public final void main(String[] args) {
		BitVector v = new BitVector(70);
		// Alternating true and false
		for (int i=0; i<v.size(); i++) {
			if (0 == i % 2) v.setTrue(i);
			else v.setFalse(i);
		}
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<v.size(); i++) {
			sb.append(v.get(i)).append(", ");
		}
		System.out.println(sb.toString());
		
		// All true
		v.setAllTrue();
		sb = new StringBuilder();
		for (int i=0; i<v.size(); i++) {
			sb.append(v.get(i)).append(", ");
		}
		System.out.println(sb.toString());
	}
}
