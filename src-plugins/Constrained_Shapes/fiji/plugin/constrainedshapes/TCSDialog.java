package fiji.plugin.constrainedshapes;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.EventObject;

import static fiji.plugin.constrainedshapes.GeomShape.EvalFunction;
import ij.ImageListener;
import ij.ImagePlus;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

public class TCSDialog extends javax.swing.JDialog implements ImageListener, ActionListener, FocusListener {

	public static final int OK = 0;
	public static final int CANCELED = 1;
	
	private static final long serialVersionUID = 1L;

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private JLabel jLabelStart;
	private JLabel jLabelLast;
	private JLabel jLabelStep;
	private JLabel jLabelHelp;
	private JButton jButtonCancel;
	private JButton jButtonOK;
	private JTextField jTextFieldStep;
	private JTextField jTextFieldLast;
	private JTextField jTextFieldFirst;
	private JCheckBox jCheckBoxMonitor;
	private JComboBox jComboBoxTargetFunction;
	private JLabel jLabelTargetFunction;
	
	private ImagePlus source_imp;
	private int[] slicing_values;
	private int[] previous_values;
	private int[] upper_bounds; // inclusive
	private int[] lower_bounds;
	private boolean do_monitor;
	
	private ArrayList<ActionListener> action_listeners = new ArrayList<ActionListener>();

