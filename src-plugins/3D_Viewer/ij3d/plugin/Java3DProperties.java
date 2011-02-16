package ij3d.plugin;

import ij.text.TextWindow;
import ij3d.Image3DUniverse;
import imagej.plugin.Parameter;
import imagej.plugin.Plugin;

import java.util.Map;

import javax.media.j3d.Canvas3D;

@Plugin(
		menuPath="Help>Java 3D Properties",
		type=Viewer3DPlugin.class
)
public class Java3DProperties implements Viewer3DPlugin {

	@Parameter
	private Image3DUniverse universe;

	@Override
	public void run() {
		j3dproperties(universe.getCanvas());
	}

	public static void j3dproperties(Canvas3D canvas) {
		TextWindow tw = new TextWindow("Java 3D Properties",
			"Key\tValue", "", 512, 512);
		Map<?,?> props = Image3DUniverse.getProperties();
		tw.append("Java 3D properties\n \n");
		for(Map.Entry<?, ?> me : props.entrySet())
			tw.append(me.getKey() + "\t" + me.getValue());

		props = canvas.queryProperties();
		tw.append(" \nRendering properties\n \n");
		for(Map.Entry<?, ?> me : props.entrySet())
			tw.append(me.getKey() + "\t" + me.getValue());
	}
}
