import fiji.scripting.TextEditor;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;

import ij.plugin.PlugIn;

import ij.plugin.frame.Recorder;

import java.awt.Button;
import java.awt.Container;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class OMERO_Importer implements PlugIn {

	public void run(final String arg) {
		// The argument could be passed in via Java's IJ.run() method (arg) or via a macro call (Macro.getOptions())
		final String sessionKey = arg != null && !arg.equals("") ? arg : Macro.getOptions();
		final ImagePlus image = IJ.openImage("/home/gene099/fiji/samples/clown.jpg");
		// Store true session key
		image.setProperty("omero_session_key", sessionKey);
		image.show();

		// start the Recorder
		if (Recorder.getInstance() == null) {
			final Recorder recorder = new Recorder();
			final JPopupMenu omero = new JPopupMenu("OMERO");
			final JMenuItem saveToOmero = new JMenuItem("Attach Workflow");
			saveToOmero.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					saveWorkflowToOMERO(sessionKey, recorder.getText());
				}
			});
			omero.add(saveToOmero);
			final JMenuItem editAndSaveToOmero = new JMenuItem("Edit & Attach Workflow");
			editAndSaveToOmero.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					editAndSaveWorkflowToOMERO(sessionKey, recorder.getText());
				}
			});
			omero.add(editAndSaveToOmero);
			final Button button = new Button("OMERO");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					omero.show(button, 0, 0);
				}
			});
			((Container)recorder.getComponents()[0]).add(button);
			recorder.pack();
		}
	}

	protected void saveWorkflowToOMERO(final String sessionKey, final String attachmentText) {
		IJ.log("TODO: save (sessionKey " + sessionKey + "):\n" + attachmentText);
	}


	protected void editAndSaveWorkflowToOMERO(final String sessionKey, final String attachmentText) {
		final TextEditor editor = new TextEditor("Workflow", attachmentText);

		// remove some menus, add a menu item in File
		final JMenuBar menuBar = editor.getJMenuBar();
		for (int i = menuBar.getMenuCount() - 1; i >= 0; i--) {
			JMenu menu = menuBar.getMenu(i);
			String label = menu.getLabel();
			if (label.equals("Language") || label.equals("Templates") || label.equals("Run"))
				menuBar.remove(i);
		}

		final JMenuItem save = new JMenuItem("Save workflow to OMERO");
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				saveWorkflowToOMERO(sessionKey, editor.getTextArea().getText());
			}
		});
		menuBar.getMenu(0).insert(save, 3);
		editor.setVisible(true);
	}
}