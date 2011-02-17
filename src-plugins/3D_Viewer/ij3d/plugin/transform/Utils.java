package ij3d.plugin.transform;

import ij.IJ;
import ij.measure.Calibration;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix4f;

public class Utils {

	private Utils() {}

	public static final void fromCalibration(Matrix4f mat, Calibration c) {
		mat.setIdentity();
		mat.m00 = (float) Math.abs(c.pixelWidth);
		mat.m11 = (float) Math.abs(c.pixelHeight);
		mat.m22 = (float) Math.abs(c.pixelDepth);
		mat.m03 = (float) c.xOrigin;
		mat.m13 = (float) c.yOrigin;
		mat.m23 = (float) c.zOrigin;
	}


	public static final String toString(Transform3D t) {
		float[] mat = new float[16];
		t.get(mat);
		StringBuffer buf = new StringBuffer();
		buf.append(mat[0]);
		for(int i = 1; i < 16; i++)
			buf.append(" ").append(mat[i]);
		return buf.toString();
	}

	public static final Transform3D toTransform(String s) {
		String[] toks = s.split(" ");
		float[] mat = new float[16];
		for(int i = 0; i < toks.length; i++)
			mat[i] = Float.parseFloat(toks[i]);

		return new Transform3D(mat);
	}

	public static void error(String s) {
		IJ.error(s);
	}
}
