package ij3d.plugin;

import imagej.plugin.IPlugin;

public interface Viewer3DPlugin extends IPlugin {
	// ImageJPlugin trivially extends IPlugin, so that the name of the interface
	// unambiguously identifies an ImageJ plugin, for discovery by SezPoz.
}
