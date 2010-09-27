package core.function;

import java.util.ArrayList;

import core.image.Region;

public class RegionSizeFilter {

	private ArrayList<Region> regions;
	private int[] regionSizes;

	public RegionSizeFilter(ArrayList<Region> regions) {
		this.regions = regions;
		setRegionSizes();
	}

	private void setRegionSizes() {
		regionSizes = new int[regions.size()];
		for (int i = 0; i < regions.size(); i++) {
			regionSizes[i] = regions.get(i).size;
		}
	}

	public ArrayList<Region> getRegionsSmallerThan(int size) {
		ArrayList<Region> smallRegions = new ArrayList<Region>();
		for (Region region : regions) {
			if (region.size <= size) {
				smallRegions.add(region);
			}
		}
		return smallRegions;
	}
}
