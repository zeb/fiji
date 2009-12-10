package fiji.plugin.constrainedshapes;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import fiji.util.AbstractTool;

public class TwoCircleTool extends AbstractTool {

	/*
	 * FIELDS
	 */
	
	TwoCircleShape tcs;
	ImagePlus imp;
	ImageCanvas canvas;
	

	
	/*
	 * PUBLIC METHODS
	 */
	
	public void run(String arg) {
		tcs = new TwoCircleShape();
		imp = WindowManager.getCurrentImage();
		Roi roi = null;
		if (imp != null) { 
			roi = imp.getRoi(); 
			canvas = imp.getCanvas();
		}
	}
	
	/*
	 * ABSTRACTTOOL METHODS
	 */
	
	@Override
	public String getToolIcon() {
		return "C000D38D39D3dD53D62D63D7dD8cD9cDc2Dc3Dc9DcaDd3Dd9C000D29D2aD2cD2dD37D3aD3cD3eD43D44D45D47D48D4dD4eD54D55D61D6dD6eD71D72D7cD7eD81D82D8dD9bDa1Da2DaaDabDb1Db2DbaDbbDc1DcbDd4Dd5Dd7Dd8De3De4De5De7De8De9C000C111C222C333C444D1aD1bD1cD28D2bD2eD35D36D3bD46D49D4fD52D56D57D5dD5eD5fD6fD80D8bD8eD90D91D92D9aDa0DacDd2Dd6DdaDe6Df5Df6Df7C444C555C666C777C888D19D1dD34D3fD42D51D58D64D70D73D7bD7fD8aD9dDb0Db3Db9DbcDc4Dc8Dd1DdbDe2DeaDf4Df8C888C999CaaaCbbbD18D1eD27D2fD33D41D4aD4cD59D60D65D6cD7aD83D8fD9eDa3Da9Dc0Dc5Dc7DccDe1DebDf3Df9CbbbCcccCdddCeeeCfff";
	}

	@Override
	public String getToolName() {
		return "Two-circle";
	}

	@Override
	public boolean hasOptionDialog() {
		return false;
	}

	@Override
	public void showOptionDialog() {}

}
