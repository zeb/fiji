package core.image;

public interface Iterable {

	int getIterationSize();
	
	int get(int x, int y, int z, int position);
	
	void set(int x, int y, int z, int position, int value);
	
	int getWidth();
	
	int getHeight();
	
	int getDepth();
}
