package fiji.plugin;

import ij.plugin.PlugIn;

import java.util.Map;

public class RunnableAdapter extends AbstractPlugIn {
	Runnable plugin;

	public RunnableAdapter(Runnable plugin) {
		this.plugin = plugin;
	}

	public void run() {
		plugin.run();
	}

	public void runInteractively() {
		PlugInFunctions.runInteractively(plugin);
	}

	public Map<String, Object> run(Object... parameters)
			throws PlugInException {
		return PlugInFunctions.run(plugin, parameters);
	}

	public void setParameter(String key, Object value) {
		PlugInFunctions.setParameter(plugin, key, value);
	}

	public Map<String, Object> getOutputMap() {
		return PlugInFunctions.getOutputMap(plugin);
	}
}

