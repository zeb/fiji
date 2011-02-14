package ij3d.plugin;

import ij3d.Image3DUniverse;
import imagej.plugin.api.PluginEntry;
import imagej.plugin.finder.IPluginFinder;
import imagej.plugin.finder.PluginFinder;
import imagej.plugin.finder.SezpozPluginFinder;

import java.util.List;


@PluginFinder
public class Viewer3DPluginFinder extends SezpozPluginFinder<Viewer3DPlugin>
	implements IPluginFinder
{

	private Image3DUniverse univ;

	public Viewer3DPluginFinder() {}

	public Viewer3DPluginFinder(Image3DUniverse univ) {
		this.univ = univ;
	}

	@Override
	public void findPlugins(List<PluginEntry> plugins) {
		findPlugins(plugins, Viewer3DPlugin.class);
		Viewer3DPluginHandlerFactory f = new Viewer3DPluginHandlerFactory(univ);
		for(PluginEntry entry : plugins) {
			entry.setPluginHandlerFactory(f);
		}
	}
}
