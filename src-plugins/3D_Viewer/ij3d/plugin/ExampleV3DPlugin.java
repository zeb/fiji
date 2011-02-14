package ij3d.plugin;

import ij3d.Image3DUniverse;
import imagej.plugin.Parameter;
import imagej.plugin.Plugin;

import javax.swing.JOptionPane;

@Plugin(
		menuPath="Help>IJ2 example"
)
public class ExampleV3DPlugin implements Viewer3DPlugin {

	@Parameter
	private Image3DUniverse universe;

	@Parameter(label="hahaha")
	private String blabber;

	@Override
	public void run() {
		System.out.println("universe = " + universe);
		System.out.println("blabber = " + blabber);
		JOptionPane.showMessageDialog(null, "Hello 3D Viewer");
	}
}
