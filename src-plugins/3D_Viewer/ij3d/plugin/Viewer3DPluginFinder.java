package ij3d.plugin;

import ij.IJ;
import ij3d.Image3DUniverse;
import imagej.plugin.api.PluginEntry;
import imagej.plugin.api.PluginIndex;
import imagej.plugin.finder.IPluginFinder;
import imagej.plugin.finder.PluginFinder;

import java.util.ArrayList;
import java.util.List;

@PluginFinder
public class Viewer3DPluginFinder implements IPluginFinder {
	private Image3DUniverse univ;

	public Viewer3DPluginFinder() {
	}

	public Viewer3DPluginFinder(Image3DUniverse univ) {
		this.univ = univ;
	}

	@Override
	public void findPlugins(List<PluginEntry<?>> plugins) {
		final ArrayList<PluginEntry<Viewer3DPlugin>> pluginList = PluginIndex
				.getIndex(IJ.getClassLoader()).getPlugins(Viewer3DPlugin.class);

		for (final PluginEntry<Viewer3DPlugin> entry : pluginList)
			entry.getPresets().put("universe", univ);

		plugins.addAll(pluginList);
	}
}
