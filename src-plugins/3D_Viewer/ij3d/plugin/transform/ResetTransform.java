package ij3d.plugin.transform;

import ij3d.Image3DUniverse;
import ij3d.plugin.Viewer3DPlugin;
import imagej.plugin.Menu;
import imagej.plugin.Parameter;
import imagej.plugin.Plugin;

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
		System.out.println("reset transform");
	}
}
