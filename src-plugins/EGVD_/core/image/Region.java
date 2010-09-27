package core.image;

public class Region {

	public int topMostLeftPosition;
	public int size;
	public Image boundary = null;
	public Image regionImage = null;
	
	public boolean equals(Region analysis) {
		return (topMostLeftPosition == analysis.topMostLeftPosition && size == analysis.size);
	}

}
