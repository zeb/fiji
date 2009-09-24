import fiji.recorder.RecorderBase.Language;
import fiji.recorder.rule.RegexRule;
import fiji.recorder.rule.Rule;
import fiji.recorder.rule.XMLRuleReader;
import fiji.recorder.util.SortedArrayList;
import fiji.scripting.TextEditor;
import ij.Command;
import ij.CommandListenerPlus;
import ij.Executer;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.border.LineBorder;
import javax.xml.parsers.ParserConfigurationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.xml.sax.SAXException;



/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
/** This is ImageJ's macro recorder. */
public class Python_Recorder extends JFrame implements PlugIn, CommandListenerPlus {

	/** Rule collections	 */
	SortedArrayList<Rule> rule_set = new SortedArrayList<Rule>();

	private JPanel jPanel;
	private JToggleButton jButton_start_record;
	private JButton jButton_new;
	private JButton jButton_list_rules;
	private JButton jButton_help;
	private JLabel jLabel_last_command;
	private JLabel jLabel_caught_by;
	private JTextArea jTextArea_caught_by;
	private JLabel jLabel_templates;
	private JComboBox jComboBox_templates;
	private JButton jButton_add_template;
	
	/**
	 * The text area containing the last command caught.
	 */
	private RSyntaxTextArea jTextArea_last_command;
	/**
	 * The current script editor text  zone to output caught commands.
	 */
	private TextEditor current_editor;
	/**
	 * True when the user selected to record command. 
	 */
	private boolean is_recording = false;
	

	/** Default SUID  */
	private static final long serialVersionUID = 1L;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public Python_Recorder() {
		super("Python recorder");
		// Create and draw the JFrame
		init();
		// Load rules
		loadRuleSet();
		// This instance will be added to listeners by the run method.
	}

	/*
	 * METHODS
	 */
	
	public void run(String arg) {
		// Register as a listener
		Executer.addCommandListener(this);
		// Create a new Script editor
		current_editor = new TextEditor(null);
		current_editor.setLanguage("Python");
		current_editor.setVisible(true);
	}

	public String commandExecuting(String command) {
		System.out.println("commandExecuting: "+command);
		return command;
	}


