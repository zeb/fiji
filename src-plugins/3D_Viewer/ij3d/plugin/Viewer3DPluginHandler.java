package ij3d.plugin;

import ij3d.Image3DUniverse;
import imagej.plugin.PluginHandler;
import imagej.plugin.api.PluginEntry;
import imagej.plugin.api.PluginException;

public class Viewer3DPluginHandler extends PluginHandler {

	private Image3DUniverse universe;

	public Viewer3DPluginHandler(final Image3DUniverse universe,
		final PluginEntry entry) throws PluginException
	{
		super(entry);
		setUniverse(universe);
	}

	public Image3DUniverse getUniverse() {
		return universe;
	}

	private void setUniverse(Image3DUniverse universe) {
		this.universe = universe;
		super.setValue("universe", universe);
		//super.getPresets().put("universe", universe);
	}
}
