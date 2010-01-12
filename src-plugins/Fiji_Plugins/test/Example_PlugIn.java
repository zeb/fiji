package test;

import fiji.plugin.AbstractPlugIn;
import fiji.plugin.Parameter;

import ij.IJ;

public class Example_PlugIn extends AbstractPlugIn {
	/* the name will be used for the dialog, so it starts upcased. */
	@Parameter public String First_name;

	public void run() {
		IJ.showMessage("Good morning, " + First_name + "!");
	}
}
