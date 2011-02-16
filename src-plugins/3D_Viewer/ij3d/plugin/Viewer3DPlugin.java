package ij3d.plugin;

import imagej.plugin.RunnablePlugin;


public interface Viewer3DPlugin extends RunnablePlugin {
	// ImageJPlugin trivially extends IPlugin, so that the name of the interface
	// unambiguously identifies an ImageJ plugin, for discovery by SezPoz.
}
