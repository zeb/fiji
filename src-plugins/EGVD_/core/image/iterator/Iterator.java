package core.image.iterator;

public interface Iterator {

	boolean hasNext();

	void next();

	int get();

	void set(int value);

	int getX();

	int getY();

	int getZ();

	int getPosition();

}