	public void stateChanged(Command cmd, int state) {
		System.out.println("stateChanged: " + cmd);
		// Only deal with finished commands
		if (state != CommandListenerPlus.CMD_FINISHED) { return; }
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
				jTextArea_last_command.setText(result);
				jTextArea_caught_by.setText(rule.getName());
				if (is_recording) {
					current_editor.append(result);
				}
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

//		BorderLayout thisLayout = new BorderLayout();
//		getContentPane().setLayout(thisLayout);
		this.setVisible(true);
		{
			jPanel = new JPanel();
//			getContentPane().add(jPanel, BorderLayout.CENTER);
			this.setContentPane(jPanel);
			GridBagLayout jPanelLayout = new GridBagLayout();
			jPanelLayout.rowWeights = new double[] {0.0, 0.0, 1.0, 0.0, 0.0};
			jPanelLayout.rowHeights = new int[] {22, 15, 50, 61, 15};
			jPanelLayout.columnWeights = new double[] {0.1, 0.1, 0.1, 0.1};
			jPanelLayout.columnWidths = new int[] {7, 7, 7, 7};
			jPanel.setLayout(jPanelLayout);
			jPanel.setPreferredSize(new java.awt.Dimension(443, 259));
			jPanel.setVisible(true);
			jPanel.setEnabled(true);
			{
				jButton_start_record = new JToggleButton();
				jPanel.add(jButton_start_record, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(10, 10, 0, 0), 0, 0));
				jButton_start_record.setText("Record");
				jButton_start_record.setBounds(12, 2, 99, 22);
				jButton_start_record.setSelected(false);
				jButton_start_record.addActionListener(new ActionListener()  {					
					public void actionPerformed(ActionEvent e) {
						if (jButton_start_record.isSelected()) { 
							is_recording = true;
							jButton_start_record.setForeground(Color.red);
						} else { 
							is_recording = false; 
							jButton_start_record.setForeground(Color.black);
						}
					}
				});
			}
			{
				jButton_new = new JButton();
				jPanel.add(jButton_new, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(10, 0, 0, 0), 0, 0));
				jButton_new.setText("New script");
				jButton_new.setBounds(158, 2, 111, 22);
				jButton_new.addActionListener(new ActionListener() {					
					public void actionPerformed(ActionEvent e) {
						// Create a new Script editor
						current_editor = new TextEditor(null);
						current_editor.setLanguage("Python");
						current_editor.setVisible(true);						
					}
				});
			}
			{
				jButton_list_rules = new JButton();
				jPanel.add(jButton_list_rules, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(10, 0, 0, 0), 0, 0));
				jButton_list_rules.setText("List rules");
				jButton_list_rules.setBounds(300, 2, 65, 22);
				jButton_list_rules.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) { displayRuleTable(); }
				});
			}
			{
				jButton_help = new JButton();
				jPanel.add(jButton_help, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(10, 0, 0, 10), 0, 0));
				jButton_help.setText("Help");
				jButton_help.setBounds(445, 2, 41, 22);
			}
			{
				jLabel_last_command = new JLabel();
				jPanel.add(jLabel_last_command, new GridBagConstraints(0, 1, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
				jLabel_last_command.setText("Last command:");
			}
			{
				jTextArea_last_command = new RSyntaxTextArea();
				jTextArea_last_command.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
				Style[] styles = jTextArea_last_command.getSyntaxScheme().styles;
				for (int i = 0; i < styles.length; i++) {
					try {
						styles[i].font =  styles[i].font.deriveFont((float) 10);
					} catch (NullPointerException ne) {}
				}
				jTextArea_last_command.setFont(new Font("monospaced", Font.PLAIN, 10));
				
				jPanel.add(jTextArea_last_command, new GridBagConstraints(0, 2, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 0, 10), 0, 0));
				jTextArea_last_command.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
				jTextArea_last_command.setEditable(false);
			}
			{
				jLabel_caught_by = new JLabel();
				jPanel.add(jLabel_caught_by, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
				jLabel_caught_by.setText("Caught by rule:");
			}
			{
				jTextArea_caught_by = new JTextArea();
				jTextArea_caught_by.setBackground(jPanel.getBackground());
				jTextArea_caught_by.setFont(new Font("monospaced", Font.ITALIC, 11));
				
				jPanel.add(jTextArea_caught_by, new GridBagConstraints(1, 3, 3, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jTextArea_caught_by.setEditable(false);
			}
			{
				jLabel_templates = new JLabel();
				jPanel.add(jLabel_templates, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(10, 10, 10, 0), 0, 0));
				jLabel_templates.setText("Templates:");
			}
			{

				ComboBoxModel jComboBox_templatesModel = 
					new DefaultComboBoxModel(
							new String[] { "Item One", "Item Two" });
				jComboBox_templates = new JComboBox();
				jPanel.add(jComboBox_templates, new GridBagConstraints(1, 4, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 0), 0, 0));
				jComboBox_templates.setModel(jComboBox_templatesModel);
			}
			{
				jButton_add_template = new JButton();
				jPanel.add(jButton_add_template, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(10, 0, 10, 10), 0, 0));
				jButton_add_template.setText("Add");
			}
		}
		pack();
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

	public void displayRuleTable() {
		String[] column_names = {
				"Priority",
				"Name",
				"Command pattern",
				"Class name pattern",
				"Arguments pattern",
				"Modifiers pattern",
				"Description",
				"Python translator"		};
		Object[][] rule_data = new Object[rule_set.size()][column_names.length];
		
		rule_set.sort();
		Rule rule;
		RegexRule rex_rule;
		for (int i = 0; i < rule_data.length; i++) {
			rule = rule_set.get(i);
			rule_data[i][0] = rule.getPriority();
			rule_data[i][1] = rule.getName();
			if (RegexRule.class.isInstance(rule)) {
				rex_rule = (RegexRule) rule;
				rule_data[i][2] = rex_rule.getCommand();
				rule_data[i][3] = rex_rule.getClassName();
				rule_data[i][4] = rex_rule.getArguments();
				rule_data[i][5] = rex_rule.getModifiers();
				rule_data[i][6] = rex_rule.getDescription();
				rule_data[i][7] = rex_rule.getPythonTranslator();
			}
		}
		
		JTable table = new JTable(rule_data, column_names);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));

		JScrollPane scrollPane = new JScrollPane(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		JPanel		table_panel = new JPanel(new GridLayout());
		table_panel.add(scrollPane);	
	    JFrame 		frame = new JFrame("Recorder rules");

	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    //Create and set up the content pane.
	    frame.setContentPane(table_panel);

	    //Display the window.
	    frame.pack();
	    frame.setVisible(true);
	}

	/*
	 * MAIN
	 */
	
	public static void main(String[] args) {
		Python_Recorder pr = new Python_Recorder();
		pr.run(null);
	}
	
}