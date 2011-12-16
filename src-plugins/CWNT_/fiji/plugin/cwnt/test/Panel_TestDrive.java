package fiji.plugin.cwnt.test;

import javax.swing.JFrame;

import fiji.plugin.cwnt.segmentation.CWNTPanel;
import fiji.plugin.cwnt.segmentation.CWSettings;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;

public class Panel_TestDrive {

	public static void main(String[] args) {
		
		CWNTPanel panel = new CWNTPanel();
		
		TrackMateModel model = new TrackMateModel();
		CWSettings cws = new CWSettings();
		Settings settings = new Settings();
		settings.segmenterSettings = cws;
		model.setSettings(settings);
		panel.setSegmenterSettings(model);
		
		JFrame frame = new JFrame("CWNT panel");
		frame.getContentPane().add(panel);
		frame.setSize(320, 500);
		frame.setVisible(true);
		
		
		
	}
	
}
