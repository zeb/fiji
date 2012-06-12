import fiji.User_Plugins;

import fiji.scripting.TextEditor;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;

import ij.plugin.PlugIn;

import ij.plugin.frame.Recorder;

import java.awt.Button;
import java.awt.Component;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.DateFormat;

import java.util.Date;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class OMERO_Importer implements PlugIn {

	public void run(final String arg) {
		// The argument could be passed in via Java's IJ.run() method (arg) or via a macro call (Macro.getOptions())
		final String sessionKey = arg != null && !arg.equals("") ? arg : Macro.getOptions();
		final ImagePlus image = importImage(sessionKey);
		// Store session key so we can access the image and its metadata later, too
		image.setProperty("omero_session_key", sessionKey);
		image.show();

		// start the Recorder
		final Recorder recorder = getRecorder();
		recorder.recordString("// OMERO import " + image.getTitle().replace('\n', ' ') + "\n");
		addOmeroButton(recorder, sessionKey);

		addOmeroMenu(sessionKey);
	}

	protected Recorder getRecorder() {
		final Recorder recorder = Recorder.getInstance();
		if (Recorder.getInstance() == null) {
			return new Recorder();
		}
		return recorder;
	}

	protected void addOmeroButton(final Recorder recorder, final String sessionKey) {
		final Component[] components = recorder.getComponents();
		final Panel panel = (Panel)components[0];
		final Component[] panelComponents = panel.getComponents();
		Component lastComponent = panelComponents[panelComponents.length - 1];
		// Only add the OMERO button if it is not there yet
		if (!(lastComponent instanceof Button) || !((Button)lastComponent).getLabel().equals("OMERO")) {
			final JPopupMenu omero = new JPopupMenu("OMERO");
			final JMenuItem saveToOmero = new JMenuItem("Attach Workflow");
			saveToOmero.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					saveWorkflowToOMERO(sessionKey, getMacroTitle(), recorder.getText());
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
			panel.add(button);
			recorder.pack();
		}
	}

	// TODO: add the name of the image to which we'll attach
	protected void addOmeroMenu(final String sessionKey) {
		final Menu omero = User_Plugins.getMenu("OMERO");
		final MenuItem saveToOmero = new MenuItem("Attach Workflow");
		saveToOmero.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Recorder recorder = Recorder.getInstance();
				if (recorder == null)
					IJ.error("No Recorder window found");
				else
					saveWorkflowToOMERO(sessionKey, getMacroTitle(), recorder.getText());
			}
		});
		omero.add(saveToOmero);
		final MenuItem editAndSaveToOmero = new MenuItem("Edit & Attach Workflow");
		editAndSaveToOmero.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Recorder recorder = Recorder.getInstance();
				if (recorder == null)
					IJ.error("No Recorder window found");
				else
					editAndSaveWorkflowToOMERO(sessionKey, recorder.getText());
			}
		});
		omero.add(editAndSaveToOmero);
	}

	// Use Loci_Importer instead
	protected ImagePlus importImage(final String sessionKey) {
		return IJ.openImage("/home/gene099/fiji/samples/clown.jpg");
	}

	protected void saveWorkflowToOMERO(final String sessionKey, final String attachmentTitle, final String attachmentText) {
		IJ.log("TODO: save (sessionKey " + sessionKey + ") " + attachmentTitle + ":\n" + attachmentText);
	}

	protected void editAndSaveWorkflowToOMERO(final String sessionKey, final String attachmentText) {
		final TextEditor editor = new TextEditor(getMacroTitle(), attachmentText);

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
				saveWorkflowToOMERO(sessionKey, editor.getTitle(), editor.getTextArea().getText());
			}
		});
		menuBar.getMenu(0).insert(save, 3);
		editor.setVisible(true);
	}

	protected String getMacroTitle() {
		return "Workflow " + DateFormat.getDateTimeInstance().format(new Date()) + ".ijm";
	}
}