package ij3d.plugin.transform;

import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.plugin.Viewer3DPlugin;
import imagej.ext.plugin.Menu;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;

@Plugin(type = Viewer3DPlugin.class, menu = {
		@Menu(label = "Transformation", mnemonic = 't'),
		@Menu(label = "Set Transform", weight = 2) })
public class SetTransform implements Viewer3DPlugin {
	@Parameter
	private Image3DUniverse universe;

	@Parameter()
	private String transformation = "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1";

	@Override
	public void run() {
		Content c = universe.getSelectedOrSingleContent();
		if(c != null)
			c.setTransform(Utils.toTransform(transformation));
	}
}
