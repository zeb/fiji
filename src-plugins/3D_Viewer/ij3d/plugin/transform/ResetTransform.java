package ij3d.plugin.transform;

import javax.media.j3d.Transform3D;

import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.plugin.Viewer3DPlugin;
import imagej.ext.plugin.Menu;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;

@Plugin(
		type=Viewer3DPlugin.class,
		menu={
			@Menu(label="Transformation", mnemonic='t'),
			@Menu(label="Reset Transform", weight=3)
		}
)
public class ResetTransform implements Viewer3DPlugin {
	@Parameter
	private Image3DUniverse universe;

	@Override
	public void run() {
		Content c = universe.getSelectedOrSingleContent();
		if(c != null)
			c.setTransform(new Transform3D());
	}
}
