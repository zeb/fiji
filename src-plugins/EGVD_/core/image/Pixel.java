package core.image;

public class Pixel {

	public int x;
	public int y;
	public int z;
	public int value;
	
	public Pixel() {		
	}
	
	public Pixel(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Pixel(int x, int y, int z, int value) {
		this(x, y, z);
		this.value = value;
	}
	
	public boolean equals(int x, int y, int z) {
		return (this.x == x && this.y == y && this.z == z);
	}
	
	public boolean equals(Pixel p) {
		return (x == p.x && y == p.y && z == p.z && value == p.value);
	}
}
