package ij3d.plugin.transform;

import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.plugin.Viewer3DPlugin;
import imagej.ext.plugin.Menu;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;

import javax.media.j3d.Transform3D;

import math3d.TransformIO;

@Plugin(
		type=Viewer3DPlugin.class,
		menu={
			@Menu(label="Transformation", mnemonic='t'),
			@Menu(label="Save Transform", weight=5)
		}
)
public class SaveTransform implements Viewer3DPlugin {
	@Parameter
	private Image3DUniverse universe;

	@Override
	public void run() {
		Content c = universe.getSelectedOrSingleContent();
		if(c == null)
			return;
		Transform3D t1 = new Transform3D();
		c.getLocalTranslate(t1);
		Transform3D t2 = new Transform3D();
		c.getLocalRotate(t2);
		t1.mul(t2);
		float[] matrix = new float[16];
		t1.get(matrix);
		if(!new TransformIO().saveAffineTransform(matrix))
			Utils.error("Cannot save transformation");
	}
}
