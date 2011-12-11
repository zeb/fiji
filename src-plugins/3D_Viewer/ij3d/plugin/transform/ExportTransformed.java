package ij3d.plugin.transform;

import ij.IJ;
import ij.ImagePlus;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.plugin.Viewer3DPlugin;
import imagej.ext.plugin.Menu;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import vib.InterpolatedImage;

@Plugin(type = Viewer3DPlugin.class, menu = {
		@Menu(label = "Transformation", mnemonic = 't'),
		@Menu(label = "Export Transformed", weight = 7) })
public class ExportTransformed implements Viewer3DPlugin {
	@Parameter
	private Image3DUniverse universe;

	@Override
	public void run() {
		final Content c = universe.getSelectedOrSingleContent();
		if(c == null)
			return;
		new Thread() {
			{ setPriority(Thread.NORM_PRIORITY); }
			public void run() {
				ImagePlus imp = createTransformedStack(c);
				if(imp == null)
					Utils.error("No greyscale image exists for " + c.getName());
				else
					imp.show();
			}
		}.start();
	}

	public static ImagePlus createTransformedStack(Content c) {
		ImagePlus orig = c.getImage();
		if(orig == null)
			return null;

		Transform3D t1 = new Transform3D();
		c.getLocalTranslate(t1);
		Transform3D t2 = new Transform3D();
		c.getLocalRotate(t2);
		t1.mul(t2);
		t1.invert();

		Matrix4f fc = new Matrix4f();
		Utils.fromCalibration(fc, orig.getCalibration());

		Matrix4f fcInverse = new Matrix4f(fc);
		fcInverse.invert();

		Matrix4f t = new Matrix4f();
		t1.get(t);

		fcInverse.mul(t);
		fcInverse.mul(fc);
		InterpolatedImage in = new InterpolatedImage(orig);
		InterpolatedImage out = in.cloneDimensionsOnly();
		int w = orig.getWidth(), h = orig.getHeight();
		int d = orig.getStackSize();
		Point3f p = new Point3f();

		for (int k = 0; k < d; k++) {
			for (int j = 0; j < h; j++) {
				for(int i = 0; i < w; i++) {
					p.set(i, j, k);
					fcInverse.transform(p);
					out.set(i, j, k, (byte)in.interpol.get(p.x, p.y, p.z));
				}
				IJ.showProgress(k + 1, d);
			}
		}
		out.getImage().setTitle(orig.getTitle() + "_transformed");
		out.getImage().getProcessor().setColorModel(
			orig.getProcessor().getColorModel());
		return out.getImage();
	}
}
