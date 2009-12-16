package fiji.plugin.constrainedshapes;
import ij.ImagePlus;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

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
public class TCSDialog extends javax.swing.JDialog {

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

	/**
	* Auto-generated main method to display this JDialog
	*/
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				TCSDialog inst = new TCSDialog(null);
				inst.setVisible(true);
			}
		});
	}
	
	public TCSDialog(ImagePlus imp) {
		super();
		initGUI();
	}
	
	private void initGUI() {
		try {
			{
				getContentPane().setLayout(null);
				this.setResizable(false);
				this.setPreferredSize(new java.awt.Dimension(395, 175));
				this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				this.setTitle("TCS fitter");
			}
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
				jTextFieldFirst.setText("1");
				jTextFieldFirst.setBounds(106, 14, 37, 16);
				jTextFieldFirst.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
			}
			{
				jTextFieldLast = new JTextField();
				getContentPane().add(jTextFieldLast);
				jTextFieldLast.setText("1");
				jTextFieldLast.setBorder(new LineBorder(new java.awt.Color(0,0,0), 1, false));
				jTextFieldLast.setBounds(106, 38, 37, 16);
			}
			{
				jButtonOK = new JButton();
				getContentPane().add(jButtonOK);
				jButtonOK.setText("OK");
				jButtonOK.setBounds(287, 104, 75, 29);
			}
			{
				jButtonCancel = new JButton();
				getContentPane().add(jButtonCancel);
				jButtonCancel.setText("Cancel");
				jButtonCancel.setBounds(20, 104, 86, 29);
			}
			{
				jLabelHelp = new JLabel();
				getContentPane().add(jLabelHelp);
				jLabelHelp.setBounds(178, 14, 184, 75);
				jLabelHelp.setText("<html>Adjust the two-circle shape as starting point for the fit. Set the slice parameters, then press OK.</html>");
			}
			{
				jTextFieldStep = new JTextField();
				getContentPane().add(jTextFieldStep);
				jTextFieldStep.setText("1");
				jTextFieldStep.setBorder(new LineBorder(new java.awt.Color(0,0,0),1,false));
				jTextFieldStep.setBounds(106, 60, 37, 16);
			}
			pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
