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


/** This is ImageJ's macro recorder. */
public class Python_Recorder extends PlugInFrame implements CommandListenerPlus, ActionListener {

	
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
		
	}


	public String commandExecuting(String command) {
		System.out.println("A command was launched: "+command);
		return command;
	}


	public void actionPerformed(ActionEvent e) {
		System.out.println("Something was pressed");
		
	}


	public void stateChanged(Command arg0, int arg1) {
		System.out.println("A command was launched: "+arg0+" with state "+arg1);
		
	}
	
}