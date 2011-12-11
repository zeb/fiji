package ij3d.plugin;

import ij3d.Image3DUniverse;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;

import javax.swing.JOptionPane;

@Plugin(
		menuPath="Help>IJ2 example",
		type=Viewer3DPlugin.class
)
public class ExampleV3DPlugin implements Viewer3DPlugin {

	@Parameter
	private Image3DUniverse universe;

	@Parameter(label="hahaha")
	private String blabber;

	@Override
	public void run() {
		JOptionPane.showMessageDialog(null, "Hello 3D Viewer!" +
			"\nuniverse = " + universe +
			"\nblabber = " + blabber);
	}
}
