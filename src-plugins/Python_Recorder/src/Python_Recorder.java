import fiji.recorder.CommandTranslatorRule;
import fiji.recorder.XMLRuleReader;
import fiji.recorder.util.SortedArrayList;
import ij.Command;
import ij.CommandListenerPlus;
import ij.Executer;
import ij.WindowManager;
import ij.gui.GUI;
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

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


/** This is ImageJ's macro recorder. */
public class Python_Recorder extends PlugInFrame implements CommandListenerPlus, ActionListener {

	/** Rule collections	 */
	SortedArrayList<CommandTranslatorRule> rule_set = new SortedArrayList<CommandTranslatorRule>();
	
	/** Default SUID  */
	private static final long serialVersionUID = 1L;
	/** 
	 * Default name for new scripts
	 */
	private static final String DEFAULT_NAME = "Python_Script.py";


	public Python_Recorder() {
		super("Python_Recorder");
		
		// Register this in ImageJ menus
		WindowManager.addWindow(this);
		
		// Create frame
		Panel panel = new Panel(new FlowLayout(FlowLayout.CENTER, 2, 0));
		panel.add(new Label("Name:"));
		TextField macro_name = new TextField(DEFAULT_NAME,15);
		panel.add(macro_name);
		panel.add(new Label("     "));
		Button makeMacro = new Button("To editor");
		makeMacro.addActionListener(this);
		panel.add(makeMacro);
		panel.add(new Label("     "));
		Button help = new Button("?");
		help.addActionListener(this);
		panel.add(help);
		add("North", panel);
		TextArea textArea = new TextArea("",15,60,TextArea.SCROLLBARS_VERTICAL_ONLY);
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		add("Center", textArea);
		pack();
		GUI.center(this);
		this.setVisible(true);

		// Register as a listener
		Executer.addCommandListener(this);
		
		loadRuleSet();
		
	}


	public String commandExecuting(String command) {
		System.out.println("A command was launched: "+command);
		return command;
	}


	public void actionPerformed(ActionEvent e) {
		System.out.println("Something was pressed");
		
	}


	public void stateChanged(Command cmd, int state) {
		
		// Only deal with finished commands
		if (state != CommandListenerPlus.CMD_FINISHED) { return; }
		rule_set.sort();
		Iterator<CommandTranslatorRule> it = rule_set.iterator();
		CommandTranslatorRule rule;
		// Because we have a sorted array list, we will match them with increasing priority
		while (it.hasNext()) {
			rule = it.next();
			if (rule.match(cmd)) {
				System.out.println("This command:");
				System.out.println(cmd);
				System.out.println("Matched the following rule:");
				System.out.println(rule);
			}
		}
		
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void loadRuleSet() {
		
		// Ugly for the moment, don't pay attention
		String path_to_fji = System.getProperty("fiji.dir");
		String path_to_rule = "src-plugins" 
			+ File.separator + "Python_Recorder"
			+ File.separator + "DefaultRule.xml";
		File rule_file = new File(path_to_fji, path_to_rule);
		if (rule_file.exists()) 
			System.out.println("Rule file found.");
		
		XMLRuleReader xrr = null;
		try {
			xrr = new XMLRuleReader(rule_file.getPath());
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		CommandTranslatorRule rule = xrr.getRule();
		rule_set.add(rule);
		rule_set.setComparator(CommandTranslatorRule.getComparator());
	}
}