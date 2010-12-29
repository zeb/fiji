package ac;

public final class BoundedArray {

	final int[] a;

	/** May be 0 &lt;= size &lt;= a.length */
	final int size;

	final boolean[] complete;

	public BoundedArray(final int[] a, final int size, final boolean[] complete) {
		this.a = a;
		this.size = size;
		this.complete = complete;
	}

	public BoundedArray(final int[] a) {
		this.a = a;
		this.size = a.length;
		this.complete = new boolean[a.length];
		for (int i=0; i<a.length; i++) this.complete[i] = true;
	}
}
