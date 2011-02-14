package ij3d.plugin;

import ij3d.Image3DUniverse;
import imagej.plugin.PluginHandler;
import imagej.plugin.api.PluginEntry;
import imagej.plugin.api.PluginException;
import imagej.plugin.api.PluginHandlerFactory;

public class Viewer3DPluginHandlerFactory implements PluginHandlerFactory {

	private Image3DUniverse univ;

	public Viewer3DPluginHandlerFactory(Image3DUniverse u) {
		this.univ = u;
	}
	@Override
	public PluginHandler createPluginHandler(PluginEntry entry) throws PluginException {
		return new Viewer3DPluginHandler(univ, entry);
	}
}
