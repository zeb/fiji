package fiji.recorder;

import fiji.recorder.rule.Rule;
import fiji.recorder.util.SortedArrayList;
import ij.Command;
import ij.CommandListenerPlus;
import ij.plugin.PlugIn;

import java.util.Collection;

public class RecorderBase implements CommandListenerPlus, PlugIn {


	public static class NotHandledException extends Exception {
		/** Default SUID */
		private static final long serialVersionUID = 1L;

		NotHandledException(String reason) {
			super(reason);
		}
	}
	
	/**
	 * Enum that stores what language we can handle
	 */
	public enum Language  { BeanShell, Clojure, JavaScript, Python, Ruby };
	

	Collection<Rule> rules;

	public RecorderBase() {
		rules = new SortedArrayList<Rule>();
	}

	public RecorderBase(Collection<Rule> rules) {
		this.rules = rules;
	}

	public void stateChanged(Command command, int state) {
		// TODO Auto-generated method stub
		
	}

	public String commandExecuting(String command) {
		// TODO Auto-generated method stub
		return null;
	}

	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}
}