	/**
	* Auto-generated main method to display this JDialog
	*/
	public static void main(String[] args) {
		final ImagePlus imp;
		if (args.length > 0) {
			ij.io.Opener o = new ij.io.Opener();
			imp = o.openTiff("/Users/tinevez/Development/fiji", args[0]);
			imp.show();
		} else {
			imp = null;
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				TCSDialog inst = new TCSDialog(imp);
				inst.setVisible(true);
			}
		});
	}
	
	/*
	 * CONSTRUCTOR
	 */
	
	public TCSDialog(ImagePlus imp) {
		super();
		source_imp = imp;
		slicing_values = new int[] { 1, 1, 1} ;
		previous_values = new int[] { 1, 1, 1} ;
		upper_bounds = new int[] { 1, 1, 1} ;
		lower_bounds = new int[] { 1, 1, 1} ;
		if (imp != null) {
			slicing_values[0] = source_imp.getSlice();
			slicing_values[1] = source_imp.getStack().getSize();
			previous_values[0] = slicing_values[0];
			previous_values[1] = slicing_values[1];
			upper_bounds[0] = source_imp.getStack().getSize(); // first
			upper_bounds[1] = upper_bounds[0]; // last
			upper_bounds[2] = source_imp.getStack().getSize();
		}
		initGUI();
		ImagePlus.addImageListener(this);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void addActionListener(ActionListener l) {
		action_listeners.add(l);
	}
	
	public void removeActionListener(ActionListener l) {
		action_listeners.remove(l);
	}
	
	public ActionListener[] getActionListeners() {
		return (ActionListener[]) action_listeners.toArray();
	}
	
	public GeomShape.EvalFunction getSelectedTargetFunction() {
		return (EvalFunction) jComboBoxTargetFunction.getSelectedItem();
	}
	
	public int[] getSliceParameters() {
		return slicing_values;
	}
	
	public boolean doMonitor() {
		do_monitor = jCheckBoxMonitor.isSelected();
		return do_monitor;
	}
	
	
	/*
	 * IMAGEOBSERVER METHODS
	 */
	
	/**
	 * Terminate dialog if source ImagePlus is closed.
	 */
	public void imageClosed(ImagePlus imp) {
		if (source_imp == imp) { dispose(); }
	}

	public void imageOpened(ImagePlus imp) {	}
	public void imageUpdated(ImagePlus imp) {	}

	/*
	 * ACTIONLISTENER METHOD
	 */
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.contentEquals(jButtonOK.getText())) {
			fireActionProperty(OK, jButtonOK.getText());
		} else if (command.contentEquals(jButtonCancel.getText())) {
			fireActionProperty(CANCELED, jButtonCancel.getText());
		} else {
			dealWithTextFields(e);
		}
	}

	/*
	 * FOCUSLISTENER METHODS
	 */
	
	public void focusGained(FocusEvent e) {	}

	public void focusLost(FocusEvent e) {
		dealWithTextFields(e);
	}

	/*
	 * PRIVATE METHODS
	 */
	
	private void dealWithTextFields(EventObject e) {
		JTextField source = (JTextField) e.getSource();
		int index = 0;
		if (source == jTextFieldFirst) { index = 0; }
		else if (source == jTextFieldLast) { index = 1; }
		else { index = 2; }
		try {
			int new_val = (int) Double.parseDouble(source.getText());
			if ( (new_val >= lower_bounds[index]) && (new_val <= upper_bounds[index])  ) {
				slicing_values[index] = new_val;
				previous_values[index] = new_val;
				upper_bounds[0] = slicing_values[1];
				lower_bounds[1] = slicing_values[0];
			} else {
				source.setText(String.format("%d", previous_values[index]));
			}
		} catch (NumberFormatException nfe) {
			source.setText(String.format("%d", previous_values[index]));
		}
	}
	
	private void fireActionProperty(int event_id, String command) {
		ActionEvent action = new ActionEvent(this, event_id, command);
		for (ActionListener l : action_listeners) {
			synchronized (l) {
				l.notifyAll();
				l.actionPerformed(action);				
			}
		}
	}
	
	private void initGUI() {
		try {
			{
				getContentPane().setLayout(null);
				this.setResizable(false);
				this.setPreferredSize(new java.awt.Dimension(400, 259));
				this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				this.setTitle("TCS fitter");
			}
			String message;
			Rectangle message_bounds;
			if ( (source_imp != null) && (source_imp.getStack().getSize() > 1) ) {
				message = "<html>Adjust the two-circle shape as starting point for the fit. " +
						"Set the slice parameters, then press OK.</html>";
				message_bounds = new Rectangle(178, 14, 184, 75);
				{
					jLabelStart = new JLabel();
					getContentPane().add(jLabelStart, "Center");
					jLabelStart.setText("Start slice");
					jLabelStart.setBounds(20, 14, 76, 16);
					jLabelStart.setHorizontalTextPosition(SwingConstants.RIGHT);
					jLabelStart.setHorizontalAlignment(SwingConstants.RIGHT);
				}
				{
					jLabelLast = new JLabel();
					getContentPane().add(jLabelLast, "Center");
					jLabelLast.setText("Last slice");
					jLabelLast.setBounds(20, 37, 76, 16);
					jLabelLast.setHorizontalTextPosition(SwingConstants.RIGHT);
					jLabelLast.setHorizontalAlignment(SwingConstants.RIGHT);
				}
				{
					jLabelStep = new JLabel();
					getContentPane().add(jLabelStep, "Center");
					jLabelStep.setText("Step");
					jLabelStep.setBounds(20, 60, 76, 16);
					jLabelStep.setHorizontalTextPosition(SwingConstants.RIGHT);
					jLabelStep.setHorizontalAlignment(SwingConstants.RIGHT);				
				}
				{
					jTextFieldFirst = new JTextField();
					getContentPane().add(jTextFieldFirst);
					jTextFieldFirst.setText(String.format("%d", slicing_values[0]));
					jTextFieldFirst.setBounds(106, 14, 37, 16);
					jTextFieldFirst.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
					jTextFieldFirst.addActionListener(this);
					jTextFieldFirst.addFocusListener(this);
				}
				{
					jTextFieldLast = new JTextField();
					getContentPane().add(jTextFieldLast);
					jTextFieldLast.setText(String.format("%d", slicing_values[1]));
					jTextFieldLast.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
					jTextFieldLast.setBounds(106, 38, 37, 16);
					jTextFieldLast.addActionListener(this);
					jTextFieldLast.addFocusListener(this);
				}
				{
					jTextFieldStep = new JTextField();
					getContentPane().add(jTextFieldStep);
					jTextFieldStep.setText(String.format("%d", slicing_values[2]));
					jTextFieldStep.setBorder(new LineBorder(new java.awt.Color(0,0,0),1,false));
					jTextFieldStep.setBounds(106, 60, 37, 16);
					jTextFieldStep.addActionListener(this);
					jTextFieldStep.addFocusListener(this);
				}
			} else {
				message = "<html>Adjust the two-circle shape as starting point for the fit, " +
				"then press OK.</html>";
				message_bounds = new Rectangle(20, 14, 334, 75);
			}
			{
				jButtonOK = new JButton();
				getContentPane().add(jButtonOK);
				jButtonOK.setText("OK");
				jButtonOK.setBounds(287, 191, 75, 29);
				jButtonOK.addActionListener(this);
			}
			{
				jButtonCancel = new JButton();
				getContentPane().add(jButtonCancel);
				jButtonCancel.setText("Cancel");
				jButtonCancel.setBounds(20, 191, 86, 29);
				jButtonCancel.addActionListener(this);
			}
			{
				jLabelHelp = new JLabel();
				getContentPane().add(jLabelHelp);
				jLabelHelp.setBounds(message_bounds);
				jLabelHelp.setText(message);
			}
			{
				jLabelTargetFunction = new JLabel();
				getContentPane().add(jLabelTargetFunction);
				jLabelTargetFunction.setText("Target function");
				jLabelTargetFunction.setBounds(46, 96, 97, 16);
			}
			{
				jComboBoxTargetFunction = new JComboBox(GeomShape.EvalFunction.values());
				getContentPane().add(jComboBoxTargetFunction);
				jComboBoxTargetFunction.setBounds(173, 92, 189, 27);
				jComboBoxTargetFunction.setFont(new java.awt.Font("Lucida Grande",0,9));
			}
			{
				jCheckBoxMonitor = new JCheckBox();
				jCheckBoxMonitor.setSelected(do_monitor);
				getContentPane().add(jCheckBoxMonitor);
				jCheckBoxMonitor.setText("Monitor fitting process");
				jCheckBoxMonitor.setBounds(124, 156, 221, 23);
				jCheckBoxMonitor.setIconTextGap(5);
			}

			pack();
			this.setSize(400, 259);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
