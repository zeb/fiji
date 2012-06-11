import ij.IJ;
import ij.ImagePlus;

import ij.plugin.PlugIn;

import ij.plugin.frame.Recorder;

import java.awt.Button;
import java.awt.Container;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OMERO_Importer implements PlugIn {

	public void run(final String arg) {
		// TODO: Extract session key from arg (passed via IJ.run()) or Macro.getOptions() (passed via macro's run(.., options))
		final String sessionKey = "blub";
		// Use Loci_Importer instead
		final ImagePlus image = IJ.openImage("/home/gene099/fiji/samples/clown.jpg");
		// Store true session key
		image.setProperty("omero_session_key", sessionKey);
		image.show();

		// start the Recorder
		if (Recorder.getInstance() == null) {
			final Recorder recorder = new Recorder();
			final Button saveToOmero = new Button("Save Workflow to OMERO");
			saveToOmero.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					saveWorkflowToOMERO(sessionKey, recorder.getText());
				}
			});
			((Container)recorder.getComponents()[0]).add(saveToOmero);
			recorder.pack();
		}
	}

	protected void saveWorkflowToOMERO(final String sessionKey, final String attachmentText) {
		IJ.log("TODO: save (sessionKey " + sessionKey + "):\n" + attachmentText);
	}
}