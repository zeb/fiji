import fiji.recorder.RecorderBase;
import fiji.recorder.RecorderBase.Language;
import fiji.recorder.rule.RegexRule;
import fiji.recorder.rule.Rule;
import fiji.recorder.rule.XMLRuleReader;
import fiji.recorder.util.SortedArrayList;
import ij.Command;
import ij.CommandListenerPlus;
import ij.Executer;
import ij.WindowManager;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


/** This is ImageJ's macro recorder. */
public class Python_Recorder extends JFrame implements PlugIn, CommandListenerPlus, ActionListener {

	/** Rule collections	 */
	SortedArrayList<Rule> rule_set = new SortedArrayList<Rule>();
	
	/** Default SUID  */
	private static final long serialVersionUID = 1L;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public Python_Recorder() {
		super("Python recorder");
		init();
		loadRuleSet();
	}

	/*
	 * METHODS
	 */

	public String commandExecuting(String command) {
		return command;
	}


	public void actionPerformed(ActionEvent e) {
		System.out.println("Something was pressed");
		
	}


	public void stateChanged(Command cmd, int state) {
		
		// Only deal with finished commands
		if (state != CommandListenerPlus.CMD_FINISHED) { return; }
		System.out.println(cmd);
		rule_set.sort();
		Iterator<Rule> it = rule_set.iterator();
		Rule rule;
		String result;
		// Because we have a sorted array list, we will match them with increasing priority
		while (it.hasNext()) {
			rule = it.next();
			if (rule.match(cmd)) {
//				System.out.println("This command: " + cmd.getCommand() );
//				System.out.println("Matched the following rule:" + rule.getName());
				result = rule.handle(cmd, Language.Python);
				// textArea.append("\n\n"+result);
				break;
			}
		}
		
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Creates the frame 
	 */
	private void init() {
		// Register this in ImageJ menus
		WindowManager.addWindow(this);
		
		// Create frame
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		


		this.setVisible(true);
		
		// Register as a listener
		Executer.addCommandListener(this);
	}
	
	/**
	 * Load the rule set from the folder 'recorder_rules' in the fiji dev folder.
	 */
	private void loadRuleSet() {
		
		// We chose to put xml rules in a folder, hardcoded
		String path_to_fji = System.getProperty("fiji.dir");
		File rule_folder = new File(path_to_fji, "recorder_rules");
		File[] rule_files = rule_folder.listFiles();		
		
		File rule_file;
		XMLRuleReader xrr = null;
		
		for (int i = 0; i < rule_files.length; i++) {
			rule_file = rule_files[i];
			if (!rule_file.getName().toLowerCase().endsWith(".xml")) {
				continue;
			}
		
			try {
				xrr = new XMLRuleReader(rule_file.getPath());
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
		
			RegexRule rule = xrr.getRule();
			rule_set.add(rule);
		}
		rule_set.setComparator(RegexRule.getReversedComparator());
	}

	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}
}